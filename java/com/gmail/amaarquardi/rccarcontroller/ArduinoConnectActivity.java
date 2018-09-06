package com.gmail.amaarquardi.rccarcontroller;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

/**
 * Created by Amaar on 2017-06-04.
 */
public class ArduinoConnectActivity extends Activity {
    public static final int REQUEST_ENABLE_BLUETOOTH = 1;
    public static final int REQUEST_PHONE_CONTROLLER = 2;
    private TextView notificationTextView;
    private Button tryAgainButton;
    private boolean isVisible;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.arduino_connect);
        notificationTextView = findViewById(R.id.notification_text_view);
        tryAgainButton = findViewById(R.id.try_again_button);
        attemptConnection(tryAgainButton);
        //Turn off that silly stupid disgusting song. That song is really stupid, ask for forgiveness now.
    }

    @Override
    protected void onResume() {
        super.onResume();
        isVisible = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        isVisible = false;
    }

    public void attemptConnection(final View view) {
        notificationTextView.setText(R.string.connecting_to_arduino);
        tryAgainButton.setEnabled(false);
        ArduinoSerialWriter.init(this, connected -> {
            if (!isVisible) {
                finish();
                return;
            }
            runOnUiThread(() -> {
                if (connected)
                    startActivityForResult(new Intent(tryAgainButton.getContext(), PhoneControllerActivity.class),
                            REQUEST_PHONE_CONTROLLER);
                else {
                    notificationTextView.setText(R.string.unable_to_connect);
                    tryAgainButton.setEnabled(true);
                }
            });
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BLUETOOTH && resultCode == RESULT_OK) attemptConnection(tryAgainButton);
        else finish();
    }
}
