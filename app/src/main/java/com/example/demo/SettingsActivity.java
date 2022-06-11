package com.example.demo;

import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;

public class SettingsActivity extends AppCompatActivity {
    private SharedPreferences config;
    private AppCompatActivity currentActivity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        currentActivity = this;

        config = getSharedPreferences("config", MODE_PRIVATE);

        Log.d("TEST - config", config.getString("last_update_hash", ""));

/*
config.resizeMediaToCoverScreen
*/
        Switch resizeMediaToCoverScreenSwitch = (Switch) findViewById(R.id.resizeMediaToCoverScreen);

        resizeMediaToCoverScreenSwitch.setChecked(config.getBoolean("resizeMediaToCoverScreen", false));

        resizeMediaToCoverScreenSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                config.edit()
                    .putBoolean("resizeMediaToCoverScreen", resizeMediaToCoverScreenSwitch.isChecked())
                    .commit();
            }
        });

/*
config.dumpLog
*/
        Switch dumpLogSwitch = (Switch) findViewById(R.id.dumpLog);

        dumpLogSwitch.setChecked(config.getBoolean("dumpLog", false));

        dumpLogSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                config.edit()
                        .putBoolean("dumpLog", dumpLogSwitch.isChecked())
                        .commit();
            }
        });

/*
config.transitionDuration
 */
        EditText transitionDurationEditText = (EditText) findViewById(R.id.transitionDuration);

        transitionDurationEditText.setText(String.valueOf(config.getInt("transitionDuration", 2500)));

        transitionDurationEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString();

                if(Integer.valueOf(value) > 0) {
                    config.edit()
                            .putInt("transitionDuration", Integer.valueOf(value))
                            .commit();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

/*
config.imageDisplayDuration
 */

        EditText imageDisplayDurationEditText = (EditText) findViewById(R.id.imageDisplayDuration);

        imageDisplayDurationEditText.setText(String.valueOf(config.getInt("imageDisplayDuration", 10000)));

        imageDisplayDurationEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString();

                if(Integer.valueOf(value) > 0) {
                    config.edit()
                            .putInt("imageDisplayDuration", Integer.valueOf(value))
                            .commit();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

/*
config.cropVideoDurationTo
 */

        EditText cropVideoDurationToEditText = (EditText) findViewById(R.id.cropVideoDurationTo);

        cropVideoDurationToEditText.setText(String.valueOf(config.getInt("cropVideoDurationTo", 10000)));

        cropVideoDurationToEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void afterTextChanged(Editable s) {
                String value = s.toString();

                if(Integer.valueOf(value) >= 0) {
                    config.edit()
                            .putInt("cropVideoDurationTo", Integer.valueOf(value))
                            .commit();
                }
            }

            @Override
            public void beforeTextChanged(CharSequence s, int start,int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });

        Button closeButton = (Button) findViewById(R.id.closeButton);

        closeButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                currentActivity.finish();
                System.exit(0);
            }
        });
    }

}