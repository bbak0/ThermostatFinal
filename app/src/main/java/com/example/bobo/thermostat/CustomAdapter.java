package com.example.bobo.thermostat;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import java.util.ArrayList;

import util.*;

import static java.lang.Thread.sleep;

/**
 * Created by Bobo on 6/14/2017.
 */

public class CustomAdapter extends BaseAdapter {
    Context c;
    int[] images = new int[100];
    ArrayList<Switch> switches;
    CharSequence options[] = new CharSequence[] {"Modify switch", "Delete switch"};
    //Fragment fragment;
    // string used to identify with which fragment is the user interacting
    String day;
    // weekprogram for modification/deletion and for verification
    WeekProgram wpg_md;
    WeekProgram wpg_v;

    // thread used for retrieving data in order to delete/modify it
    Thread dataThread;
    // switch used for deleting/modifying
    Switch s2;
    // string used to identify switch and delete/modify it
    //String time_modifyOrDeleteData;


    // thread used for retrieving data in order to verify it after deletion/modification
    Thread verifyThread;
    // string used to verify deletion/modification of switch
    String time_verify;

    // thread used in the modifySwitchDialog
    Thread dialogThread;
    WeekProgram wpg_dialog;

    public CustomAdapter(Context c, ArrayList<Switch> switches, int[] images, String day) {
        this.c = c;
        this.switches = switches;
        this.images = images;
        this.day = day;
    }

    // updating data, or basically making a connection between the new switch from xActivity
    // to the old switch from the adapter
    public void updateData(ArrayList<Switch> switches, int[] images) {
        this.switches = switches;
        this.images = images;

    }

    @Override
    public int getCount() {
        return switches.size();
    }

    @Override
    public Object getItem(int i) {
        try {
            return switches.get(i);
        } catch (Exception e) {
            Log.e("getItem()", "The adapter is returning a switch that does not exist!!");
            return null;
        }
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        View view = convertView;
        if (view == null) {
            view = LayoutInflater.from(c).inflate(R.layout.custom_adapter, viewGroup, false);
        }
        final Switch s = (Switch) this.getItem(i);
        ImageView img = (ImageView) view.findViewById(R.id.imageView);
        TextView temperatureTxt = (TextView) view.findViewById(R.id.temperature);
        TextView dateTxt = (TextView) view.findViewById(R.id.date);
        ImageButton editButton = (ImageButton) view.findViewById(R.id.imageButton);
        try {
            temperatureTxt.setText(capitalize(s.getType()));
            dateTxt.setText(s.getTime());
            img.setImageResource(images[i]);
            editButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    selectionDialog(s);
                }
            });
        } catch (Exception e) {
            Log.e("getView()","from CustomAdapter, writing on object that does not exist");
        }
        //Log.e("color",String.valueOf(dateTxt.getShadowColor()));
        //if ((temperatureTxt.getText().toString()).equals("day")) {
        //   img.setImageResource(R.drawable.sun);
        //    Log.e("Error", "Day");
        //}
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(c, s.getTime(), Toast.LENGTH_SHORT).show();
                //v.setBackgroundColor(Color.BLUE);
                //v.setSelected(true);
            }
        });

        view.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //displaying the selection dialog
                selectionDialog(s);
                return true;
            }
        });
        return view;
    }

    // START FROM HERE
    // writing modification in progress
    Handler writeTypeExceeded = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(c,
                    "You have more than 5 switches of the same temperature! Try a different temperature!", Toast.LENGTH_SHORT).show();
        }
    };

    // writing modification in progress
    Handler writeModificating = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(c,
                    "Modificating", Toast.LENGTH_SHORT).show();
        }
    };

    // writing modification Succesful
    Handler writeModificationSuccesful = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(c,
                    "Modification successful!", Toast.LENGTH_SHORT).show();
        }
    };

    // writing modification unsuccesful
    Handler writeModificationUnsuccesful = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(c,
                    "Modification unsuccessful!", Toast.LENGTH_SHORT).show();
        }
    };

    // writing deletion in progress
    Handler writeDeleting = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(c,
                    "Deleting", Toast.LENGTH_SHORT).show();
        }
    };

    // writing deletion Succesful
    Handler writeDeletionSuccesful = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(c,
                    "Deletion successful!", Toast.LENGTH_SHORT).show();
        }
    };

    // writing deletion unsuccesful
    Handler writeDeletionUnsuccesful = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Toast.makeText(c,
                    "Deletion unsuccessful!", Toast.LENGTH_SHORT).show();
        }
    };



    // displaying the selection pop-up dialog for long-press on switch
    void selectionDialog(Switch s1) {
        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle("Please select an option");

        // android errors
        s2 = s1;

        // list of options
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    // modify switch
                    modifySwitchDialog(s2.getTime(), s2.getType());
                    dialog.cancel();
                } else if (which == 1) {
                    // delete switch
                    deleteSwitch(s2.getTime());
                    dialog.cancel();
                }
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
        builder.show();
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

        // I assumed that switches is "new" enough to be used
        for (int i = 0; i < (switches.size()); i++) {
            if (switches.get(i).getType().equals("day"))
                nr ++;
            }
        return nr;
    }

    // count the number of Day switches that are active until now
    int nrNightSwitchesOn() {
        int nr = 0;

        // I assumed that switches is "new" enough to be used
        for (int i = 0; i < (switches.size()); i++) {
            if (switches.get(i).getType().equals("night"))
                nr ++;
        }
        return nr;
    }

    // pop-up dialog in which the user can modify the switch
    void modifySwitchDialog(final String time_selected, final String type_selected) {

        AlertDialog.Builder builder = new AlertDialog.Builder(c);
        builder.setTitle("");

        // custom layout
        LayoutInflater inflater = (LayoutInflater) c.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
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
        // setting the time input to the selected one
        //desiredTime_text.setText(time_selected);
        final TextView daySwitchesLeft = (TextView)dialoglayout.findViewById(R.id.dialog_day_left);
        final TextView nightSwitchesLeft = (TextView)dialoglayout.findViewById(R.id.dialog_night_left);
        final TextView dayTemp = (TextView)dialoglayout.findViewById(R.id.dayTemp_text);
        final TextView nightTemp = (TextView)dialoglayout.findViewById(R.id.nightTemp_text);

        daySwitchesLeft.setText(nrDaySwitchesOn() + "/5 used");
        nightSwitchesLeft.setText(nrNightSwitchesOn() + "/5 used");
        
        //24 hour format for the time picker
        timePicker.setIs24HourView(true);
        
        // setting the picker to match the same time as the selected switch
        timePicker.setHour(Integer.parseInt(time_selected.substring(0,2)));
        timePicker.setMinute(Integer.parseInt(time_selected.substring(3,5)));
        

        new Thread(new Runnable() {
            @Override
            public void run() {

                try {
                    final String dayTempVal = HeatingSystem.get("dayTemperature");
                    final String nightTempVal = HeatingSystem.get("nightTemperature");
                    Log.d("temps",dayTempVal + " " + nightTempVal);
                    dayTemp.post(new Runnable() {
                        @Override
                        public void run() {
                            dayTemp.setText(dayTempVal + "\u2103");
                        }
                    });
                    nightTemp.post(new Runnable() {
                        @Override
                        public void run() {
                            nightTemp.setText(nightTempVal + "\u2103");
                        }
                    });
                } catch (Exception e) {
                    System.err.println("Error from getdata "+e);
                }
            }
        }).start();

        // setting the availability of the radio buttons in accordance with the nr of day/night switches on until now
        // I have assumed that switches is "new" enough and that there is no need to start a new thread
        // in this way we do not exceed the max nr of day/night switches
        if (nrDaySwitchesOn() == 5)
            dayTemperature_radioButton.setEnabled(false);
        if (nrNightSwitchesOn() == 5)
            nightTemperature_radioButton.setEnabled(false);

        // if the user wants to modify a switch, it does not matter if the maximum nr of switches has been achieved or not
        // because we do not introduce a new switch, we just change the existing one, thus the nr of switches is the same
        if (type_selected.equals("day")) {
            dayTemperature_radioButton.setEnabled(true);
            dayTemperature_radioButton.setChecked(true);
        }

        if (type_selected.equals("night")) {
            nightTemperature_radioButton.setEnabled(true);
            nightTemperature_radioButton.setChecked(true);
        }

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
                //String time_desired = String.valueOf(desiredTime_text.getText());
                String time_desired = String.format("%02d:%02d", timePicker.getHour(), timePicker.getMinute());
                // add a 0 in case the user inputs something like: 2:36
                // apparently the server does not accept 2:36
                time_desired = addZero(time_desired);

                // CHECK TO SEE IF THE TIME HAS A GOOD FORMAT !!!!!!
                if (!goodFormat(time_desired)) {
                    Toast.makeText(c,
                            "Invalid time or format HH:MM not respected!", Toast.LENGTH_SHORT).show();
                    return;
                    // in this way the pop-up dialog is not closed!
                }

                // check to see if the user wants to change the time of the selected switch
                // if time is not changed, this means that the user wants only to change the type (day/night)
                // thus there is no need to verify that the desired switch is available, because we already know
                // that the selected switch to modify is available (thus, it has no duplicate)
                if (!time_desired.equals(time_selected)) {
                    // time is changed
                    // CHECK TO SEE IF THE TIME IS AVAILABLE !!!!!!
                    // CHECK TO SEE IF THERE ARE SWITCHES AVAILABLE!!!!
                    // by all of these I mean check to see if there is already another ON switch in that desired time
                    // i have not created a new thread to retrieve the latest data
                    // I have assumed that switches is "new" enough and that there is no need to start a new thread
                    for (int i = 0; i < (switches.size()); i++) {
                        if (time_desired.equals(switches.get(i).getTime()) &&
                                switches.get(i).getState()) {
                            Toast.makeText(c,
                                    "There is already an active switch in that desired time! Please choose a different time!",
                                    Toast.LENGTH_SHORT).show();
                            return;
                            // in this way the pop-up dialog is not closed!
                        }
                    }
                }


                //getting the id of the selected
                int radioButtonID = -1;
                radioButtonID = radioButtonGroup.getCheckedRadioButtonId();
                View radioButton = radioButtonGroup.findViewById(radioButtonID);
                int idx = radioButtonGroup.indexOfChild(radioButton);
                Log.e("radioButton", String.valueOf(idx));


                // CHECK TO SEE IF SOMETHING HAS BEEN SELECTED
                // by something I mean radioButtons
                if (idx == 0) {
                    // if day selected
                    modifySwitch(time_selected, "day", time_desired);
                    dialog.dismiss();
                } else if (idx == 1) {
                    // if night selected
                    modifySwitch(time_selected, "night", time_desired);
                    dialog.dismiss();
                } else {
                    Toast.makeText(c,
                            "Please select a temperature!", Toast.LENGTH_SHORT).show();
                }
            }
        });

    }

    // verifying that the desired switch has been modified
    void verifyModify(final String time_selected, final String type_desired, final String time_desired) {
        //retrieving data
        verifyThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Get the week program
                    wpg_v = HeatingSystem.getWeekProgram();

                    Log.e("verifyModify()", "retrieving data was successful");

                    // verify that the desired switch exists
                    for (int i = 0; i < (wpg_v.getDay(day).size()); i++) {
                        if (time_desired.equals(wpg_v.getDay(day).get(i).getTime()) &&
                                wpg_v.getDay(day).get(i).getState() &&
                                type_desired.equals(wpg_v.getDay(day).get(i).getType())) {
                            // modification was successful
                            writeModificationSuccesful.sendEmptyMessage(0);
                            return;
                        }
                    }
                    // modification was unsucessful
                    writeModificationUnsuccesful.sendEmptyMessage(0);
                } catch (Exception e) {
                    System.err.println("Error from getdata in verifyDelete() from CustomAdapter " + e);
                }
            }
        });
        // starting the thread for retrieving the list
        verifyThread.start();
    }

    void modifySwitch(final String time_selected, final String type_desired, final String time_desired) {

        //retrieving data
        dataThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Get the week program
                    wpg_md = HeatingSystem.getWeekProgram();

                    Log.e("modifySwitch()"," retrieving data was successful " + time_selected);

                    writeModificating.sendEmptyMessage(0);

                    // identifying the selected switch to be modified
                    for (int i = 0; i < (wpg_md.getDay(day).size()); i++) {
                        if (time_selected.equals(wpg_md.getDay(day).get(i).getTime()) &&
                                wpg_md.getDay(day).get(i).getState()) {

                            Log.e("modifySwitch()","item found!");

                            //checking to see if the switch changes its type as well
                            // i have not assumed that there are still available switches
                            if (!type_desired.equals(wpg_md.getDay(day).get(i).getType())) {
                                // the switch needs to change its type
                                // searching for an offline switch to change
                                boolean switch_available = false;
                                for (int j = 0; j < (wpg_md.getDay(day).size()); j++) {
                                    if (//!time_selected.equals(wpg_md.getDay(day).get(j).getTime()) &&
                                            type_desired.equals(wpg_md.getDay(day).get(j).getType()) &&
                                            !wpg_md.getDay(day).get(j).getState()) {
                                        wpg_md.getDay(day).get(j).setType(wpg_md.getDay(day).get(i).getType());
                                        // there exists an opposite offline switch from which
                                        // we can steal its type and give him ours (exchange)
                                        switch_available = true;
                                        Log.e("modifySwitch()", "opposite switch found! " + String.valueOf(j));
                                        break;
                                    }
                                }
                                if (!switch_available) {
                                    writeTypeExceeded.sendEmptyMessage(0);
                                }
                            }
                            //modifying the desired switch
                            wpg_md.getDay(day).get(i).setState(true);
                            wpg_md.getDay(day).get(i).setTime(time_desired);
                            wpg_md.getDay(day).get(i).setType(type_desired);
                            Log.e("modifySwitch()","item found and modified!");
                            break;
                        }
                    }
                    // updating the schedule
                    HeatingSystem.setWeekProgram(wpg_md);
                    wait(2000);

                    // informing the adapter that the list has been changed !!!
                    // VERY IMPORTANT, IF NOT IT WILL RESULT IN CRASHES !!!!
                    notifyDataSetChanged();

                    // verifying the modification
                    verifyModify(time_selected, type_desired, time_desired);
                } catch (Exception e) {
                    System.err.println("Error from getdata in deleteSwitch() from CustomAdapter " + e);
                    // verifying the modification
                    verifyModify(time_selected, type_desired, time_desired);
                }
            }
        });
        // starting the thread for retrieving the list
        dataThread.start();
    }

    // verify that the desired switch was deleted successfully
    void verifyDelete(final String time_selected) {

        //retrieving data
        verifyThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Get the week program
                    wpg_v = HeatingSystem.getWeekProgram();

                    Log.e("verifySwitch()", "retrieving data was successful");

                    // verify that all the desired switches are turned off
                    for (int i = 0; i < (wpg_v.getDay(day).size()); i++) {
                        if (time_selected.equals(wpg_v.getDay(day).get(i).getTime()) &&
                                wpg_v.getDay(day).get(i).getState()) {
                            writeDeletionUnsuccesful.sendEmptyMessage(0);
                            return;
                        }
                    }
                    writeDeletionSuccesful.sendEmptyMessage(0);
                } catch (Exception e) {
                    System.err.println("Error from getdata in verifyDelete() from CustomAdapter " + e);
                }
            }
        });
        // starting the thread for retrieving the list
        verifyThread.start();
    }


    // deleting the desired switch (by time)
    // by deleting we mean setting it to off
    void deleteSwitch(final String time_selected) {

        //retrieving data
        dataThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Get the week program
                    wpg_md = HeatingSystem.getWeekProgram();

                    Log.e("deleteSwitch()"," retrieving data was successful");

                    // identifying the switch from the desired day
                    for (int i = 0; i < (wpg_md.getDay(day).size()); i++) {
                        if (time_selected.equals(wpg_md.getDay(day).get(i).getTime()) &&
                                wpg_md.getDay(day).get(i).getState()) {
                            //turning off the desired switch
                            Log.e("deleteSwitch()","item found and modified!");
                            wpg_md.getDay(day).get(i).setState(false);
                            break;
                        }
                    }
                    // updating the schedule
                    HeatingSystem.setWeekProgram(wpg_md);
                    writeDeleting.sendEmptyMessage(0);
                    wait(2000);

                    // informing the adapter that the list has been changed !!!
                    // VERY IMPORTANT, IF NOT IT WILL RESULT IN CRASHES !!!!
                    notifyDataSetChanged();

                    // verifying the deletion
                    verifyDelete(time_selected);
                } catch (Exception e) {
                    System.err.println("Error from getdata in deleteSwitch() from CustomAdapter " + e);
                    // verifying the deletion
                    verifyDelete(time_selected);
                }
            }
        });
        // starting the thread for retrieving the list
        dataThread.start();

    }
    private String capitalize(final String line) {
        return Character.toUpperCase(line.charAt(0)) + line.substring(1);
    }

}
