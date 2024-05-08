#include <Servo.h>

Servo servo;
int servoPin = 3;

void setup() {
  Serial.begin(9600);
  servo.attach(servoPin);
}

void loop() {
  if (Serial.available()) {
    int faceCenterX = Serial.parseInt();
    if (Serial.read() == '\n') {
      int servoPos = map(faceCenterX, 0, 640, 180, 0);  // Инвертирование маппинга
      servo.write(servoPos);
    }
  }
}

