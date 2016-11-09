// bibliotecas para conectividade à wifi e para conecção ao thingspeak
#include <ESP8266WiFi.h>

// dados da rede de wifi
const char* ssid = "House18";
const char* password = "thespinney";

//const char* ssid = "Dlink CAROL";
//const char* password = "93420864";
WiFiClient  client;
String s = "";

// VARIAVEIS DE APLICAÇÃO 
byte table_lamp_not;    // Estado negado do abajur no quarto (saída ativo-baixa)
byte user_entry;  // Alteração no estado do switch do botão do abajur ou pelo aplicativo
byte user_change;
byte light_sensor;    // Estado do sensor de iluminação (LDR)
byte tab_lam_app;     // Estado da variável atualizada pela web
byte sensor_movement;  // Estado do sensor de movimento (PIR)
byte sensor_presence;  // Estado do sensor de presença (sonar)
#define sensor_presence_distance_threshold_cm 75 // para calibrar a distância ao obstáculo padrão
#define sensor_light_dark_threshold 150 // para calibrar em qual valor de leitura acusará luz
byte last_sensor_movement;
byte light_switch;
byte last_light_switch;

// VARIAVEIS DE HARDWARE
byte pin_table_lamp = 5;
byte pin_light_sensor = A0;
byte pin_sensor_movement = 12;
byte pin_sensor_presence_echo = 16;
byte pin_sensor_presence_trig = 14;
byte led = 4;
byte pin_light_switch = 13;

// RELATIVO AO RECEBIMENTO DE TAREFAS
// servidor para onde outros dispositivos enviarão informações
// Create an instance of the server, specify the port to listen on as an argument
WiFiServer server(80);
const char* password8266 = "/14896255";

#define DEBOUCING_INTERVAL_MS 200

void setup() {
  ////Serial.begin(115200);

  pinMode(pin_table_lamp, OUTPUT);
  pinMode(pin_light_sensor, INPUT);  
  pinMode(pin_light_switch, INPUT); 
  pinMode(pin_sensor_movement, INPUT);
  //pinMode(pin_sensor_presence_trig, OUTPUT);
  //pinMode(pin_sensor_presence_echo, INPUT);
  pinMode(led, OUTPUT);

  // INICIALIZA AS VARIAVEIS DE APLICAÇÃO DESLIGADAS
  table_lamp_not = LOW; // Inicializando com o abajur ligado
  light_sensor = LOW;
  tab_lam_app = LOW;
  sensor_presence = LOW;
  user_entry = LOW;
  user_change = LOW;
  digitalWrite(pin_table_lamp, table_lamp_not);
  digitalWrite(pin_sensor_presence_trig, LOW);
  sensor_movement = digitalRead(pin_sensor_movement);
  last_sensor_movement = sensor_movement;
  digitalWrite(led, LOW);
  light_switch = digitalRead(pin_light_switch);
  
  ConnectWifi();
  CreateServer();
}

void loop() {
  LightSwitchCheck();
  MovementCheck();
  //PresenceCheck();
  SenseLight();
  Verify_request();
  delay(10);
}

void ConnectWifi()
{
  /*
  ////Serial.println();
  //Serial.println();

  // We start by connecting to a WiFi network
  //Serial.print("Connecting to ");
  //Serial.println(ssid); */
  WiFi.begin(ssid, password);
  while (WiFi.status() != WL_CONNECTED) { delay(500); } //Serial.print("."); }
  /* 
  //Serial.println("WiFi connected");
  //Serial.println("IP address: ");
  //Serial.println(WiFi.localIP());*/
}

void CreateServer()
{
  server.begin();
  /*
  //Serial.println("Server started");

  // Print the IP address
  //Serial.println("IP address: ");
  //Serial.println(WiFi.localIP()); */
}

void SenseLight()
{ 
  int sensorValue = analogRead(pin_light_sensor);

  if(sensorValue >= sensor_light_dark_threshold)
    light_sensor = HIGH;
  else
    light_sensor = LOW;
}

void LightSwitchCheck()
{
  // VARIAVEIS TEMPORIZADORAS PARA DEBOUNCING
  static unsigned long lsw_last_loop_time = 0;
  unsigned long lsw_loop_time = millis();

  light_switch = digitalRead(pin_light_switch);
  if(light_switch != last_light_switch)
  {
    if (lsw_loop_time - lsw_last_loop_time > DEBOUCING_INTERVAL_MS)
    {
      lsw_last_loop_time = lsw_loop_time;
      last_light_switch = light_switch;
      table_lamp_not = !table_lamp_not; // entrou aqui pra INVERTER o estado da lâmpada, que não necessariamente é o mesmo do switch.
      user_entry = !table_lamp_not;
      user_change = HIGH;
      digitalWrite(pin_table_lamp, table_lamp_not);   // atualiza o estado da lampada
    }
  }
}

void MovementCheck()
{
  // VARIAVEIS TEMPORIZADORAS PARA DEBOUNCING
  static unsigned long mov_last_loop_time = 0;
  unsigned long mov_loop_time = millis();

  sensor_movement = digitalRead(pin_sensor_movement);
  if(sensor_movement != last_sensor_movement)
  {
    if (mov_loop_time - mov_last_loop_time > DEBOUCING_INTERVAL_MS)
    {
      mov_last_loop_time = mov_loop_time;
      last_sensor_movement = sensor_movement;
    }
  }
}


bool PresenceCheck()
{
  long duration, cm;
  
  // Triggering by a HIGH pulse of 2 or more microseconds.
  // Give a short LOW pulse beforehand to ensure a clean HIGH pulse:
  digitalWrite(pin_sensor_presence_trig, LOW);
  delayMicroseconds(5);
  digitalWrite(pin_sensor_presence_trig, HIGH);
  delayMicroseconds(10);
  digitalWrite(pin_sensor_presence_trig, LOW);

  // Signal from the sensor: a HIGH pulse whose duration is the time (in microseconds) 
  // from the sending of the ping to the reception of its echo off of an object.
  duration = pulseIn(pin_sensor_presence_echo, HIGH);

  // The speed of sound is 340 m/s or 29 microseconds per centimeter.
  // The ping travels out and back, so to find the distance of the
  // object we take half of the distance travelled.
  cm = duration / 29 / 2;

  if(cm < sensor_presence_distance_threshold_cm)
    sensor_presence = HIGH;
  else
    sensor_presence = LOW;
}

void Verify_request()
{
  client = server.available();
  if(client)    // verifica se tem novo client
  {
    //Serial.println("New client!");
    if(client.available())
    {
      String req = client.readStringUntil('\r');
      //Serial.print("Request: ");
      //Serial.println(req);
      client.flush();

      // Initialize the response
      s = "HTTP/1.1 200 OK\r\nContent-Type: text/html\r\n\r\n<!DOCTYPE HTML>\r\n<html>\r\n";
            
      /* DAQUI PARA BAIXO VERIFICA O REQUEST DO USUÁRIO! */

      if(req.indexOf("/ID") != -1) // retorna -1 se não encontrar a string
        ESPServer_ID();
      else {
        int pass_index = req.indexOf(password8266);
        if(pass_index != -1) {  // senha correta, agora checar o comando
          if(req.indexOf("/S1", pass_index) != -1)
            ESPServer_S1();
          else if (req.indexOf("/S2", pass_index) != -1)
            ESPServer_S2();
          else if (req.indexOf("/S3", pass_index) != -1)
            ESPServer_S3();
          else if (req.indexOf("/S4", pass_index) != -1)
            ESPServer_S45();
          else if (req.indexOf("/S", pass_index) != -1)
            ESPServer_S();
          else if (req.indexOf("/A1", pass_index) != -1)
          {
            if(req.indexOf("/OFF") != -1) // retorna -1 se não encontrar a string
              tab_lam_app = LOW;  // sets table lamp off
            else if(req.indexOf("/ON") != -1) // retorna -1 se não encontrar a string
              tab_lam_app = HIGH; // sets table lamp on
            else if(req.indexOf("/SIGNAL") != -1)
              ESPServer_A1_SIG(); // returns table lamp state
            else
              s += "<br>Invalid request. Select either /A1/ON or /A1/OFF, or /A1/SIGNAL.<br>";
            USER_ENTRY_Check(req.indexOf("/APP")); // checks whether it was an entry from the app or not
            ESPServer_A1();  
          }
          else if (req.indexOf("/bulb", pass_index) != -1)
          {
            s += "<br>Table lamp is: A1 ";
            s += (table_lamp_not)?"OFF":"ON"; // ao contrário porque o abajur é ativo baixo.
          }
          else
            s += "*** WRONG COMMAND! ***";
        }
        else
          s += "*** WRONG PASSWORD! ***";
      }
      s += "<br>";
      // Finalize the response
      s += "</html>\n";
    
      // Send the response to the client
      delay(1);
      client.print(s);
      delay(1);
      //Serial.println("Client disonnected");
      client.stop();  
    }
  }
}

void ESPServer_S1()
{
  s += "<br>Light sensor reads: S1 ";
  s += (light_sensor)?"LIGHT":"DARK";
}
void ESPServer_S2()
{
  s += "<br>Movement sensor reads: S2  ";
  s += (sensor_movement)?"HIGH":"LOW";
}
void ESPServer_S3()
{
  s += "<br>Presence sensor reads: S3 ";
  s += (sensor_presence)?"HIGH":"LOW";
}
void ESPServer_S45()
{
  s += "<br>User entry reads: S4 ";
  s += (user_entry)?"HIGH":"LOW";
  s += "<br>User change reads: S5 ";
  s += (user_change)?"HIGH":"LOW";
  user_change = LOW; //after it is read, user_change must go low
}
void ESPServer_S()
{
  ESPServer_A1_SIG();
  ESPServer_S1();
  ESPServer_S2();
  ESPServer_S3();
  ESPServer_S45();
}

void ESPServer_A1()
{
  table_lamp_not = !tab_lam_app; // abajur é ativo-baixo
  digitalWrite(pin_table_lamp, table_lamp_not);   // atualiza o estado da abajur

  s += "<br>Table lamp is: A1 ";
  s += (table_lamp_not)?"OFF":"ON"; // ao contrário porque o abajur é ativo baixo.
}

void ESPServer_A1_SIG()
{
  s += "<br>Table lamp is: S0 ";
  s += (table_lamp_not)?"OFF":"ON"; // ao contrário porque o abajur é ativo baixo.
}

void USER_ENTRY_Check(int app)
{
  if(app != -1) {
    user_entry = tab_lam_app;
    user_change = HIGH;
  }
}

void ESPServer_ID() 
{
  byte mac[6];
  WiFi.macAddress(mac);
  
  s += "IDENTITY <br><br>";
  s += "ESP8266 ";
  s.concat(mac[0]);
  s.concat(mac[1]);
  s.concat(mac[2]);
  s.concat(mac[3]);
  s.concat(mac[4]);
  s.concat(mac[5]);
  s += "<br>";
  s += "IP: ";
  s +=WiFi.localIP();
  s += "<br><br><br>";
  s += "******************************************************<br><br>";
  s += "AGENTE <br>";
  s += "Sensors List: <br>";
  s += "- S1: Light sensor (bool - 0/1)<br>";
  s += "- S2: Movement sensor 2 (bool - 0/1)<br>";
  s += "- S3: Presence sensor 2 (bool - 0/1)<br><br>";
  s += "- S4: Table Lamp Switch change (bool - 0/1)<br>";
  
  s += "Actuators List: <br>";
  s += "- A1: Table lamp (bool - On/Off)<br>";
  s += "Messages List: <br>";
  s += "(msg) - (description) - (returns)<br>";
  s += "S - All sensors's current readings - S1,S2,S3<br>";
  s += "S1 - Light sensor current reading - S1<br>";
  s += "S2 - Movement sensor 2 current reading - S2<br>";
  s += "S3 - Presence sensor 2 current reading - S3<br>";
  s += "S4 - Table Lamp switch change current reading - S4<br>";
  s += "A1/ON:OFF - set table lamp on:off - A1<br>";
  s += "A1/SIGNAL - Table lamp current state - S0<br>";
  s += "<br><br>";
  s += "Auto-message-out: (no-more)";
  s += "(trigger) - (description) - (returns)<br>";
  s += "S1 on change - when light's state changes - S1<br>";
  s += "S2 on change - when movement is detected or stopped - S2<br>";
  s += "S3 high - while presence is detected - S3<br>";
  s += "S4 on change - when table lamp switch is changed - S4,A1<br>";
}

