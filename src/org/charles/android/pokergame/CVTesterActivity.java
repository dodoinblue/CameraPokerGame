package org.charles.android.pokergame;

import android.app.Activity;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.util.ArrayList;

/**
 * Created by Charles on 13-8-31.
 */
public class CVTesterActivity extends Activity {

    private static final String ORIGINAL_IMAGE = Environment.getExternalStorageDirectory()
           + "/cards.png";
    private Display mDisplay;
    private Spinner mOperationList;
    private ArrayList<String> mList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cvtester_activity);

        initSpinner();

        mDisplay = (Display) findViewById(R.id.display);
        mDisplay.showImage(ORIGINAL_IMAGE);
    }
    private void log(String s) {
        Log.i("Charles_TAG", "CVTester :: " + s);
    }

    private void showToast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }

    private void initSpinner() {
        mOperationList = (Spinner) findViewById(R.id.op_list);
        mList = new ArrayList<String>();
        mList.add("cvtColor");
        mList.add("GaussianBlur");
        mList.add("threshold");
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, mList);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mOperationList.setAdapter(adapter);
        mOperationList.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                showToast("Item selected: " + mList.get(i));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                log("onNothingSelected");
            }
        });
    }

}
