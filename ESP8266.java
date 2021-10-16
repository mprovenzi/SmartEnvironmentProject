package smartenvironment.devices._interface;	// project's package

import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.util.Logger;

import java.lang.String;
import java.net.*;
import java.io.*;
import java.util.Calendar;
import java.text.SimpleDateFormat;

/* Imports to handle file writing - used only for tests
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
*/

public class ESP8266 extends Agent {
	private Logger myLogger = Logger.getMyLogger(getClass().getName());
	private final static String myESP1ip = "xxx.xxx.x.xxx";
	private final static String myESP2ip = "xxx.xxx.x.xxy";
	private final static String pwd = "xxx";
	private final static String presence_type = "Presence estimation";
	private final static String light_type    = "Light Control";
	private static int[] mysensors = {0, 1, 2, 3, 4, 5};
	private InputStream instream = null;
	private InputStreamReader instreamread = null;
	private ESPloop myloop = null;
	private ReceiveMsg mymsg = null;
	private String ESP1light, ESP2light, ESP1pres, ESP2pres;
	
	private AID[] presence;
	private AID[] light;

	/* variables for writing in a file - used only for tests
	private static String timeStamp2 = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss-SSS").format(Calendar.getInstance().getTime());
	private static String filename = "WifiErrorTests/"+timeStamp2+".txt";
	private static String outstr = "";
	private static int count = 0;
	private static int espcount = 0; */
	
	public int ESPToggleActuator(int actuatorID, boolean value, int esp) {
		int result = -1;
		String inputLine;
		String ip = "";
		
		switch(esp) {
			case 1: ip = myESP1ip;
					break;
			case 2: ip = myESP2ip;
					break;
			default: 
					return result;
		}
		
		String urltoesp = "http://" + ip + "/" + pwd + "/" + "A" + String.valueOf(actuatorID) + "/" + (value ? "ON":"OFF");
		
		BufferedReader in = ESPConnect(urltoesp);
		try {
			while ((inputLine = in.readLine()) != null)
			{
				//System.out.println(inputLine);
				result = decodeLine(inputLine);
			}
		} catch(IOException e) {
			String timeStamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
			System.out.println(timeStamp + " ******** ERROR! ESPTOGGLEACTUATOR FAILED! ");
			System.out.println("IOException caught: "+e.getMessage());
		} finally {
			try {
				if(in!=null)
					in.close();
			} catch(IOException e) {
				String timeStamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
				System.out.println(timeStamp + " ******** ERROR! INPUTSTREAM CLOSE FAILED! ");
				System.out.println("IOException caught: "+e.getMessage());
			}
			return result;
		}
	}
	
	private class ESPloop extends TickerBehaviour {
		protected ESPloop(Agent a, long period) {
			super(a, period); // this MUST be this constructor's first line
		}
		
		protected void onTick() {
			String inputLine;
			int sensorsESP1 = ESPQuerySensors(myESP1ip, 1);
			int sensorsESP2 = ESPQuerySensors(myESP2ip, 2);
			
			if(sensorsESP1==1) {
				ACLMessage msgp1 = new ACLMessage(ACLMessage.INFORM);
				msgp1.addReceiver(presence[0]);
				msgp1.setConversationId("ESP1");
				msgp1.setContent(ESP1pres);
				myAgent.send(msgp1);
				ACLMessage msgl1 = new ACLMessage(ACLMessage.INFORM);
				msgl1.addReceiver(light[0]);
				msgl1.setConversationId("ESP1");
				msgl1.setContent(ESP1light);
				myAgent.send(msgl1);
			}
			if(sensorsESP2==1) {
				ACLMessage msgp2 = new ACLMessage(ACLMessage.INFORM);
				msgp2.addReceiver(presence[0]);
				msgp2.setConversationId("ESP2");
				msgp2.setContent(ESP2pres);
				myAgent.send(msgp2);
				ACLMessage msgl2 = new ACLMessage(ACLMessage.INFORM);
				msgl2.addReceiver(light[0]);
				msgl2.setConversationId("ESP2");
				msgl2.setContent(ESP2light);
				myAgent.send(msgl2);
			}
		}
	}
	
	/******************************************/
	// This behaviour reads input messages
	private class ReceiveMsg extends CyclicBehaviour {
		private final static int ESP1id = 1;
		private final static int ESP2id = 2;
		private final static int LIGHT_BULB = 1;
		private final static int TABLE_LAMP = 1;
		private MessageTemplate templateESPact;
		public ReceiveMsg(Agent a) {
			super(a);
			templateESPact = MessageTemplate.MatchConversationId("ESP Toggle actuators");
		}

		public void action() {
			ACLMessage msgLA = myAgent.receive(templateESPact);
			if(msgLA != null) { 
				String inputLine = msgLA.getContent();
				//String timeStamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
				//System.out.print(timeStamp + " ***** ESP8266 ***** MSG rcv");
				int indexbegin = 0; // so that at the first iteration indexbegin+1=1
				int indexend = inputLine.indexOf("<>",indexbegin+1);
				String subLine;

				while(indexend != -1) { // breaking in substrings because it will come all in one line only
					subLine = inputLine.substring(indexbegin,indexend);
					indexbegin = indexend;
					indexend = inputLine.indexOf("<>",indexbegin+1);
					
					if(subLine.indexOf("LB")  != -1) { 
						int LB = decodeLine(subLine);
						int returned;
						//System.out.print(" LB = "+LB);
						do { returned = ESPToggleActuator(LIGHT_BULB, ((LB==1) ? true:false), ESP1id); } while(returned == -1);
					}
					if(subLine.indexOf("TL")  != -1) { 
						int TL = decodeLine(subLine); 
						//System.out.print(" TL = "+TL);
						int returned;
						do { returned = ESPToggleActuator(TABLE_LAMP, ((TL==1) ? true:false), ESP2id); } while(returned == -1);
					}
					ACLMessage msgLAreply = msgLA.createReply();
					msgLAreply.setPerformative(ACLMessage.CONFIRM);
					myAgent.send(msgLAreply);
				}
				//System.out.println("");
			}
			block(); // this method is blocked until the agent this behaviour belongs to receives a message
		}
	}
	
	public void setup() {
		ESP1light = "";
		ESP2light = "";
		ESP1pres  = "";
		ESP2pres  = "";
		
		addBehaviour(new WakerBehaviour(this, 1000) { // wait 1 second for handling startup time before start working
				protected void handleElapsedTimeout() {
					try {
						ServiceDescription sd = new ServiceDescription();
						// SEARCH FOR THE PRESENCE ESTIMATION AGENT
						DFAgentDescription templatep = new DFAgentDescription();
						sd.setType(presence_type);
						templatep.addServices(sd);
						DFAgentDescription [] resultp = DFService.search(myAgent, templatep);
						presence = new AID[resultp.length];
						presence[0] = resultp[0].getName();
						// SEARCH FOR THE LIGHT CONTROL AGENT
						DFAgentDescription templatel = new DFAgentDescription();
						sd.setType(light_type);
						templatel.addServices(sd);
						DFAgentDescription [] resultl = DFService.search(myAgent, templatel);
						light = new AID[resultl.length];
						light[0] = resultl[0].getName();
						mymsg = new ReceiveMsg(myAgent);
						myloop = new ESPloop(myAgent, 750); // 7500 -> worked, but slow on anticipation
						addBehaviour(myloop);
						addBehaviour(mymsg);
					} catch (FIPAException e) {
						myLogger.log(Logger.SEVERE, "Agent "+getLocalName()+" - Cannot find from DF", e);
					}
				}
			});
	}
	
	
	private int ESPQuerySensors(String ip, int esp) {		// fazer retornar int, trabalhando com bits.
		int result = -1;	// this one needs to start in 0 because of sensor2value
		int sensor0value = -1;
		int sensor1value = -1;
		int sensor2value = -1;
		int sensor3value = -1;
		int sensor4value = -1;
		int sensor5value = -1;
		String inputLine;
		String urltoesp = "http://" + ip + "/" + pwd + "/" + "S";
		
		BufferedReader in = ESPConnect(urltoesp);
		try {
			while ((inputLine = in.readLine()) != null) {
				int indexbegin = 0; // so that at the first iteration indexbegin+1=1
				int indexend = inputLine.indexOf("<br>",indexbegin+1);
				String subLine;

				while(indexend != -1) { // breaking in substrings because it may come all in one line only
					subLine = inputLine.substring(indexbegin,indexend);
					indexbegin = indexend;
					indexend = inputLine.indexOf("<br>",indexbegin+1);
					if(subLine.indexOf(String.valueOf(mysensors[0])) != -1) { // searching for first sensor term in the current line
						sensor0value = decodeLine(subLine);
					}
					if(subLine.indexOf(String.valueOf(mysensors[1])) != -1) { // searching for first sensor term in the current line
						sensor1value = decodeLine(subLine);
					}
					if(subLine.indexOf(String.valueOf(mysensors[2])) != -1) { // searching for second sensor term in the current line
						sensor2value = decodeLine(subLine);
					}
					if(subLine.indexOf(String.valueOf(mysensors[3])) != -1) { // searching for third sensor term in the current line
						sensor3value = decodeLine(subLine);
					}
					if(subLine.indexOf(String.valueOf(mysensors[4])) != -1) { // searching for third sensor term in the current line
						sensor4value = decodeLine(subLine);
					}
					if(subLine.indexOf(String.valueOf(mysensors[5])) != -1) { // searching for first sensor term in the current line
						sensor5value = decodeLine(subLine);
					}
				}
			}
			if(esp==1) {
				ESP1light = "."+String.valueOf(sensor0value)+"."+String.valueOf(sensor1value)+"."+String.valueOf(sensor5value)+"."; // agent light reads sensors 0, 1 and 5 from esp1
				ESP1pres  = "."+String.valueOf(sensor2value)+"."+String.valueOf(sensor3value)+".";
			} else if(esp==2) { 
				ESP2light = "."+String.valueOf(sensor0value)+"."+String.valueOf(sensor1value)+"."+String.valueOf(sensor4value)+"."+String.valueOf(sensor5value)+"."; // agent light reads sensors 0, 1, 4 and 5 from esp2
				ESP2pres  = "."+String.valueOf(sensor2value)+"."+String.valueOf(sensor3value)+".";
			}
			
			result = 1;
		} catch(IOException e) {
			String timeStamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
			System.out.println(timeStamp + " ******** ERROR! ESPQUERY4SENSORS FAILED! ");
			System.out.println("IOException caught: "+e.getMessage());
			result = -1;
		} finally {
			try {
				if(instream != null) instream.close();
			} catch(IOException e) {
				System.out.println("IOException caught closing streams 1: "+e.getMessage());
			}
			try {
				if(instreamread != null) instreamread.close();
			} catch(IOException e) {
				System.out.println("IOException caught closing streams 2: "+e.getMessage());
			}
			try {
				if(in!=null)
					in.close();
			} catch(IOException e) {
				String timeStamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
				System.out.println(timeStamp + " ******** ERROR! INPUTSTREAM CLOSE FAILED! ");
				System.out.println("IOException caught: "+e.getMessage());
			}
			return result;
		}
	}
	
	private int decodeLine(String inputLine) {
		int value = -1;
		if((inputLine.indexOf("HIGH") != -1) || (inputLine.indexOf("LIGHT") != -1) || (inputLine.indexOf("ON")  != -1))
			value = 1; 
		else
		if((inputLine.indexOf("LOW")  != -1) || (inputLine.indexOf("DARK")  != -1) || (inputLine.indexOf("OFF") != -1))
			value = 0; 
		return value;
	}
	
	private BufferedReader ESPConnect(String espurl) {
		BufferedReader in = null;	
		/* variables for writing in a file - used only for tests
		long millistook, millisbefore, millisafter;
		PrintWriter pwFile = null;
		boolean exc = false;
	
		millisbefore = 0;
		millisafter = 0; */
		try {
			//pwFile = new PrintWriter(filename);
			// count++;
			URL query = new URL(espurl);
			
			//millisbefore = System.currentTimeMillis(); // captures the time just before opening the connection to the device - used only for tests
			
			URLConnection con = query.openConnection();
			con.setConnectTimeout(5000);
			con.setReadTimeout(5000);
			instream = con.getInputStream();
			instreamread = new InputStreamReader(instream);
			in = new BufferedReader(instreamread);
			
			// millisafter = System.currentTimeMillis(); // captures the time just after opening the connection to the device - used only for tests
		} catch(SocketTimeoutException e) { 
			String timeStamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime()); 
			System.out.println(timeStamp +" !!!!!!! ESPCONNECT ESP URL TIMEOUT! Msg: "+ e.getMessage()); //e.printStackTrace();
		} catch(SecurityException | IOException e) { // FileNotFoundException is a subclass of IOException
			String timeStamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime()); 
			System.out.println(timeStamp +" !!!!!!! ESPCONNECT ESP IOEXCEPTION OR SECURITYEXCEPTION! Msg: "+ e.getMessage());
			//e.printStackTrace();
			in = null;
		} 
		/*catch(IOException e) { // this routine captures to a file the moment when this exception is thrown - used only for tests, so exception was placed in the previous catch
			millisafter = System.currentTimeMillis();
			exc = true;
			espcount++;
			String timeStamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
			System.out.println(timeStamp + " ******** ERROR! ESPCONNECT FAILED! URL created: "+espurl);
			System.out.println("IOException caught: "+e.getMessage());
			in = null; }*/ 
		finally {
			/*if(pwFile != null) { // this routine writes to a file how long it took to run the procedure - used only for tests
				millistook = millisafter - millisbefore;
				outstr += String.valueOf(count) + " " + String.valueOf(millistook) ;
				
				if(exc) outstr += " ESPERROR " + String.valueOf(espcount);
				outstr += "\n";
				pwFile.write(outstr);
                pwFile.close();
			}*/
			return in;
		}
		
		/*finally { // unused - previous stage of development
		   try {
				if(instream != null) instream.close();
			} catch(IOException e) {
				System.out.println("IOException caught closing streams 1: "+e.getMessage());
			}
			try {
				if(instreamread != null) instreamread.close();
			} catch(IOException e) {
				System.out.println("IOException caught closing streams 2: "+e.getMessage());
			}
			try {
				if(in!=null)
					in.close();
			} catch(IOException e) {
				System.out.println("******** ERROR! INPUTSTREAM CLOSE FAILED! ");
				System.out.println("IOException caught: "+e.getMessage());
			}
		}*/
	}
	
	/* 
	 * METHODS BELOW ARE LEFT OVERS METHODS FROM THE TIME 
	 * WHEN ESP8266 WAS NOT AN AGENT, BUT ONLY A CLASS
	 */
	
	/*public ESP8266(String ip_ad, String password) { 
		this.ip = ip_ad;
		this.pwd = password;
	}*/

	/* public int ESPQuerySensor(int sensorID) {
		int result = -1;
		String inputLine;
		String urltoesp = "http://" + ip + "/" + pwd + "/" + "S" + String.valueOf(sensorID);
		
		BufferedReader in = ESPConnect(urltoesp);
		try {
			while ((inputLine = in.readLine()) != null)
			{
				//System.out.println(inputLine);
				result = decodeLine(inputLine);
			}
		} catch(IOException e) {
			String timeStamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
			System.out.println(timeStamp + " ******** ERROR! ESP QUERY SENSOR FAILED! ");
			System.out.println("IOException caught: "+e.getMessage());
		} finally {
			try {
				if(in!=null)
					in.close();
			} catch(IOException e) {
				String timeStamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
				System.out.println(timeStamp + " ******** ERROR! INPUTSTREAM CLOSE FAILED! ");
				System.out.println("IOException caught: "+e.getMessage());
			}
			return result;
		}
	}*/ 
	
	/*public int ESPQuery2Sensors(int sensorID1, int sensorID2) {
		int result = 0;	// this one needs to start in 0 because of sensor2value
		int sensor1value = -1;
		int sensor2value = -1;
		String inputLine;
		String urltoesp = "http://" + ip + "/" + pwd + "/" + "S";
		
		BufferedReader in = ESPConnect(urltoesp);
		try {
			while ((inputLine = in.readLine()) != null)
			{
				int indexbegin = 0; // so that at the first iteration indexbegin+1=1
				int indexend = inputLine.indexOf("<br>",indexbegin+1);
				String subLine;

				while(indexend != -1) { // breaking in substrings because it may come all in one line only
					subLine = inputLine.substring(indexbegin,indexend);
					indexbegin = indexend;
					indexend = inputLine.indexOf("<br>",indexbegin+1);
					//System.out.println(subLine);
					if(subLine.indexOf(String.valueOf(sensorID1)) != -1) { // searching for first sensor term in the current line
						//System.out.println(" SensorID1 aqui: " + subLine);
						sensor1value = decodeLine(subLine);
						//System.out.println(" Sensor1value: " + sensor1value);
						result += sensor1value;		// units digit = sensor1value
					}
					if(subLine.indexOf(String.valueOf(sensorID2)) != -1) { // searching for second sensor term in the current line
						//System.out.println(" SensorID2 aqui: " + subLine);
						sensor2value = decodeLine(subLine);
						//System.out.println(" Sensor2value: " + sensor2value);
						result += sensor2value*10;	// tens digit  = sensor2value if any
					}
				}
			}
			if( (sensor1value == -1) && (sensor2value == -1) ) // -1 if neither sensor1value nor sensor2value
				result = -1;	// if only one of them is not found, it is assumed to be 0.
		} catch(IOException e) {
			String timeStamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
			System.out.println(timeStamp + " ******** ERROR! ESPQUERY2SENSORS FAILED! ");
			System.out.println("IOException caught: "+e.getMessage());
			result = -1;
		} finally {
			try {
				if(instream != null) instream.close();
			} catch(IOException e) {
				System.out.println("IOException caught closing streams 1: "+e.getMessage());
			}
			try {
				if(instreamread != null) instreamread.close();
			} catch(IOException e) {
				System.out.println("IOException caught closing streams 2: "+e.getMessage());
			}
			try {
				if(in!=null)
					in.close();
			} catch(IOException e) {
				String timeStamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
				System.out.println(timeStamp + " ******** ERROR! INPUTSTREAM CLOSE FAILED! ");
				System.out.println("IOException caught 3: "+e.getMessage());
			}
			return result;
		}
	}*/

	/******************************************/
	protected void takeDown() {
		doDelete();
	}
}
