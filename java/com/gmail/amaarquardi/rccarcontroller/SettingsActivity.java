package com.gmail.amaarquardi.rccarcontroller;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;

/**
 * Created by Amaar on 2017-06-04.
 */
public class SettingsActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //add the fragment
        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new SettingsFragment())
                .commit();
    }

    /**
     * A fragment to wrap the XML for this Activity.
     */
    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);
            final Context context = getActivity();

            Preference.OnPreferenceChangeListener doubleListener = (preference, newValue) -> {
                try {
                    double value = Double.valueOf((String) newValue);
                    if (value <= 0) throw new NumberFormatException();
                }
                catch (NumberFormatException e) {
                    new AlertDialog.Builder(context)
                            .setTitle("Invalid!")
                            .setMessage("The value for " + preference.getTitle() + " must be a positive number.")
                            .show();
                    return false;
                }
                return true;
            };
            findPreference("maxSpeed").setOnPreferenceChangeListener(getIntListener(context, 253));
            findPreference("maxAcceleration").setOnPreferenceChangeListener(doubleListener);
            findPreference("centerAngle").setOnPreferenceChangeListener(getIntListener(context, 180));
            findPreference("angularVelocity").setOnPreferenceChangeListener(doubleListener);
            findPreference("maxAngularDisplacement").setOnPreferenceChangeListener(getIntListener(context, 90));
            findPreference("brakingTorqueToEngineTorqueRatio").setOnPreferenceChangeListener(doubleListener);
        }

        private static Preference.OnPreferenceChangeListener getIntListener(final Context context, final int maxValue) {
            return (preference, newValue) -> {
                try {
                    int value = Integer.valueOf((String) newValue);
                    if (value <= 0 || value > maxValue) throw new NumberFormatException();
                }
                catch (NumberFormatException e) {
                    new AlertDialog.Builder(context)
                            .setTitle("Invalid!")
                            .setMessage("The value for " + preference.getTitle() +
                                    " must be a positive integer no bigger than " + maxValue + ".")
                            .show();
                    return false;
                }
                return true;
            };
        }
    }
}
