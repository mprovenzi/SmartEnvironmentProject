package smartenvironment.DataBaseAgent;

// directory home/pi, export CLASSPATH=%CLASSPATH:.:jade/lib/jade.jar:thingspeak-java/dist/thingspeak-1.1.1.jar

// necessary packages
import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.util.Logger;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import java.lang.String;
import com.angryelectron.thingspeak.Entry;
import com.angryelectron.thingspeak.Channel;
import com.angryelectron.thingspeak.ThingSpeakException;
import com.mashape.unirest.http.exceptions.UnirestException;
import jade.lang.acl.MessageTemplate;

public class DataBaseAgent extends Agent {
	private Logger myLogger = Logger.getMyLogger(getClass().getName());
	private MessageTemplate templateLA;
	private MessageTemplate templatePA;
	
	private Channel LAchannel = new Channel(xxx, "xxx");
	private Channel PAchannel = new Channel(xxx, "xxx");
	private long[] LAuploadtime = new long[2];
	private long[] PAuploadtime = new long[2];
	private boolean LAuploadflag = false;
	private boolean PAuploadflag = false;
	// thingspeak fields for entries
	// light agent
	private final static int LAfieldLS 	= 1; // light sensor field 
	private final static int LAfieldLB 	= 2; // light bulb
	private final static int LAfieldUE1 	= 3; // user entry lb
	private final static int LAfieldAT1	= 4; // automated entry lb
	private final static int LAfieldTL 	= 5; // table light
	private final static int LAfieldUE2 	= 6; // user entry tl
	private final static int LAfieldAT2 	= 7; // automated entry tl
	private final static int LAfieldUC  	= 8; // user change 
	// presence agent
	private final static int PAfieldPres= 1; // presence 
	private final static int PAfieldM1 	= 2; // movement sensor 1
	private final static int PAfieldP1 	= 3; // presence sensor 1
	private final static int PAfieldM2 	= 4; // movement sensor 2
	private final static int PAfieldP2 	= 5; // presence sensor 2
	private final static int PAfieldPS 	= 6; // presence, string
	
	// light agent variables
	private int[] DBestadoLS	= new int[2]; // index 0 = current state, index 1 = previous state
	private int[] DBestadoLB 	= new int[2];
	private int[] DBestadoUE1	= new int[2];
	private int[] DBestadoAT1	= new int[2];
	private int[] DBestadoTL	= new int[2];
	private int[] DBestadoUE2	= new int[2];
	private int[] DBestadoAT2	= new int[2];
	private int[] DBestadoUC    	= new int[2];
	
	// presence agent variables
	private int[] DBestadoPres 	= new int[2]; 
	private int[] DBestadoM1 	= new int[2]; 
	private int[] DBestadoP1 	= new int[2];
	private int[] DBestadoM2 	= new int[2];
	private int[] DBestadoP2 	= new int[2];
	
	private Entry LAentry;	
	private Entry PAentry;
	
	/******************************************/
	// This behaviour is this agent's main
	private class DBAgentLoop extends TickerBehaviour {
		private long looptime, lastlooptime;
		private int counter = 0;
		protected DBAgentLoop(Agent a, long period) {
			super(a, period); // this MUST be this constructor's first line
			looptime = 0;
			lastlooptime = 0;
		}

		protected void onTick() {
			try {
				// update lightagent channel
				LAuploadtime[0] = System.currentTimeMillis();
				if( ((LAuploadtime[0] - LAuploadtime[1]) >= 15000)&& LAuploadflag) {
					int LAup_res = LAchannel.update(LAentry); // this command might throw exception
					if(LAup_res>0) {
						LAuploadflag = false;
						LAuploadtime[1] = System.currentTimeMillis();
					} else { System.out.println("Thingspeak Light Agent update failure.");}
				}
				// update presenceagent channel
				PAuploadtime[0] = System.currentTimeMillis();
				if( ((PAuploadtime[0] - PAuploadtime[1]) >= 15000)&& PAuploadflag) {
					int PAup_res = PAchannel.update(PAentry); // this command might throw exception
					if(PAup_res>0) {
						PAuploadflag = false;
						PAuploadtime[1] = System.currentTimeMillis();
					} else { System.out.println("Thingspeak Presence Agent update failure.");}
				}
			} catch (ThingSpeakException | UnirestException e) {
				String timeStamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
				System.out.println(timeStamp + "***** DB AGENT ***** ThingsPeakException msg: "+ e.getMessage());
				/*System.out.println("Caught exception ");
				System.err.println("e: ");
				System.err.println(e);
				System.err.println("Message:");
				System.err.println(e.getMessage());
				System.err.println("LocalizedMessage");
				System.err.println(e.getLocalizedMessage());
				System.err.println("Cause");
				System.err.println(e.getCause());*/
			}
			/*
			looptime = System.currentTimeMillis();
			if((looptime - lastlooptime) >= 15000) {
				
				int response = 0;
				counter++;
				System.out.println("");
				System.out.println("***** DB AGENT ***** ");
				System.out.println("DataBaseAgent " + getLocalName() + ": " + getBehaviourName());
				System.out.println("Sending data to ThingSpeak ");
				try {
					String apiWriteKey = "F7GUK8F8ZHFN1597";
					Channel channel = new Channel(161815, apiWriteKey);
					Entry entry = new Entry();
					entry.setField(1, String.valueOf(counter));
					response = channel.update(entry);
					System.out.println("Data to ThingSpeak sent.");
					if(response>0) {
						System.err.println("SUCCESFULL, response: "+String.valueOf(response)); 
						lastlooptime = System.currentTimeMillis();
					}
				} catch (ThingSpeakException | UnirestException e) {
					System.out.println("Caught exception ");
					System.err.println("e: ");
					System.err.println(e);
					System.err.println("\nMessage:");
					System.err.println(e.getMessage());
					System.err.println("\nLocalizedMessage");
					System.err.println(e.getLocalizedMessage());
					System.err.println("\nCause");
					System.err.println(e.getCause());
				}
				System.out.println("");
			} */
		}
	}
	
	/******************************************/
	// This behaviour is this agent's social habilities
	private class SocialBehaviour extends CyclicBehaviour {
		public SocialBehaviour(Agent a) {
			super(a);
			templateLA = templateLA.MatchConversationId("DB Light notification");
			templatePA = templatePA.MatchConversationId("DB Presence notification");
			/*			
			msg.setContent(db_message);
			msg.setReplyWith("DBupdate_"+System.currentTimeMillis());
			myAgent.send(msg); 
			templateDB = MessageTemplate.and(	MessageTemplate.MatchConversationId(msg.getConversationId),
												MessageTemplate.MatchInReplyTo(msg.getReplyWith()));
			*/
		}
		public void action() {
			String inputLine;
			
			ACLMessage  msgLA = myAgent.receive(templateLA);
			if(msgLA != null) { 
				inputLine = msgLA.getContent();
				LAentry = new Entry();
				//System.out.println("");
				String timeStamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
				//System.out.println(timeStamp + "***** DB AGENT *****");
				//System.out.println("Message from Light Agent: " + inputLine);
				//System.out.println("Sent by " + (msgLA.getSender()).getLocalName());
				
				int indexbegin = 0; // so that at the first iteration indexbegin+1=1
				int indexend = inputLine.indexOf("<>",indexbegin+1);
				String subLine;

				while(indexend != -1) { // breaking in substrings because it will come all in one line only
					subLine = inputLine.substring(indexbegin,indexend);
					indexbegin = indexend;
					indexend = inputLine.indexOf("<>",indexbegin+1);
					
					if(subLine.indexOf("UC")  != -1) { 
						DBestadoUC[0] = decodeLine(subLine);
						LAentry.setField(LAfieldUC, String.valueOf(DBestadoUC[0]));
						LAuploadflag = true;
					}
					if(subLine.indexOf("LS")  != -1) { 
						DBestadoLS[0]  = decodeLine(subLine); 
						LAentry.setField(LAfieldLS, String.valueOf(DBestadoLS[0]));
						LAuploadflag = true;
					}
					if(subLine.indexOf("UE1") != -1) { 
						DBestadoUE1[0] = decodeLine(subLine);
						LAentry.setField(LAfieldUE1, String.valueOf(DBestadoUE1[0]));
						LAuploadflag = true;
					}
					if(subLine.indexOf("UE2") != -1) { 
						DBestadoUE2[0] = decodeLine(subLine);
						LAentry.setField(LAfieldUE2, String.valueOf(DBestadoUE2[0]));
						LAuploadflag = true;
					}
					if(subLine.indexOf("LB")  != -1) { 
						DBestadoLB[0] = decodeLine(subLine);
						LAentry.setField(LAfieldLB, String.valueOf(DBestadoLB[0]));
						LAuploadflag = true;
					}
					if(subLine.indexOf("AT1") != -1) { 
						DBestadoAT1[0] = decodeLine(subLine);
						LAentry.setField(LAfieldAT1, String.valueOf(DBestadoAT1[0]));
						LAuploadflag = true;
					}
					if(subLine.indexOf("AT2") != -1) { 
						DBestadoAT2[0] = decodeLine(subLine);
						LAentry.setField(LAfieldAT2, String.valueOf(DBestadoAT2[0]));
						LAuploadflag = true;
					}
					if(subLine.indexOf("TL")  != -1) { 
						DBestadoTL[0] = decodeLine(subLine);
						LAentry.setField(LAfieldTL, String.valueOf(DBestadoTL[0]));
						LAuploadflag = true;
					}
				} 
			}
			ACLMessage  msgPA = myAgent.receive(templatePA);
			if(msgPA != null) { 
				inputLine = msgPA.getContent();
				PAentry = new Entry();
				//System.out.println("");
				String timeStamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
				//System.out.println(timeStamp + " ***** DB AGENT *****");
				//System.out.println("Message from Presence Agent: " + inputLine);
				//System.out.println("Sent by " + (msgPA.getSender()).getLocalName());
				
				int indexbegin = 0; // so that at the first iteration indexbegin+1=1
				int indexend = inputLine.indexOf("<>",indexbegin+1);
				String subLine;

				while(indexend != -1) { // breaking in substrings because it will come all in one line only
					subLine = inputLine.substring(indexbegin,indexend);
					indexbegin = indexend;
					indexend = inputLine.indexOf("<>",indexbegin+1);
					
					if(subLine.indexOf("M1") != -1) { 
						DBestadoM1[0] = decodeLine(subLine);
						PAentry.setField(PAfieldM1, String.valueOf(DBestadoM1[0]));
						PAuploadflag = true;
						//System.out.println("M1: " + DBestadoM1[0]);
					}
					if(subLine.indexOf("P1") != -1) { 
						DBestadoP1[0] = decodeLine(subLine);
						PAentry.setField(PAfieldP1, String.valueOf(DBestadoP1[0]));
						PAuploadflag = true;
						//System.out.println("P1: " + DBestadoP1[0]);
					}
					if(subLine.indexOf("M2") != -1) { 
						DBestadoM2[0]  = decodeLine(subLine);
						PAentry.setField(PAfieldM2, String.valueOf(DBestadoM2[0]));
						PAuploadflag = true;
						//System.out.println("M2: " + DBestadoM2[0]);
					}
					if(subLine.indexOf("P2") != -1) { 
						DBestadoP2[0]  = decodeLine(subLine);
						PAentry.setField(PAfieldP2, String.valueOf(DBestadoP2[0]));
						PAuploadflag = true;
						//System.out.println("P2: " + DBestadoP2[0]);
					}
					if(subLine.indexOf("PR") != -1) { 
						String my_presence = subLine.substring(subLine.indexOf("=")+1);
						if(my_presence.indexOf("Bed") != -1) {
							if(my_presence.indexOf("Hall") != -1) 
								DBestadoPres[0] = 3; 
							else 
								DBestadoPres[0] = 1;	
						} else if(my_presence.indexOf("Hall") != -1)
							DBestadoPres[0] = 2;
						else if(my_presence.indexOf("IDLE") != -1)
							DBestadoPres[0] = 0;	
						//System.out.println("Pres: " + DBestadoPres[0] + " my_presence: " + my_presence);
						
						PAentry.setField(PAfieldPres, String.valueOf(DBestadoPres[0]));
						PAentry.setField(PAfieldPS, my_presence);
						PAuploadflag = true;
					}
				} 
			}
			/*try {
				// update lightagent channel
				LAuploadtime[0] = System.currentTimeMillis();
				if( ((LAuploadtime[0] - LAuploadtime[1]) >= 15000)&& LAuploadflag) {
					int LAup_res = LAchannel.update(LAentry); // this command might throw exception
					if(LAup_res>0) {
						LAuploadflag = false;
						LAuploadtime[1] = System.currentTimeMillis();
					} else { System.out.println("Thingspeak Light Agent update failure.");}
				}
				// update presenceagent channel
				PAuploadtime[0] = System.currentTimeMillis();
				if( ((PAuploadtime[0] - PAuploadtime[1]) >= 15000)&& PAuploadflag) {
					int PAup_res = PAchannel.update(PAentry); // this command might throw exception
					if(PAup_res>0) {
						PAuploadflag = false;
						PAuploadtime[1] = System.currentTimeMillis();
					} else { System.out.println("Thingspeak Presence Agent update failure.");}
				}
			} catch (ThingSpeakException | UnirestException e) {
				System.out.println("Caught exception ");
				System.err.println("e: ");
				System.err.println(e);
				System.err.println("Message:");
				System.err.println(e.getMessage());
				System.err.println("LocalizedMessage");
				System.err.println(e.getLocalizedMessage());
				System.err.println("Cause");
				System.err.println(e.getCause());
			}
			System.out.println(""); */
			block(); // this method is blocked until the agent this behaviour belongs to receives a message	
		}
		
		private int decodeLine(String input) {
			int result = -1; // initializes as not found
			int equal_index = input.indexOf("=");
			if(input.indexOf("1",equal_index) != -1) {
				result = 1;
			} else if(input.indexOf("0",equal_index) != -1) {
				result = 0;
			}
			return result;
		}
	}
	/******************************************/

	
	/******************************************/
	public void setup() {
		System.out.println("Starting Agent: "+getName());
		
		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();   
		sd.setType("Data management"); 
		sd.setName("DataBaseAgent " + getName());
		sd.setOwnership("smartenvironment");
		dfd.setName(getAID());
		dfd.addServices(sd);
		
		LAuploadtime[0] = 0;
		LAuploadtime[1] = 0;
		PAuploadtime[0] = 0;
		PAuploadtime[1] = 0;
		
		try {
			DFService.register(this,dfd);
			
			DBAgentLoop loop = new DBAgentLoop(this, 500);
			addBehaviour(loop);
			SocialBehaviour agmsn = new SocialBehaviour(this);
			addBehaviour(agmsn);
			
			
			System.out.println("");
			System.out.println("DataBaseAgent " + getLocalName());
			System.out.println("SETUP COMPLETE.");
			System.out.println("");	
			
			
		} catch (FIPAException e) {
			myLogger.log(Logger.SEVERE, "Agent "+getLocalName()+" - Cannot register with DF", e);
			doDelete();
		}
		
	}
	/******************************************/

	
	/******************************************/
	protected void takeDown() {
		try {
			DFService.deregister(this);
		} catch (FIPAException e) {
			e.printStackTrace();
		}
		System.out.println("");
		System.out.println("DataBaseAgent " + getLocalName());
		System.out.println("AGENT "+getName()+" terminating.");
		System.out.println("");
		doDelete();
	}
	
}
