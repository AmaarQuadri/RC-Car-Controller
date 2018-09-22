//import the Servo class to be used to control the Servo
#include <Servo.h>

//constants that indicate the various pin numbers
const int SERVO_PIN = 6;
const int MOTOR_DIRECTION_PIN_1 = 2;
const int MOTOR_DIRECTION_PIN_2 = 4;
const int MOTOR_SPEED_PIN = 5;

//Servo that is used to control the servo to steer the RC car
Servo steering;

//runs when the Arduino starts
void setup() {
	//begin the serial to allow incoming bytes from python to be read
	Serial.begin(9600);

	//setup the pins
	steering.attach(SERVO_PIN);
	pinMode(MOTOR_DIRECTION_PIN_1, OUTPUT);
	pinMode(MOTOR_DIRECTION_PIN_2, OUTPUT);
	pinMode(MOTOR_SPEED_PIN, OUTPUT);

	//set the steering and motor to the default values
	steering.write(90);
	analogWrite(MOTOR_SPEED_PIN, 0);
}

//runs indefinately after the setup() function
void loop() {
  //dump anything apart from the last 5 bytes
  while (Serial.available() > 5) Serial.read();

  //keep searching unless there are only 2 bytes left (because then there isn't a full message to be read)
  while (Serial.available() > 2) {
    //if the next byte is not the header byte ignore it and try again
    int headerByte = Serial.read();
    if (headerByte != 254 && headerByte != 255) continue;

    //if the last bit of the header is a 1 set the driving direction to forwards, otherwise set it to backwards
    if (headerByte == 255) {
      digitalWrite(MOTOR_DIRECTION_PIN_1, HIGH);
      digitalWrite(MOTOR_DIRECTION_PIN_2, LOW);
    }
    else {
      digitalWrite(MOTOR_DIRECTION_PIN_1, LOW);
      digitalWrite(MOTOR_DIRECTION_PIN_2, HIGH);
    }

    //assign the second byte to the steering and the third byte to the motor's speed
    steering.write(Serial.read());
    analogWrite(MOTOR_SPEED_PIN, Serial.read());
  }
}
