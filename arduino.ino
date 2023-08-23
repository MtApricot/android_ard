int data = 0;
int led = 13;

void setup() {
  pinMode(led, OUTPUT);
  Serial.begin(9600);//シリアル通信開始、転送速度は9600ビット/秒
  
}
 
void loop() {
  if (Serial.available() > 0) {
    data = Serial.read();//シリアル通信で受け取ったデータを読み込む
    if (data == '1') {//1が送られてきたらLEDをON
      digitalWrite(led, HIGH);
      Serial.println("ON");
    
    } else if (data == '0') {//0が送られてきたらLEDをOFF
      digitalWrite(led, LOW);
      Serial.println("OFF");
    }
    delay(10);
  }
}
