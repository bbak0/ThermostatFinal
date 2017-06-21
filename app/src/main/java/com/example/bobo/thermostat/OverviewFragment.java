package com.example.bobo.thermostat;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;//PAY ATTENTION TO HAVE THIS ONE!
import android.os.Handler;
import android.os.Message;
import android.telephony.SignalStrength;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import util.HeatingSystem;

import static com.example.bobo.thermostat.R.id.updateDayButton;
import static com.example.bobo.thermostat.R.id.updateNightButton;
import static com.example.bobo.thermostat.R.id.updateTargetButton;
import static java.lang.Thread.sleep;


/**
 * A simple {@link Fragment} subclass.
 */
public class OverviewFragment extends Fragment implements View.OnClickListener {

    View v;
    //TextView
    TextView currentTemp_text, dayTemp_text, nightTemp_text;
    //EditText
    EditText targetTemp_text;
    //Buttons
    Button updateTarget_button;
    Button updateDay_button;
    Button updateNight_button;
    // Declare day/night/current/desired temperature
    String dayTemp, nightTemp, currentTemp, targetTemp;
    // auto-updates
    boolean autoCurrent = true;
    boolean autoDayNight = true;
    int UPDATE_INTERVAL_DN = 3000; //update interval for day/night (old 4000)
    int UPDATE_INTERVAL_C = 1500; //update current interval (old 2500)
    int numberReconnections = 0;
    // Signal strength
    SignalStrength signal;
    int signalStrength = 0;
    boolean informedUser = false;
    //Threads
    Thread NightDayThread,CurrentThread;
    String getInfo;
    // stoping threads
    boolean alreadyNotified = false;
    // items for change temperature pop-up dialog
    String changeTemperature="20";
    String dialog_message;
    String typeOfSet;

    public OverviewFragment() {
        // Required empty public constructor
        setHasOptionsMenu(true);
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        v = inflater.inflate(R.layout.fragment_overview, container, false);

        // START

        HeatingSystem.BASE_ADDRESS = "http://wwwis.win.tue.nl/2id40-ws/35";

        currentTemp_text = (TextView) v.findViewById(R.id.currentTemp_text);
        dayTemp_text = (TextView) v.findViewById(R.id.dayTemp_text);
        nightTemp_text = (TextView) v.findViewById(R.id.nightTemp_text);
        targetTemp_text = (EditText) v.findViewById(R.id.targetTemperature_text);
        updateTarget_button = (Button) v.findViewById(updateTargetButton);
        updateDay_button = (Button) v.findViewById(updateDayButton);
        updateNight_button = (Button) v.findViewById(updateNightButton);
        NightDayThread = new Thread();
        CurrentThread = new Thread();

        // setting the clicks
        updateTarget_button.setOnClickListener(this);
        updateDay_button.setOnClickListener(this);
        updateNight_button.setOnClickListener(this);

        // Start the auto-updates
        nightDayTemperature();
        currentTemperature();
        
        return v;
    }

    // Reconnecting to the internet and start the auto-updates

    public void reconnectNetwork() {

        // avoid starting a billion of threads if they are already alive
        if (NightDayThread.isAlive() && CurrentThread.isAlive()) {
            Toast.makeText(getActivity().getApplicationContext(), "Already connected!", Toast.LENGTH_SHORT).show();
            return;
        }

        int numberReconnections = 0;
        for (numberReconnections = 1; numberReconnections <= 3; numberReconnections++) {
            Toast.makeText(getActivity().getApplicationContext(), "Connecting.. (" + numberReconnections + ")", Toast.LENGTH_SHORT).show();
            if (isNetworkAvailable()) {
                // reconnecting and starting the auto-update threads
                autoCurrent = true;
                autoDayNight = true;
                nightDayTemperature();
                currentTemperature();
                // informing the user that reconnection was successful
                Toast.makeText(getActivity().getApplicationContext(), "Connected! Retrieving data, please wait", Toast.LENGTH_SHORT).show();
                return;//!!
            }
        }
        Toast.makeText(getActivity().getApplicationContext(), "Failed", Toast.LENGTH_SHORT).show();

    }

    // If no internet, stop the auto-updates by closing the threads and writing OFFLINE

    public void closeNetwork() {
        if (!isNetworkAvailable() && !alreadyNotified) {
            // canceling the loops/auto-updates/threads
            alreadyNotified = true;
            autoCurrent = false;
            autoDayNight = false;
            NightDayThread.interrupt();
            CurrentThread.interrupt();
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
                                    nightTemp_text.setText("OFFLINE");
                                    dayTemp_text.setText("OFFLINE");
                                    currentTemp_text.setText("OFFLINE");
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

    // Inform user that there is no internet connection
    Handler writeNoInternet = new Handler() {
        @Override
        public void handleMessage (Message msg){
            Toast.makeText(getActivity().getApplicationContext(),
                    "Network not available, please reconnect", Toast.LENGTH_LONG).show();
        }
    };

    //Writing day/night temperature on UI
    Handler writeNightDay = new Handler() {
        @Override
        public void handleMessage (Message msg){
            // try and catch because threads can run on the background
            // and the interface might not be active
            // in this way we prevent crashes
            try {
                nightTemp_text.setText(nightTemp);
                dayTemp_text.setText(dayTemp);
            } catch (Error e) {
                System.err.println("No interface!" + e);
            }
        }
    };

    //Writing current temperature on UI
    Handler writeCurrent = new Handler() {
        @Override
        public void handleMessage (Message msg){
            // try and catch because threads can run on the background
            // and the interface might not be active
            // in this way we prevent crashes
            try {
                currentTemp_text.setText(currentTemp);
            } catch (Error e) {
                System.err.println("No interface!" + e);
            }
        }
    };

    Handler writeSent = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // for Toast we do not make try and catch
            // because getActivity().getApplicationContext() will return null and nothing will be shown
            // thus no errors
            Toast.makeText(getActivity().getApplicationContext(),
                    "Sent!", Toast.LENGTH_LONG).show();
        }
    };

    Handler writeSending = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(getActivity().getApplicationContext(),
                    "Sending...", Toast.LENGTH_SHORT).show();
        }
    };

    Handler writeError = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(getActivity().getApplicationContext(),
                    "Error while sending", Toast.LENGTH_SHORT).show();
        }
    };


    //Retrieving night/day temperature

    public void nightDayTemperature() {
        NightDayThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while(autoDayNight) {

                        if (isNetworkAvailable()) {
                            // getting day/night temperature
                            dayTemp = HeatingSystem.get("dayTemperature");
                            nightTemp = HeatingSystem.get("nightTemperature");
                            // writing day/night temperature on UI
                            writeNightDay.sendEmptyMessage(0);
                        } else {
                            // closing threads
                            closeNetwork();
                        }
                        sleep(UPDATE_INTERVAL_DN);
                    }
                } catch (Exception e) {
                    System.err.println("Error from getdata " + e);
                }

            }
        });
        //starting thread (auto-update night/day)
        NightDayThread.start();
    }

    //Retrieving current temperature

    public void currentTemperature() {
        CurrentThread = new Thread(new Runnable() {
            @Override
            public void run() {
                currentTemp = "";
                try {
                    while (autoCurrent) {
                        if (isNetworkAvailable()) {
                            // getting current temperature
                            currentTemp = HeatingSystem.get("currentTemperature");
                            // writing current temperature on UI
                            writeCurrent.sendEmptyMessage(0);
                        } else {
                            // closing threads
                            closeNetwork();
                        }
                        sleep(UPDATE_INTERVAL_C);
                    }
                } catch (Exception e) {
                    System.err.println("Error from getdata " + e);
                }
            }
        });
        // starting thread (auto-update current)
        CurrentThread.start();
    }

    // overwriting the current temperature

    public void updateTarget(View v) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (isNetworkAvailable() && autoCurrent && autoDayNight) {
                        // getting the text from the box
                        targetTemp = String.valueOf(targetTemp_text.getText());
                        HeatingSystem.put("currentTemperature", targetTemp);
                    } else {
                        writeNoInternet.sendEmptyMessage(0);
                    }
                } catch (Exception e) {
                    System.err.println("Error from getdata " + e);
                }
            }
        }).start();
    }

    // input dialog for changing all temperatures

    public void sendUpdatedInfo() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (isNetworkAvailable() && autoCurrent && autoDayNight) {
                        HeatingSystem.put(typeOfSet, changeTemperature);
                        writeSent.sendEmptyMessage(0);
                    } else {
                        writeNoInternet.sendEmptyMessage(0);
                    }
                } catch (Exception e) {
                    System.err.println("Error from getdata " + e);
                    writeError.sendEmptyMessage(0);
                }
            }
        }).start();
    }

    // pop-up dialog for changing the night/day temperature

    Handler changeTempDialog = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            AlertDialog.Builder builder1 = new AlertDialog.Builder(getActivity());
            builder1.setMessage(dialog_message);
            builder1.setCancelable(true);

            final EditText input = new EditText(getActivity());
            // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
            input.setInputType(InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_CLASS_NUMBER);
            builder1.setView(input);

            builder1.setPositiveButton(
                    "Yes",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            changeTemperature = input.getText().toString();
                            sendUpdatedInfo();
                            writeSending.sendEmptyMessage(0);
                        }
                    });

            builder1.setNegativeButton(
                    "No",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            dialog.cancel();
                        }
                    });

            AlertDialog alert11 = builder1.create();
            alert11.show();
        }
    };

    // updating day temperature

    public void updateDay(View v) {
        try {
            if (isNetworkAvailable() && autoCurrent && autoDayNight) {
                dialog_message = "Update day temperature";
                typeOfSet = "dayTemperature";
                changeTempDialog.sendEmptyMessage(0);
                dayTemp = String.valueOf(changeTemperature);
            } else {
                writeNoInternet.sendEmptyMessage(0);
            }
        } catch (Exception e) {
            System.err.println("Error from getdata " + e);
        }
    }

    //updating night temperature

    public void updateNight(View v) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (isNetworkAvailable() && autoCurrent && autoDayNight) {
                        dialog_message = "Update night temperature";
                        typeOfSet = "nightTemperature";
                        changeTempDialog.sendEmptyMessage(0);
                        nightTemp = String.valueOf(changeTemperature);
                    } else {
                        writeNoInternet.sendEmptyMessage(0);
                    }
                } catch (Exception e) {
                    System.err.println("Error from getdata " + e);
                }
            }
        }).start();
    }

    // listening for clicks
    @Override
    public void onClick(View v) {
        if (v.getId() == updateTargetButton) {
            updateTarget(v);
        } else if (v.getId() == updateDayButton) {
            updateDay(v);
        } else if (v.getId() == updateNightButton) {
            updateNight(v);
        }
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
