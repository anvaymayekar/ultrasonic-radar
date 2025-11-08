#include <Servo.h>
#include <SoftwareSerial.h>

// --- Pin configuration ---
#define TRIG_PIN 8
#define ECHO_PIN 9
#define HC05_TX 2  // Arduino RX (connect to HC-05 TX)
#define HC05_RX 3  // Arduino TX (connect to HC-05 RX)
#define SERVO_PIN 12

// --- Globals ---
SoftwareSerial bluetooth(HC05_TX, HC05_RX);
Servo radarServo;

long duration;
int distance;

// --- Setup ---
void setup() {
    pinMode(TRIG_PIN, OUTPUT);
    pinMode(ECHO_PIN, INPUT);

    Serial.begin(9600);
    bluetooth.begin(9600);

    radarServo.attach(SERVO_PIN);
    radarServo.write(90);  // Start centered
    delay(500);

    Serial.println("Radar System Started");
    Serial.println("Sending data via HC-05 Bluetooth...");
}

// --- Main loop ---
void loop() {
    // Sweep from 0° to 180°
    for (size_t angle = 0; angle <= 180; angle++) {
        radarServo.write(angle);
        delay(15);  // Smooth movement
        distance = calculateDistance();
        sendData(angle, distance);
    }

    // Sweep back from 180° to 0°
    for (size_t angle = 180; angle >= 0; angle--) {
        radarServo.write(angle);
        delay(15);
        distance = calculateDistance();
        sendData(angle, distance);
    }
}

// --- Send angle and distance to Serial + Bluetooth ---
void sendData(int angle, int dist) {
    bluetooth.print(angle);
    bluetooth.print(",");
    bluetooth.print(dist);
    bluetooth.print(".");

    Serial.print(angle);
    Serial.print(",");
    Serial.print(dist);
    Serial.print(".");
}

// --- Ultrasonic distance measurement ---
int calculateDistance() {
    digitalWrite(TRIG_PIN, LOW);
    delayMicroseconds(2);

    digitalWrite(TRIG_PIN, HIGH);
    delayMicroseconds(10);
    digitalWrite(TRIG_PIN, LOW);

    duration = pulseIn(ECHO_PIN, HIGH, 20000);  // 20ms timeout
    int dist = duration * 0.034 / 2;

    // Limit distance to realistic range
    if (dist < 0 || dist > 400) dist = 0;

    return dist;
}
