package com.gmail.amaarquardi.rccarcontroller;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Created by Amaar on 2017-06-10.
 */

public class InputProcessor {
    //VARIABLES THAT CAN BE EDITED PRIOR TO COMPILATION TO TWEAK AND CALIBRATE THE RC CAR'S PERFORMANCE

    /**
     * The angle at which the servo motor causes the wheels to point perfectly straight.
     * This is ideally 90, but may need calibration.
     */
    private static int CENTER_ANGLE;

    /**
     * The largest angle that the servo can turn.
     * This is implemented to prevent the servo motor from encountering mechanical resistance due to a piece in its way.
     * This cannot be greater than 90 because the servo only has 180 degrees of rotation.
     */
    private static int MAX_ANGULAR_DISPLACEMENT;

    /**
     * The angular velocity at which the servo motor rotates.
     */
    private static double ANGULAR_VELOCITY;

    /**
     * Whether or not to switch the direction the motor considers to be forwards.
     * This is for convenience to avoid having to switch the wires or reprogram the Arduino.
     */
    private static boolean REVERSE_MOTOR_DIRECTION;


    //VARIABLES THAT DETERMINE THE STRENGTHS OF THE FORCES ACTING ON THE RC CAR FOR CALCULATING ACCELERATION


    /**
     * Coefficient determining the strength of the engine, which is used in determining the acceleration.
     */
    private static double ENGINE_COEFFICIENT;

    /**
     * Coefficient determining the strength of the brakes, which is used in determining the acceleration.
     */
    private static double BRAKING_COEFFICIENT;

    /**
     * Coefficient determining the strength of friction (including rolling resistance, transmission losses etc.), which is used in determining the acceleration.
     */
    private static double FRICTION_COEFFICIENT;

    /**
     * Coefficient determining the strength of aerodynamic drag, which is used in determining the acceleration.
     */
    private static double DRAG_COEFFICIENT;


    //VARIABLES REPRESENTING THE DATA THAT IS SENT TO THE ARDUINO
    //THIS DATA IS DESIGNED SO THAT THE ARDUINO CAN PROCESS IT AS QUICKLY AS POSSIBLE


    /**
     * The angle at which to set the steering servo to.
     * This value can be directly used by the Arduino.
     */
    private static double steeringAngle;

    /**
     * Whether to drive the motor in a direction that drives the car forwards or backwards.
     * This boolean can be directly used by the Arduino.
     */
    private static boolean isDrivingForwards;

    /**
     * The speed to set the motor at.
     * This value can be directly used by the Arduino.
     */
    private static double drivingSpeed;

    public static class ProcessedData {
        private final byte[] data;
        private final int drivingSpeed;
        private final int steeringDirection;

        private ProcessedData(byte[] data, int drivingSpeed, int steeringDirection) {
            this.data = data;
            this.drivingSpeed = drivingSpeed;
            this.steeringDirection = steeringDirection;
        }

        public byte[] getBytes() {
            return data;
        }

        public int getDrivingSpeed() {
            return drivingSpeed;
        }

        public int getSteeringDirection() {
            return steeringDirection;
        }
    }


    public static void init(Context context) {
        //load constant values from shared preferences
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        CENTER_ANGLE = Integer.valueOf(sharedPreferences.getString("centerAngle", "90"));
        MAX_ANGULAR_DISPLACEMENT = Integer.valueOf(sharedPreferences.getString("maxAngularDisplacement", "75"));
        ANGULAR_VELOCITY = Double.valueOf(sharedPreferences.getString("angularVelocity", "2.88"));
        REVERSE_MOTOR_DIRECTION = sharedPreferences.getBoolean("reverseMotorDirection", false);
        int MAX_SPEED = Integer.valueOf(sharedPreferences.getString("maxSpeed", "100"));
        double MAX_ACCELERATION = Double.valueOf(sharedPreferences.getString("maxAcceleration", "1"));
        double BRAKING_TORQUE_TO_ENGINE_TORQUE_RATIO = Double.valueOf(sharedPreferences.getString("brakingTorqueToEngineTorqueRatio", "1.1"));

        //CALCULATION OF THE COEFFICIENTS BASED ON THE USER-TWEAKED VALUES WHICH IS DONE DURING CLASS LOADING
        //calculates the coefficients for the strengths of the engine, brakes, friction and drag
        //in such a way as to satisfy the requirements for MAX_SPEED, MAX_ACCELERATION, and BRAKING_TORQUE_TO_ENGINE_TORQUE_RATIO
        //the FRICTION_COEFFICIENT is assumed to be 30 times the size of the DRAG_COEFFICIENT
        ENGINE_COEFFICIENT = MAX_ACCELERATION / (BRAKING_TORQUE_TO_ENGINE_TORQUE_RATIO + 1);
        BRAKING_COEFFICIENT = BRAKING_TORQUE_TO_ENGINE_TORQUE_RATIO * ENGINE_COEFFICIENT;
        DRAG_COEFFICIENT = ENGINE_COEFFICIENT / MAX_SPEED / (MAX_SPEED + 30);
        FRICTION_COEFFICIENT = 30 * DRAG_COEFFICIENT;

        //initialize variables
        steeringAngle = CENTER_ANGLE;
        isDrivingForwards = true;
        drivingSpeed = 0;
    }

    /**
     * Determines the acceleration on the RC car by adding the forces due to the engine, brakes, friction and drag.
     * Thus, the result of this method also depends on the RC car's current speed.
     * The method is valid when driving both forwards and backwards.
     *
     * @return The resulting acceleration of the RC car.
     */
    private static double getAcceleration(boolean isDrivingForwards, double throttleInput) {
        return (throttleInput >= 0 == isDrivingForwards ? ENGINE_COEFFICIENT : BRAKING_COEFFICIENT) *
                (isDrivingForwards ? throttleInput : -throttleInput)
                - FRICTION_COEFFICIENT * drivingSpeed
                - DRAG_COEFFICIENT * drivingSpeed * drivingSpeed;
    }

    public static ProcessedData processInput(double steeringInput, double throttleInput) {
        if (Double.isNaN(steeringInput) || Double.isNaN(throttleInput)) return null;

        //update steeringAngle
        double targetAngle = CENTER_ANGLE + MAX_ANGULAR_DISPLACEMENT * steeringInput;
        if (targetAngle > steeringAngle)
            steeringAngle = Math.min(targetAngle, steeringAngle + ANGULAR_VELOCITY);
        else steeringAngle = Math.max(targetAngle, steeringAngle - ANGULAR_VELOCITY);

        //update driving direction
        if (drivingSpeed == 0) isDrivingForwards = throttleInput >= 0;
        //update the drivingSpeed based on the getAcceleration function
        drivingSpeed = Math.max(0, drivingSpeed + getAcceleration(isDrivingForwards, throttleInput));

        //write new values to the Arduino's serial port
        //if isDrivingForwards is true the first byte is "11111111" otherwise it is "11111110"
        //these are represented as -1 and -2 respectively in java because bytes are signed
        //this way isDrivingForwards can be sent along with the header byte
        //the 2 largest byte values are chosen so that they cannot be confused with steeringAngle (because it ranges from 0 to 180)
        //they also cannot be confused with drivingSpeed provided that MAX_SPEED < 254 (which isn't a big loss of control)
        //System.out.println("isDrivingForwards: " + isDrivingForwards + ", steeringAngle: " + steeringAngle + ", drivingSpeed: " + drivingSpeed);
        return new ProcessedData(new byte[]{(byte) (isDrivingForwards != REVERSE_MOTOR_DIRECTION ? -1 : -2),
                (byte) Math.rint(steeringAngle), (byte) Math.rint(drivingSpeed)},
                (int) Math.rint(isDrivingForwards ? drivingSpeed : -drivingSpeed), (int) Math.rint(steeringAngle - CENTER_ANGLE));
    }

    public static ProcessedData processEmergencyBrake() {
        drivingSpeed = 0;
        return new ProcessedData(new byte[]{(byte) (isDrivingForwards != REVERSE_MOTOR_DIRECTION ? -1 : -2),
                (byte) Math.rint(steeringAngle), (byte) Math.rint(drivingSpeed)},
                0, (int) Math.rint(steeringAngle - CENTER_ANGLE));
    }
}
