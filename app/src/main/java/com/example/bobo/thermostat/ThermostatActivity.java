package com.example.bobo.thermostat;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.telephony.SignalStrength;
import android.text.InputType;
import android.view.MenuInflater;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import static java.lang.Thread.sleep;

// webserver
import util.*;

public class ThermostatActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_thermostat);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /*FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                        .setAction("Action", null).show();
            }
        });*/

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        // setting the OverviewFragment to be displayed once the app is opened
        setTitle("Overview");
        OverviewFragment fragment = new OverviewFragment();
        navigationView.setCheckedItem(R.id.Overview);// highlight the Overview item from menu bar!
        FragmentTransaction fm = getSupportFragmentManager().beginTransaction();
        fm.replace(R.id.frame, fragment, "fragment1");
        fm.commit();
    }

    // Android's garbage

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            if (getTitle().equals("Overview")){
                super.onBackPressed();
            } else {

                setTitle("Overview");
                OverviewFragment fragment = new OverviewFragment();
                navigationView.setCheckedItem(R.id.Overview);
                FragmentTransaction fm = getSupportFragmentManager().beginTransaction();
                fm.replace(R.id.frame, fragment, "fragment1");
                fm.commit();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.thermostat, menu);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        if (id == R.id.action_reconnect) {
            //reconnectNetwork();
        }
        //
        if (id == R.id.action_Monday) {
            //Intent myIntent = new Intent(ThermostatActivity.this, MondayActivity.class);
            //myIntent.putExtra("key", 0); //Optional parameters
            //ThermostatActivity.this.startActivity(myIntent);
        }

        return super.onOptionsItemSelected(item);
    }

   @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.Overview) {
            setTitle("Overview");
            OverviewFragment fragment = new OverviewFragment();

            FragmentTransaction fm = getSupportFragmentManager().beginTransaction();
            fm.replace(R.id.frame, fragment, "fragment1");
            fm.commit();
        } else if (id == R.id.Monday) {
            //myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            setTitle("Monday");
            MondayFragment fragment = new MondayFragment();

            //putting the information related with day
            Bundle bundle = new Bundle();
            bundle.putString("day","Monday");
            fragment.setArguments(bundle);

            // making the transition
            FragmentTransaction fm = getSupportFragmentManager().beginTransaction();
            fm.replace(R.id.frame, fragment, "fragment1");
            fm.commit();

        } else if (id == R.id.Tuesday) {
            setTitle("Tuesday");
            MondayFragment fragment = new MondayFragment();

            //putting the information related with day
            Bundle bundle = new Bundle();
            bundle.putString("day","Tuesday");
            fragment.setArguments(bundle);

            // making the transition
            FragmentTransaction fm = getSupportFragmentManager().beginTransaction();
            fm.replace(R.id.frame, fragment, "fragment1");
            fm.commit();

        } else if (id == R.id.Wednesday) {

            setTitle("Wednesday");
            MondayFragment fragment = new MondayFragment();

            //putting the information related with day
            Bundle bundle = new Bundle();
            bundle.putString("day","Wednesday");
            fragment.setArguments(bundle);

            // making the transition
            FragmentTransaction fm = getSupportFragmentManager().beginTransaction();
            fm.replace(R.id.frame, fragment, "fragment1");
            fm.commit();

        } else if (id == R.id.Thursday) {

            setTitle("Thursday");
            MondayFragment fragment = new MondayFragment();

            //putting the information related with day
            Bundle bundle = new Bundle();
            bundle.putString("day","Thursday");
            fragment.setArguments(bundle);

            // making the transition
            FragmentTransaction fm = getSupportFragmentManager().beginTransaction();
            fm.replace(R.id.frame, fragment, "fragment1");
            fm.commit();

        } else if (id == R.id.Friday) {

            setTitle("Friday");
            MondayFragment fragment = new MondayFragment();

            //putting the information related with day
            Bundle bundle = new Bundle();
            bundle.putString("day","Friday");
            fragment.setArguments(bundle);

            // making the transition
            FragmentTransaction fm = getSupportFragmentManager().beginTransaction();
            fm.replace(R.id.frame, fragment, "fragment1");
            fm.commit();

        } else if (id == R.id.Saturday) {

            setTitle("Saturday");
            MondayFragment fragment = new MondayFragment();

            //putting the information related with day
            Bundle bundle = new Bundle();
            bundle.putString("day","Saturday");
            fragment.setArguments(bundle);

            // making the transition
            FragmentTransaction fm = getSupportFragmentManager().beginTransaction();
            fm.replace(R.id.frame, fragment, "fragment1");
            fm.commit();

        } else if (id == R.id.Sunday) {

            setTitle("Sunday");
            MondayFragment fragment = new MondayFragment();

            //putting the information related with day
            Bundle bundle = new Bundle();
            bundle.putString("day","Sunday");
            fragment.setArguments(bundle);

            // making the transition
            FragmentTransaction fm = getSupportFragmentManager().beginTransaction();
            fm.replace(R.id.frame, fragment, "fragment1");
            fm.commit();

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }


}
