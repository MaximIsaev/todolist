package mobile.app.com.todolist;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mobile.app.com.todolist.dao.Task;
import mobile.app.com.todolist.dialog.EditTaskDialog;
import mobile.app.com.todolist.loader.MyCursorLoader;

public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<Cursor> {

    ListView listView;
    Db db;
    SimpleCursorAdapter cursorAdapter;
    Map<Long, Boolean> itemSelection = new HashMap<>();
    MenuItem editItem;
    private int count = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        db = new Db(this);
        db.open();

        String[] from = new String[]{Db.COLUMN_ITEM_NAME};
        int[] to = new int[]{R.id.item_name};

        cursorAdapter = new SimpleCursorAdapter(this, R.layout.item_layout, null, from, to, 0);
        listView = (ListView) findViewById(R.id.taskList);
        listView.setAdapter(cursorAdapter);
        listView.setChoiceMode(AbsListView.CHOICE_MODE_MULTIPLE_MODAL);
        listView.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

            @Override
            public boolean onCreateActionMode(android.view.ActionMode mode, Menu menu) {
                count = 0;
                mode.getMenuInflater().inflate(R.menu.task_context_menu, menu);
                editItem = menu.findItem(R.id.edit_item);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(android.view.ActionMode mode, Menu menu) {
                return false;
            }

            @Override
            public boolean onActionItemClicked(android.view.ActionMode mode, MenuItem item) {

                switch (item.getItemId()) {
                    case R.id.delete_item:
                        Set<Long> keys = itemSelection.keySet();
                        Iterator<Long> iterator = keys.iterator();
                        while (iterator.hasNext()) {
                            db.delete(iterator.next());
                        }
                        break;
                    case R.id.edit_item:
                        Bundle bundle = new Bundle();
                        bundle.putLong("id", itemSelection.keySet().iterator().next());
                        bundle.putString("name", db.getOne(itemSelection.keySet().iterator().next()).getName());
                        DialogFragment dialog = new EditTaskDialog();
                        dialog.setArguments(bundle);
                        dialog.show(getSupportFragmentManager(), "editTag");
                        break;
                }
                getSupportLoaderManager().restartLoader(0, null, MainActivity.this);
                mode.finish();
                return true;
            }

            @Override
            public void onDestroyActionMode(android.view.ActionMode mode) {
                clearSelections();
            }

            @Override
            public void onItemCheckedStateChanged(android.view.ActionMode mode, int position, long id, boolean checked) {
                Log.d("TEST", "id = " + id + ", checked = "
                        + checked);
                if (checked) {
                    count++;
                    setNewSelection(id, checked);
                } else {
                    count--;
                    removeSelection(id);
                }
                if (count > 1) {
                    editItem.setVisible(false);
                } else {
                    editItem.setVisible(true);
                }
            }
        });
        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        PackageManager pm = getPackageManager();
        List activities = pm.queryIntentActivities(

                new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);

        if (activities.size() != 0) {
            fab.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (view.getId() == R.id.fab) {
                        startVoiceRecognitionActivity();
                    }
//                    Intent intent = new Intent(MainActivity.this, AddNewTaskActivity.class);
//                    startActivity(intent);
                }
            });

        } else {
            Toast.makeText(this, "Recognizer not present", Toast.LENGTH_SHORT).show();
        }


        getSupportLoaderManager().initLoader(0, null, this);
    }

    private void startVoiceRecognitionActivity() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak, please");
        startActivityForResult(intent, 7);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 7 && resultCode == RESULT_OK) {
            // Fill the list view with the strings the recognizer thought it could have heard
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
//            String resString = "";
//            for (String s : matches) {
//                resString += s + "	";
//            }
            Task task = new Task(matches.get(0), false);
            db.saveTask(task);
            getSupportLoaderManager().restartLoader(0, null, MainActivity.this);
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        return new MyCursorLoader(this, db);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        cursorAdapter.swapCursor(cursor);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        db.close();
    }

    public void setNewSelection(Long position, boolean value) {
        itemSelection.put(position, value);
    }

    public boolean isPositionChecked(Long position) {
        Boolean checked = itemSelection.get(position);
        return checked == null ? false : checked;
    }

    public void removeSelection(Long position) {
        itemSelection.remove(position);
    }

    public void clearSelections() {
        itemSelection.clear();
    }
}
