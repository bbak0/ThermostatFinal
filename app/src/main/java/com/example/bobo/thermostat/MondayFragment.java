package com.example.bobo.thermostat;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.w3c.dom.Text;

import java.net.ConnectException;
import java.util.ArrayList;

import util.HeatingSystem;
import util.Switch;
import util.WeekProgram;

import static android.R.attr.dial;

import static java.lang.Thread.sleep;


/**
 * A simple {@link Fragment} subclass.
 */
public class MondayFragment extends Fragment {

    // the list and its interface(adapter)
    ListView lv;
    CustomAdapter adapter;
    // the message for empty/offline list
    TextView emptyList;
    // the thread to update constantly the list
    Thread MondayListThread;
    // auto-updates
    boolean autoUpdateList = true;
    int UPDATE_INTERVAL_MdL = 3000;
    // the array of switches for Monday
    ArrayList<Switch> MondaySwitches = new ArrayList<>();
    ArrayList<String> temperatures = new ArrayList<>();
    int[] IMAGES = new int[100];
    Switch s,s2;
    WeekProgram wpg;
    String dayTempVal;
    String nightTempVal;
    //checking if updating has been requested
    boolean updating = false;
    int nr_switch = 0;
    boolean state_switch = false;
    String time_switch;
    String type_switch;
    // view
    View v;
    // buttons
    // thread used for retrieving data in order to verify it after addition
    Thread verifyThread;
    // weekprogram for modification/deletion and for verification
    WeekProgram wpg_md;
    WeekProgram wpg_v;
    // the day that the fragment is representing
    String day = "Monday";
    // thread used for retrieving data in order to add switches to it
    Thread dataThread;
    // stoping threads
    boolean alreadyNotified = false;

    public MondayFragment() {
        // Required empty public constructor
        setHasOptionsMenu(true);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_monday, container, false);

        getTemps();
        // START

        // declaring the list
        lv = (ListView) v.findViewById(R.id.Lista);
        // declaring the message for empty list
        emptyList = (TextView)v.findViewById(R.id.emptyList);
        
        //declaring the day
        Bundle bundle = this.getArguments();
        if (bundle != null) {
            day = bundle.getString("day");
        }

        // declaring the buttons

        // setting the clicks

        // creating the thread
        MondayListThread = new Thread();

        // setting the thread and starting it
        retrieveData();

        //adding the floating button
        FloatingActionButton fab = (FloatingActionButton)v.findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                      //  .setAction("Action", null).show();
                addSwitchDialog();
            }
        });

        //returning the view because fragments in android are retarded
        return v;


    }

    // Inform user that there is no internet connection
    Handler writeNoInternet = new Handler() {
        @Override
        public void handleMessage (Message msg){
            Toast.makeText(getActivity().getApplicationContext(),
                    "Network not available, please reconnect", Toast.LENGTH_LONG).show();
        }
    };

    Handler writeSwitchUnavailable = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(getActivity().getApplicationContext(),
                    "This switch is unavailable! Try a different configuration!", Toast.LENGTH_SHORT).show();
        }
    };

    Handler writeAdding = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(getActivity().getApplicationContext(),
                    "Adding", Toast.LENGTH_SHORT).show();
        }
    };

    Handler writeAdditionSuccesful = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(getActivity().getApplicationContext(),
                    "Addition successful!", Toast.LENGTH_SHORT).show();
        }
    };

    Handler writeAdditionUnsuccesful = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(getActivity().getApplicationContext(),
                    "Addition unsuccessful!", Toast.LENGTH_SHORT).show();
        }
    };


    Handler writeSending = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(getActivity().getApplicationContext(),
                    "Sending", Toast.LENGTH_SHORT).show();
        }
    };

    Handler writeUpdateSuccesful = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(getActivity().getApplicationContext(),
                    "Update successful!", Toast.LENGTH_SHORT).show();
        }
    };

    // unsuccesful because of the communication with the server, not a server error!
    Handler writeUpdateUnsuccesful = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(getActivity().getApplicationContext(),
                    "Update unsuccessful! Try later!", Toast.LENGTH_SHORT).show();
        }
    };

    // for the catch statement, it does not relate with the content of the list
    // like for ex: too many day switches
    Handler writeServerErrorMsg = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            Toast.makeText(getActivity().getApplicationContext(),
                    ((String)msg.obj), Toast.LENGTH_SHORT).show();
        }
    };


    // Reconnecting to the internet and start the auto-updates

    public void reconnectNetwork() {

        // avoid starting a billion of threads if they are already alive
        if (MondayListThread.isAlive()) {
            Toast.makeText(getActivity().getApplicationContext(), "Already connected!", Toast.LENGTH_SHORT).show();
            return;
        }

        int numberReconnections = 0;
        for (numberReconnections = 1; numberReconnections <= 3; numberReconnections++) {
            Toast.makeText(getActivity().getApplicationContext(), "Connecting.. (" + numberReconnections + ")", Toast.LENGTH_SHORT).show();
            if (isNetworkAvailable()) {
                // reconnecting and starting the auto-update threads
                autoUpdateList = true;
                retrieveData();

                // informing the user that reconnection was successful
                Toast.makeText(getActivity().getApplicationContext(), "Connected! Retrieving data, please wait", Toast.LENGTH_SHORT).show();
                return;//!!
            }
        }
        Toast.makeText(getActivity().getApplicationContext(), "Failed", Toast.LENGTH_SHORT).show();

    }

    public void getTemps(){
        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    final String dayTempVal = HeatingSystem.get("dayTemperature");
                    final String nightTempVal = HeatingSystem.get("nightTemperature");
                    temperatures.add(dayTempVal);
                    temperatures.add(nightTempVal);
                } catch (Exception e) {
                    System.err.println("Error from getdata "+e);
                }
            }
        }).start();
    }

    public void closeNetwork() {
        if (!isNetworkAvailable() && !alreadyNotified) {
            // canceling the loops/auto-updates/threads
            alreadyNotified = true;
            autoUpdateList = false;
            MondayListThread.interrupt();
            new Thread(){
                @Override
                public void run(){
                    try {
                        synchronized(this){
                            wait(500);

                            alreadyNotified = false;
                            // informing the user that there is no internet
                            getActivity().runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    //take actions to inform the user that no internet is available
                                    // setting the message from the empty view of the list
                                    emptyList.setText("Network not available, please reconnect");

                                    // removing all the content from the list
                                    // in this way the empty view set on displayData() can be shown
                                    lv.setAdapter(null); //!!!!!!!!
                                    //lv.setVisibility(View.GONE); - it messes the layout of the buttons

                                }
                            });
                        }
                    }
                    catch(InterruptedException ex){
                    }

                    /*alreadyNotified = false;
                    // informing the user that there is no internet
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            nightTemp_text.setText("OFFLINE");
                            dayTemp_text.setText("OFFLINE");
                            currentTemp_text.setText("OFFLINE");
                        }
                    });*/
                    writeNoInternet.sendEmptyMessage(0);
                }
            }.start();
        }

    }

    // retrieving data every 5 seconds
    public void retrieveData() {
        MondayListThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (autoUpdateList) {
                        if (isNetworkAvailable()) {

                            // Get the week program
                            wpg = HeatingSystem.getWeekProgram();
                            // clearing the old switches
                            MondaySwitches.clear();
                            // Get the switches for Monday
                            MondaySwitches = wpg.getDay(day);


                            // keeping only the switches which are on
                            for (int i = 0; i < (MondaySwitches.size()); i++) {
                                if (MondaySwitches.get(i).getState() == false) {
                                    MondaySwitches.remove(i);
                                    // maintain position !!
                                    i--;
                                    //Log.e("retrieveData()","Switch" + String.valueOf(i) + " is false, removing it");
                                }
                            }

                            displayData();
                        } else {
                            // closing the thread to save battery and to have more control
                            closeNetwork();
                        }
                        sleep(UPDATE_INTERVAL_MdL);
                    }
                } catch (Exception e) {
                    System.err.println("Error from getdata " + e);
                }
            }
        });
        // starting the thread for auto-updating the list
        MondayListThread.start();

    }

    // setting the correct images for each switch
    public int[] arrayOfImages() {
        for (int i = 0; i < MondaySwitches.size(); i++) {
            s = MondaySwitches.get(i);
            if (s.getType().equals("day")) {
                IMAGES[i] = R.drawable.sun;
                // Log.e("Error", Integer.toString(IMAGES[i]));
            } else if (s.getType().equals("night")) {
                IMAGES[i] = R.drawable.moon;
            }
        }
        //Log.e("Error","D");
        //Log.e("SIZE",Integer.toString(MondaySwitches.size()));
        return IMAGES;
    }

    // displaying the listview
    public void displayData() {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                // setting the correct images for each switch
                arrayOfImages();

                // checking to see if it is an update of the list
                // or if it is the creation of the list
                // in this way the scroll position is maintained
                if (lv.getAdapter() == null) {
                    // we also link the adapter with MondayFragment
                    // in this way we can call the methods from MondayFragment in CustomAdapter
                    adapter = new CustomAdapter(getActivity(), MondaySwitches, IMAGES, day, temperatures);
                    // adding a message to be displayed when list empty/offline
                    lv.setEmptyView(emptyList);
                    // setting the adapter/custom interface !!
                    lv.setAdapter(adapter);
                }
                else {
                    adapter.updateData(MondaySwitches,IMAGES); //update your adapter's data
                    adapter.notifyDataSetChanged();
                }

                // if list is empty display appropriate message
                if (MondaySwitches.size() == 0) {
                    emptyList.setText("List empty! " +
                            "Add switches using the button in the bottom right corner.");
                    // removing all the content from the list
                    // in this way the empty view set on displayData() can be shown
                    lv.setAdapter(null); //!!!!!!!!
                    //lv.setVisibility(View.GONE); - it messes the layout of the buttons
                }
            }
        });
    }

    // helping the user if the written time is of format H:MM instead of HH:MM
    // because the server does not accept H:MM
    // thus H:MM is transformed to 0H:MM
    String addZero (String time_desired) {
        Log.e("addZero()", "from CustomAdapter " + String.valueOf(time_desired.length()));
        // ensure that the length matches the one of H:MM
        if (time_desired.length()!=4)
            return time_desired;
        String hh = time_desired.substring(0,2);
        if (hh.substring(1,2).equals(":") && hh.substring(0,1).compareTo("0") >=0 &&
                hh.substring(0,1).compareTo("9") <= 0) {
            Log.e("addZero()", "from CustomAdapter has added a zero to the time desired " + "0" + time_desired);
            return ("0" + time_desired);
        } else{
            Log.e("addZero()", "from CustomAdapter has NOT added a zero to the time desired " + time_desired + hh);
            return time_desired;
        }
    }

    // checking to see if the time format introduced is good or not
    boolean goodFormat(String time_desired) {
        if (time_desired.length() != 5)
            return false;
        String hh = time_desired.substring(0,2);
        String dotdot = time_desired.substring(2,3);
        String mm = time_desired.substring(3,5);
        if (hh.compareTo("00") < 0 || hh.compareTo("23") > 0)
            return false;
        if (!dotdot.equals(":"))
            return false;
        if (mm.compareTo("00") < 0 || mm.compareTo("59") > 0)
            return false;
        return true;
    }

    // count the number of Day switches that are active until now
    int nrDaySwitchesOn() {
        int nr = 0;

        // I assumed that MondaySwitches is "new" enough to be used
        for (int i = 0; i < (MondaySwitches.size()); i++) {
            if (MondaySwitches.get(i).getType().equals("day"))
                nr ++;
        }
        return nr;
    }

    // count the number of Day switches that are active until now
    int nrNightSwitchesOn() {
        int nr = 0;

        // I assumed that MondaySwitches is "new" enough to be used
        for (int i = 0; i < (MondaySwitches.size()); i++) {
            if (MondaySwitches.get(i).getType().equals("night"))
                nr ++;
        }
        return nr;
    }



    // pop-up dialog in which the user can modify the switch
    void addSwitchDialog() {

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("");


        // custom layout
        LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        final View dialoglayout = inflater.inflate(R.layout.activity_switch, null);;
        builder.setView(dialoglayout);

        // declaring the radiogroup
        final RadioGroup radioButtonGroup = (RadioGroup)dialoglayout.findViewById(R.id.temperature_radioGroup);
        // declaring the radioButtons
        final RadioButton dayTemperature_radioButton = (RadioButton)dialoglayout.findViewById(R.id.dayTemperature_radioButton);
        final RadioButton nightTemperature_radioButton = (RadioButton)dialoglayout.findViewById(R.id.nightTemperature_radioButton);
        // declaring the time input
        //final EditText desiredTime_text = (EditText)dialoglayout.findViewById(R.id.desiredTIme_text);

        final TimePicker timePicker = (TimePicker)dialoglayout.findViewById(R.id.timePicker);
        final TextView daySwitchesLeft = (TextView)dialoglayout.findViewById(R.id.dialog_day_left);
        final TextView nightSwitchesLeft = (TextView)dialoglayout.findViewById(R.id.dialog_night_left);
        final TextView dayTemp = (TextView)dialoglayout.findViewById(R.id.dayTemp_text);
        final TextView nightTemp = (TextView)dialoglayout.findViewById(R.id.nightTemp_text);

        daySwitchesLeft.setText(nrDaySwitchesOn() + "/5 used");
        nightSwitchesLeft.setText(nrNightSwitchesOn() + "/5 used");
        
        //24 hour format for the time picker
        timePicker.setIs24HourView(true);
        
        // setting the picker to 00:00
        timePicker.setHour(0);
        timePicker.setMinute(0);

        dayTemp.setText(temperatures.get(0) + "\u2103");
        nightTemp.setText(temperatures.get(1) + "\u2103");



        // setting the availability of the radio buttons in accordance with the nr of day/night switches ON until now
        // I have assumed that MondaySwitches is "new" enough and that there is no need to start a new thread
        // in this way we do not exceed the max nr of day/night switches
        if (nrDaySwitchesOn() == 5)
            dayTemperature_radioButton.setEnabled(false);
        if (nrNightSwitchesOn() == 5)
            nightTemperature_radioButton.setEnabled(false);

        // confirm button
        builder.setPositiveButton(
                "Confirm",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //we will overwrite it later
                    }
                });

        // cancel button
        builder.setNegativeButton(
                "Cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        final AlertDialog dialog = builder.create();
        dialog.show();

        // overwriting the positive ("confirm") button to maintain the dialog when no radiobutton is selected
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // getting the desiredTime
                String time_desired = String.format("%02d:%02d", timePicker.getHour(), timePicker.getMinute());
                // add a 0 in case the user inputs something like: 2:36
                // apparently the server does not accept 2:36
                time_desired = addZero(time_desired);

                // CHECK TO SEE IF THE TIME HAS A GOOD FORMAT !!!!!!
                if (!goodFormat(time_desired)) {
                    Toast.makeText(getActivity(),
                            "Invalid time or format HH:MM not respected!", Toast.LENGTH_SHORT).show();
                    return;
                    // in this way the pop-up dialog is not closed!
                }

                // CHECK TO SEE IF THE TIME IS AVAILABLE !!!!!!
                // CHECK TO SEE IF THERE ARE SWITCHES AVAILABLE!!!!
                // by all of these I mean check to see if there is already another ON switch in that desired time
                // I have assumed that MondaySwitches is "new" enough so that we do not have to start a new thread
                for (int i = 0; i < (MondaySwitches.size()); i++) {
                    if (time_desired.equals(MondaySwitches.get(i).getTime()) &&
                            MondaySwitches.get(i).getState()) {
                        Toast.makeText(getActivity(),
                                "There is already an active switch in that desired time! Please choose a different time!",
                                Toast.LENGTH_SHORT).show();
                        return;
                        // in this way the pop-up dialog is not closed!
                    }
                }

                //getting the id of the selected
                int radioButtonID = -1;
                radioButtonID = radioButtonGroup.getCheckedRadioButtonId();
                View radioButton = radioButtonGroup.findViewById(radioButtonID);
                int idx = radioButtonGroup.indexOfChild(radioButton);
                Log.e("radioButton", String.valueOf(idx));


                // CHECK TO SEE IF SOMETHING HAS BEEN SELECTED
                if (idx == 0) {
                    // if day selected
                    addSwitch("day", time_desired);
                    dialog.dismiss();
                } else if (idx == 1) {
                    // if night selected
                    addSwitch("night", time_desired);
                    dialog.dismiss();
                } else {
                    Toast.makeText(getActivity(),
                            "Please select a temperature!", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    // verify that the desired switch was added successfully
    void verifyAdd(final String type_desired, final String time_desired) {
        //retrieving data
        verifyThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Get the week program
                    wpg_v = HeatingSystem.getWeekProgram();

                    Log.e("verifyAdd()", "retrieving data was successful ");

                    // verify that the desired switch exists
                    for (int i = 0; i < (wpg_v.getDay(day).size()); i++) {
                        if (time_desired.equals(wpg_v.getDay(day).get(i).getTime()) &&
                                wpg_v.getDay(day).get(i).getState() &&
                                type_desired.equals(wpg_v.getDay(day).get(i).getType())) {
                            // modification was successful
                            writeAdditionSuccesful.sendEmptyMessage(0);
                            return;
                        }
                    }
                    // modification was unsucessful
                    writeAdditionUnsuccesful.sendEmptyMessage(0);
                } catch (Exception e) {
                    System.err.println("Error from getdata in verifyDelete() from CustomAdapter " + e);
                }
            }
        });
        // starting the thread for retrieving the list
        verifyThread.start();
    }

    void addSwitch(final String type_desired, final String time_desired) {

        //retrieving data
        dataThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Get the week program
                    wpg_md = HeatingSystem.getWeekProgram();

                    Log.e("addSwitch()"," retrieving data was successful " + type_desired + " " + time_desired);

                    //informing user that the adding process has started
                    writeAdding.sendEmptyMessage(0);

                    // identifying a suitable switch from the desired day
                    // that can be changed in what we want
                    // I do not assume that such switch is available
                    boolean availableSwitch = false;
                    for (int i = 0; i < (wpg_md.getDay(day).size()); i++) {
                        if (type_desired.equals(wpg_md.getDay(day).get(i).getType()) &&
                                !wpg_md.getDay(day).get(i).getState()) {

                            Log.e("addSwitch()","item found! " + time_desired + " " + String.valueOf(i));
                            //modifying the desired switch
                            wpg_md.getDay(day).get(i).setState(true);
                            wpg_md.getDay(day).get(i).setTime(time_desired);
                            wpg_md.getDay(day).get(i).setType(type_desired);
                            Log.e("modifySwitch()","item found and modified!");
                            availableSwitch = true;
                            break;
                        }
                    }

                    if (availableSwitch == false) {
                        // no available switch!
                        writeSwitchUnavailable.sendEmptyMessage(0);
                        // there is no point in verifying if the desired switch is not available!!!
                        // + it will give confusing toast messages!
                    } else {
                        // available switch

                        // updating the schedule
                        HeatingSystem.setWeekProgram(wpg_md);
                        wait(2000);

                        // informing the adapter that the list has been changed !!!
                        // VERY IMPORTANT, IF NOT IT WILL RESULT IN CRASHES !!!!
                        adapter.notifyDataSetChanged();

                        // verifying the modification
                        verifyAdd(type_desired, time_desired);
                    }
                } catch (Exception e) {
                    System.err.println("Error from getdata in deleteSwitch() from CustomAdapter " + e);
                    // verifying the modification
                    verifyAdd(type_desired, time_desired);
                }
            }
        });
        // starting the thread for retrieving the list
        dataThread.start();
    }



    // open addition dialog pop-up when add button is pressed
    public void addSchedule (View v) {
       /* //!Switch s = new Switch("day",true, "13:00");

        // creating the desired switch
        updating = true;
        nr_switch = 3;
        state_switch = false;
        type_switch = "night";
        time_switch = "05:55";
        s1 = new Switch(type_switch,state_switch, time_switch);


        //!MondaySwitches.add(s);
        //arrayOfImages();
        //displayData();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    synchronized(this) {

                        //wpg.setDefault();
                        //wpg.data.get("Monday").remove(3); - does not work
                        //!wpg.data.get("Monday").set(3, new Switch("day", true, "12:00"));

                        // updating the new switch to the server
                        wpg = HeatingSystem.getWeekProgram();
                        wpg.data.get("Monday").set(nr_switch, s1);
                        HeatingSystem.setWeekProgram(wpg);
                        writeSending.sendEmptyMessage(0);
                        wait(2000);
                        retrieveData();
                    }
                } catch (Exception e) {
                    System.err.println("Error from getdata " + e);
                    // in case of updating error, quite technical error
                    Message msg = Message.obtain();
                    msg.obj = String.valueOf(e);
                    writeServerErrorMsg.sendMessage(msg);
                }
            }
        }).start();*/
        addSwitchDialog();
    }


    // resetting the entire schedule to default
    public void reset() {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    wpg.setDefault();
                    HeatingSystem.setWeekProgram(wpg);
                    writeSending.sendEmptyMessage(0);

                } catch (Exception e) {
                    System.err.println("Error from getdata " + e);
                    // in case of updating error, quite technical error
                }
            }
        }).start();
    }

    // resetting the whole schedule
    public void test(View v) {
        reset();
    }



    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        // Inflate the menu; this adds items to the action bar if it is present.
        menu.clear();
        inflater.inflate(R.menu.monday, menu);
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
            reconnectNetwork();
            Log.e("Reconnect","in MondayFragment");
        }
        //
        if (id == R.id.action_Monday) {
            //Intent myIntent = new Intent(getActivity().getApplicationContext(), MondayActivity.class);
            //myIntent.putExtra("key", 0); //Optional parameters
            //ThermostatActivity.this.startActivity(myIntent);
        }

        return super.onOptionsItemSelected(item);
    }

    /* Check if user is connected to a network */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getActivity().getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}

