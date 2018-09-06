package com.gmail.amaarquardi.rccarcontroller;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

/**
 * Created by Amaar on 2017-06-04.
 */
public class PhoneControllerActivity extends Activity {
    private SteeringSliderView steeringSliderView;
    private ThrottleSliderView throttleSliderView;
    private TextView speedTextView;
    private TextView directionTextView;
    private boolean killBackgroundThread;
    private boolean emergencyBrake;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.phone_controller);
        steeringSliderView = findViewById(R.id.steering_slider_view);
        throttleSliderView = findViewById(R.id.throttle_slider_view);
        speedTextView = findViewById(R.id.speed_text_view);
        directionTextView = findViewById(R.id.direction_text_view);
        killBackgroundThread = false;
        emergencyBrake = false;

        InputProcessor.init(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        new Thread(() -> {
            while (true) {
                if (killBackgroundThread) {
                    ArduinoSerialWriter.writeToArduino(InputProcessor.processEmergencyBrake().getBytes());
                    ArduinoSerialWriter.close();
                    return;
                }
                long startTime = System.currentTimeMillis();
                final InputProcessor.ProcessedData data;
                if (emergencyBrake) {
                    data = InputProcessor.processEmergencyBrake();
                    emergencyBrake = false;
                }
                else data = InputProcessor.processInput(steeringSliderView.getSteeringAngle(),
                        throttleSliderView.getThrottlePosition());
                if (data == null) continue;
                if (!writeData(PhoneControllerActivity.this, data.getBytes())) return;

                runOnUiThread(() -> {
                    speedTextView.setText("Speed: " + data.getDrivingSpeed());
                    directionTextView.setText("Steering: " + data.getSteeringDirection());
                });
                long sleepTime = startTime + 16 - System.currentTimeMillis();
                if (sleepTime > 0) {
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException ignore) {}
                }
            }
        }).start();
    }

    private boolean writeData(Activity activity, byte[] data) {
        if (ArduinoSerialWriter.writeToArduino(data)) return true;
        else {
            /*final AlertDialog dialog = new AlertDialog.Builder(activity)
                    .setTitle("Error!")
                    .setMessage("Connection to Arduino lost! Attempting Reconnection.")
                    .setOnDismissListener(dialogInterface -> finish()).create();
            runOnUiThread(dialog::show);*/
            if (!ArduinoSerialWriter.attemptReconnection(10000)) return false;
            else writeData(activity, data);
        }
    }

    public void emergencyBrake(View view) {
        emergencyBrake = true;
        throttleSliderView.resetThrottlePosition();
    }

    @Override
    protected void onPause() {
        super.onPause();
        killBackgroundThread = true;
        finish();
    }
}
