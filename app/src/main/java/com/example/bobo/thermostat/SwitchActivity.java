package com.example.bobo.thermostat;

import android.app.Activity;
import android.os.Bundle;
import android.widget.ListView;

// webservice
import util.*;

public class SwitchActivity extends Activity {

    ListView list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_switch);
        //list = (ListView) findViewById(R.id.list);
    }
}
