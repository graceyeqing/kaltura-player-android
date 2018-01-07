package com.kaltura.playerdemo;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioGroup;

public class LaunchActivity extends AppCompatActivity {

    private static final String TAG = "LaunchActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launch);
        
        ListView jsonList = findViewById(R.id.json_list);
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);
        adapter.addAll("1.json", "2.json", "3.json");
        jsonList.setAdapter(adapter);
        
        RadioGroup source = findViewById(R.id.radio_source);
        source.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(TAG, "checkedId: " + findViewById(checkedId));
            }
        });
        source.check(R.id.src_history);

        RadioGroup mode = findViewById(R.id.radio_mode);
        mode.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                Log.d(TAG, "checkedId: " + findViewById(checkedId));
            }
        });
        mode.check(R.id.modefilter_all);
    }
}
