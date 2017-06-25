package com.example.bobo.thermostat;


import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.Image;
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
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.NumberPicker;
import android.widget.TextView;
import android.widget.Toast;

import com.triggertrap.seekarc.SeekArc;

import org.w3c.dom.Text;

import util.HeatingSystem;
import util.Switch;

import static com.example.bobo.thermostat.R.id.button_editday;
import static com.example.bobo.thermostat.R.id.button_editnight;
import static com.example.bobo.thermostat.R.id.seekArc;
import static java.lang.Thread.sleep;


/**
 * A simple {@link Fragment} subclass.
 */
public class OverviewFragment extends Fragment {

    View v;
    //TextView
    TextView currentTemp_text, dayTemp_text, nightTemp_text, date_text, target_text, target_display;
    ImageButton bDay, bNight, bMinus, bPlus;
    Button apply;
    android.widget.Switch vacationSwitch;
    SeekArc arc;
    boolean vacation;
    String targetM;
    String targetF;
    String dialogTargetM;
    String dialogTargetF;
    boolean changeTarget = true;
    //EditText
    //Buttons
    // Declare day/night/current/desired temperature
    String dayTemp, nightTemp, currentTemp, targetTemp, date;
    double dialogTemp;
    int arcProgress;
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
        v = inflater.inflate(R.layout.fragment_overview_copy2, container, false);

        // START
        HeatingSystem.BASE_ADDRESS = "http://wwwis.win.tue.nl/2id40-ws/8";

        dayTemp_text = (TextView) v.findViewById(R.id.dayTemp_text);
        nightTemp_text = (TextView) v.findViewById(R.id.nightTemp_text);
        currentTemp_text = (TextView) v.findViewById(R.id.currentTemp_text);
        date_text = (TextView) v.findViewById(R.id.textView_date);
        target_text = (TextView) v.findViewById(R.id.targetTemperature_text);
        bDay = (ImageButton) v.findViewById(button_editday);
        bNight = (ImageButton) v.findViewById(button_editnight);
        bMinus  = (ImageButton) v.findViewById(R.id.bMinus);
        bPlus = (ImageButton) v.findViewById((R.id.bPlus));
        vacationSwitch = (android.widget.Switch) v.findViewById(R.id.switch1);
        target_display = (TextView) v.findViewById(R.id.target_display);
        arc = (SeekArc) v.findViewById(seekArc);
        apply = (Button) v.findViewById(R.id.b_apply);
        NightDayThread = new Thread();
        CurrentThread = new Thread();

        // setting the clicks
        getTarget();

        bMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                arc.setProgress(arc.getProgress()-1);
                int temp = Integer.valueOf(targetM)*10 + Integer.valueOf(targetF);
                temp--;
                targetF = String.valueOf(temp%10);
                targetM = String.valueOf(temp/10);
            }
        });

        bPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                arc.setProgress(arc.getProgress()+1);
                int temp = Integer.valueOf(targetM)*10 + Integer.valueOf(targetF);
                temp++;
                targetF = String.valueOf(temp%10);
                targetM = String.valueOf(temp/10);
            }
        });

        apply.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String text = target_text.getText().toString();
                String temp = text.substring(0,text.length()-1);
                updateTarget(temp);
                writeUpdating.sendEmptyMessage(0);
            }
        });

        vacationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked){
                    setWeeklyProgram(false);
                } else {
                    setWeeklyProgram(true);
                }
                writeUpdating.sendEmptyMessage(0);
            }
        });

        arc.setOnSeekArcChangeListener(new SeekArc.OnSeekArcChangeListener() {
            @Override
            public void onProgressChanged(SeekArc seekArc, int i, boolean b) {
                targetF = String.valueOf(i%10);
                targetM = String.valueOf((i+50)/10);
                target_text.setText(targetM + "." + targetF + "\u2103");

            }

            @Override
            public void onStartTrackingTouch(SeekArc seekArc) {

            }

            @Override
            public void onStopTrackingTouch(SeekArc seekArc) {

            }
        });

        arc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                targetF = String.valueOf(arc.getProgress()%10);
                targetM = String.valueOf((arc.getProgress()+50)/10);
                target_text.setText(targetM + "." + targetF + "\u2103");
            }
        });



        bDay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Choose new night temperature:");

                LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final View dialoglayout = inflater.inflate(R.layout.edit_temp2, null);
                builder.setView(dialoglayout);

                if (dayTemp.length() == 4){
                    dialogTargetM = dayTemp.substring(0,2);
                    dialogTargetF = dayTemp.substring(3,4);
                } else {
                    dialogTargetM = dayTemp.substring(0,1);
                    dialogTargetF = dayTemp.substring(2,3);
                }

                final ImageButton dbMinus = (ImageButton)dialoglayout.findViewById(R.id.bMinus);
                final ImageButton dbPlus = (ImageButton)dialoglayout.findViewById(R.id.bPlus);
                final SeekArc arc =(SeekArc)dialoglayout.findViewById(R.id.seekArc);
                final TextView display = (TextView) dialoglayout.findViewById(R.id.targetTemperature_text);

                arc.setProgress(Integer.valueOf(dialogTargetM)*10 - 50 + Integer.valueOf(dialogTargetF));
                display.setText(dialogTargetM + "." + dialogTargetF + "\u2103");

                arc.setOnSeekArcChangeListener(new SeekArc.OnSeekArcChangeListener() {
                    @Override
                    public void onProgressChanged(SeekArc seekArc, int i, boolean b) {
                        dialogTargetF = String.valueOf(i%10);
                        dialogTargetM = String.valueOf((i+50)/10);
                        display.setText(dialogTargetM + "." + dialogTargetF + "\u2103");
                    }

                    @Override
                    public void onStartTrackingTouch(SeekArc seekArc) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekArc seekArc) {

                    }
                });
                dbMinus.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        arc.setProgress(arc.getProgress()-1);
                        int temp = Integer.valueOf(dialogTargetM)*10 + Integer.valueOf(dialogTargetF);
                        temp--;
                        dialogTargetF = String.valueOf(temp%10);
                        dialogTargetM = String.valueOf(temp/10);
                    }
                });

                dbPlus.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        arc.setProgress(arc.getProgress()+1);
                        int temp = Integer.valueOf(dialogTargetM)*10 + Integer.valueOf(dialogTargetF);
                        temp++;
                        dialogTargetF = String.valueOf(temp%10);
                        dialogTargetM = String.valueOf(temp/10);
                    }
                });





                builder.setPositiveButton(
                        "Confirm",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                String text = display.getText().toString();
                                String temp = text.substring(0,text.length()-1);
                                updateDay(temp);
                                writeUpdating.sendEmptyMessage(0);
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
            }
        });

        bNight.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
                builder.setTitle("Choose new night temperature:");

                LayoutInflater inflater = (LayoutInflater) getActivity().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                final View dialoglayout = inflater.inflate(R.layout.edit_temp2, null);
                builder.setView(dialoglayout);

                if (dayTemp.length() == 4){
                    dialogTargetM = nightTemp.substring(0,2);
                    dialogTargetF = nightTemp.substring(3,4);
                } else {
                    dialogTargetM = nightTemp.substring(0,1);
                    dialogTargetF = nightTemp.substring(2,3);
                }

                final ImageButton dbMinus = (ImageButton)dialoglayout.findViewById(R.id.bMinus);
                final ImageButton dbPlus = (ImageButton)dialoglayout.findViewById(R.id.bPlus);
                final SeekArc arc =(SeekArc)dialoglayout.findViewById(R.id.seekArc);
                final TextView display = (TextView) dialoglayout.findViewById(R.id.targetTemperature_text);

                arc.setProgress(Integer.valueOf(dialogTargetM)*10 - 50 + Integer.valueOf(dialogTargetF));
                display.setText(dialogTargetM + "." + dialogTargetF + "\u2103");

                arc.setOnSeekArcChangeListener(new SeekArc.OnSeekArcChangeListener() {
                    @Override
                    public void onProgressChanged(SeekArc seekArc, int i, boolean b) {
                        dialogTargetF = String.valueOf(i%10);
                        dialogTargetM = String.valueOf((i+50)/10);
                        display.setText(dialogTargetM + "." + dialogTargetF + "\u2103");
                    }

                    @Override
                    public void onStartTrackingTouch(SeekArc seekArc) {

                    }

                    @Override
                    public void onStopTrackingTouch(SeekArc seekArc) {

                    }
                });
                dbMinus.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        arc.setProgress(arc.getProgress()-1);
                        int temp = Integer.valueOf(dialogTargetM)*10 + Integer.valueOf(dialogTargetF);
                        temp--;
                        dialogTargetF = String.valueOf(temp%10);
                        dialogTargetM = String.valueOf(temp/10);
                    }
                });

                dbPlus.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        arc.setProgress(arc.getProgress()+1);
                        int temp = Integer.valueOf(dialogTargetM)*10 + Integer.valueOf(dialogTargetF);
                        temp++;
                        dialogTargetF = String.valueOf(temp%10);
                        dialogTargetM = String.valueOf(temp/10);
                    }
                });





                builder.setPositiveButton(
                        "Confirm",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                String text = display.getText().toString();
                                String temp = text.substring(0,text.length()-1);
                                updateNight(temp);
                                writeUpdating.sendEmptyMessage(0);
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
            }
        });



        // Start the auto-updates
        nightDayTemperature();
        currentTemperature();

        return v;
    }

    void changeTempDialog(String day){

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
                changeTarget = true;
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
                                    target_text.setText("OFFLINE");
                                    target_display.setText("OFFLINE");
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
                if (changeTarget) {
                    try {
                        target_text.setText(targetM + "." + targetF + "\u2103");
                        arc.setProgress(Integer.valueOf(targetM) * 10 - 50 + Integer.valueOf(targetF));
                        changeTarget = false;
                    } catch( Exception e){
                        e.printStackTrace();
                    }
                }


                vacationSwitch.setChecked(vacation);
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
                currentTemp_text.setText(currentTemp + "\u2103");
                date_text.setText(date);
                target_display.setText(targetTemp);
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
    Handler writeUpdating = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            // for Toast we do not make try and catch
            // because getActivity().getApplicationContext() will return null and nothing will be shown
            // thus no errors
            Toast.makeText(getActivity().getApplicationContext(),
                    "Updating...", Toast.LENGTH_LONG).show();
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

    public void getTarget(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (isNetworkAvailable()) {
                        String temp = HeatingSystem.get("targetTemperature");
                        if (temp.length() == 4){
                            targetM = temp.substring(0,2);
                            targetF = temp.substring(3,4);
                        } else {
                            targetM = temp.substring(0,1);
                            targetF = temp.substring(2,3);
                        }

                    } else {
                        writeNoInternet.sendEmptyMessage(0);
                    }
                } catch (Exception e) {
                    System.err.println("Error from getdata " + e);
                }
            }
        }).start();
    }


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
                            vacation = !HeatingSystem.get("weekProgramState").equals("on");
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
                            date = HeatingSystem.get("day") + " " + HeatingSystem.get("time");
                            targetTemp = HeatingSystem.get("targetTemperature");
                            //writing current temperature on UI
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

    public void updateTarget(final String value) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (isNetworkAvailable()) {

                        HeatingSystem.put("targetTemperature", value);

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

    public void setWeeklyProgram(final boolean val){
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (isNetworkAvailable()) {

                        HeatingSystem.put("weekProgramState", val ? "on" : "off");
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

    public void updateDay(final String value) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (isNetworkAvailable() && autoCurrent && autoDayNight) {
                        HeatingSystem.put("dayTemperature", value);

                    } else {
                        writeNoInternet.sendEmptyMessage(0);
                    }
                } catch (Exception e) {
                    System.err.println("Error from getdata " + e);
                }
            }
        }).start();
    }

    //updating night temperature

    public void updateNight(final String value) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (isNetworkAvailable() && autoCurrent && autoDayNight) {
                        HeatingSystem.put("nightTemperature", value);

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
