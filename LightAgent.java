package smartenvironment.LightAgent;

// necessary packages
import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.proto.SubscriptionInitiator;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import jade.util.Logger;
import smartenvironment.devices._interface.ESP8266;
//import smartenvironment.devices._interface.ESP8266.Testando;
import java.lang.String;
import java.util.*;
import jade.lang.acl.MessageTemplate;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;
import java.io.IOException;

public class LightAgent extends Agent {
	private Logger myLogger = Logger.getMyLogger(getClass().getName());
//	ESP8266.Testando teste1 = new ESP8266.Testando();
	
	private final static int[] myESP1slist = {0, 1, 5};
	private final static int[] myESP2slist = {0, 1, 4, 5};
	private final static int ESP1id = 1;
	private final static int ESP2id = 2;
	private final static int LIGHT_BULB = 1;
	private final static int TABLE_LAMP = 1;
	private int[] estadoUE1 = new int[2];
	private int[] estadoUE2 = new int[2];
	private int[] estadoLS = new int[2];
	private int[] estadoLB = new int[2];
	private int[] estadoTL = new int[2];
	private int[] estadoAT1 = new int[2];
	private int[] estadoAT2 = new int[2];
	private int[] usr_chnge = new int[2];
	private boolean flag_blockautomation = false;

	private String[] colleagues_info = new String[50];
	private AID[] colleagues;
	private int my_presence = 0;
	private SubscriptionBehaviourInit subscription;
	private LightAgentLoop loop;
	private SocialBehaviour agmsn;
	private AID db_colleague = null;
	private MessageTemplate templateDB;
	private MessageTemplate templatePres;
	private MessageTemplate templateESP1;
	private MessageTemplate templateESP2;
	
	// inference variables
	private int RBwkdayT0;
	private int RBwkdayT1;
	private int RBhourT0;
	private int RBhourT1;
	private int RBlocationT0;
	private int RBlocationT1;
	private int RBactivityT0;
	private int RBactivityT1;
	private final static List<String> RBactivityList = Arrays.asList("Away", "Sleeping", "Wandering", "Reading", "Diverse");
	private final static List<String> RBwkdayList 	 = Arrays.asList("Weekday", "Weekend");
	private final static List<String> RBlocationList = Arrays.asList("Idle", "Bed", "Hall", "Both");
	private final static List<String> RBhourList 	 = Arrays.asList("0to3", "3to6", "6to9", "9to12", "12to15", "15to18", "18to21", "21to24");
		
	/******************************************/
	// This behaviour is this agent's main
	private class LightAgentLoop extends TickerBehaviour {
		protected LightAgentLoop(Agent a, long period) {
			super(a, period); // this MUST be this constructor's first line
		}

		protected void onTick() {
			// if the user interacts with the system, block automation
			if( (estadoUE1[0] != estadoUE1[1]) || (estadoUE2[0] != estadoUE2[1]) || (usr_chnge[0]==1) ) flag_blockautomation = true; //System.out.println("BLOCKING AUTOMATION");}
			String timeStamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
			System.out.println(timeStamp + " ***** LIGHT AGENT ***** LightAgentLoop "); // - flag BLOCKAUTOMATION = " + String.valueOf(flag_blockautomation));
			int act_est 	= RB_Curr_Act_estimate();
			estadoAT1[1] = estadoAT1[0]; // ensuring that updates the previous one before automating, but also if automation'll not run
			estadoAT2[1] = estadoAT2[0];
			if(!flag_blockautomation) {
				int pred_int 	= RB_Prediction_Interface();
				if((act_est!=-1) && (pred_int==1)) {
					this.Automate();
				}
				//System.out.println("NOW " + RBactivityT0 + " LATER " + RBactivityT1 );
			}
			
			this.updateDB();
		}
		
		/******************************************/
		// This method updates the devices's actuators in accordance to the predicted activity
		private void Automate() {
			boolean LBupdate = false;
			boolean TLupdate = false;
			switch(RBactivityT1) { 
				/* This routine compares the current state of the actuators and of the light sensor, when 
				 * necessary, to the desired depending on the next activity predicted, and if needed updates 
				 * it to the correct value. It also updates the previous state of the actuators. */
				case 0: // "Away"
					if(estadoLB[0] != 0) 	 { estadoAT1[0] = 0; LBupdate = true; } // If LB is ON, this routine switches it OFF (desired when Away)
					if(estadoTL[0] != 0) 	 { estadoAT2[0] = 0; TLupdate = true; }
					break;
				case 1: // "Sleeping"
					if(estadoLS[0] == 1) {
						if(estadoLB[0] != 0) { estadoAT1[0] = 0; LBupdate = true; }
						if(estadoTL[0] != 0) { estadoAT2[0] = 0; TLupdate = true; }
					}
					break;
				case 2: // "Wandering"
					if(estadoLB[0] != 0) 	 { estadoAT1[0] = 0; LBupdate = true; }
					if(estadoTL[0] != 1) 	 { estadoAT2[0] = 1; TLupdate = true; }
					break;
				case 3: // "Reading"
					if(estadoLS[0] == 0) {
						if(estadoTL[0] != 1) { estadoAT2[0] = 1; TLupdate = true; }
					}
					break;
				case 4: // "Diverse"
					if(estadoLS[0]==0) {
						if(estadoLB[0] != 1) { estadoAT1[0] = 1; LBupdate = true; }
					}
					break;
				default: // Should never come in here, but ends execution if it does. 
					return;
			}
			
			String esp_message = "";
			if(LBupdate) esp_message += "<>LB="+((estadoAT1[0]==1) ? "ON":"OFF");
			if(TLupdate) esp_message += "<>TL="+((estadoAT2[0]==1) ? "ON":"OFF");
			esp_message += "<>";
			
			ACLMessage  msg = new ACLMessage(ACLMessage.REQUEST);
			msg.addReceiver(new AID("esp", AID.ISLOCALNAME));
			msg.setConversationId("ESP Toggle actuators");
			msg.setContent(esp_message);
			if(!esp_message.equals("<>")){ // checks that there's new data to send
				//String timeStamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
				//System.out.print(timeStamp + " ***** LIGHT AGENT ***** Automate");
				myAgent.send(msg);
				MessageTemplate templateESPact = MessageTemplate.MatchConversationId("ESP Toggle actuators");
				long before = System.currentTimeMillis();
				ACLMessage msgESPact = null;
				while(msgESPact==null) {
					msgESPact = myAgent.receive(templateESPact);
					if(msgESPact != null) {
						if(msgESPact.getPerformative()== ACLMessage.CONFIRM) {
							if(LBupdate) { estadoLB[1] = estadoLB[0]; estadoLB[0] = estadoAT1[0]; System.out.print(" LB = " + estadoLB[0]);}
							if(TLupdate) { estadoTL[1] = estadoTL[0]; estadoTL[0] = estadoAT2[0]; System.out.print(" TL = " + estadoTL[0]);}
							System.out.println("");
						}
						else {
							myAgent.send(msg); // if ESP did not confirm, send message again.
							msgESPact = null;
						}
					}
				}
				long after = System.currentTimeMillis();
				long per = after - before;
				System.out.println("**** Actuation period: " + per + "ms");
			}
			
			/*if(LBupdate) {
				boolean LB = ((estadoAT1[0]==1) ? true:false);
				do {
					//estadoLB[0] = ESPToggleActuator(LIGHT_BULB, LB, ESP1id); 
				} while(estadoLB[0] != estadoAT1[0]);
			}
			if(TLupdate) {
				boolean TL = ((estadoAT2[0]==1) ? true:false);
				do {
					//estadoTL[0] = ESPToggleActuator(TABLE_LAMP, TL, ESP2id);
				} while(estadoTL[0] != estadoAT2[0]);
			}*/
		}
		
		private void updateDB() {
			String db_message = "";
			
			if(usr_chnge[0] != usr_chnge[1]) db_message  += "<>UC=" +String.valueOf(usr_chnge[0]);	
			if(estadoUE1[0] != estadoUE1[1]) db_message  += "<>UE1="+String.valueOf(estadoUE1[0]);
			if(estadoUE2[0] != estadoUE2[1]) db_message  += "<>UE2="+String.valueOf(estadoUE2[0]);
			if(estadoAT1[0] != estadoAT1[1]) db_message  += "<>AT1="+String.valueOf(estadoAT1[0]);
			if(estadoAT2[0] != estadoAT2[1]) db_message  += "<>AT2="+String.valueOf(estadoAT2[0]);
			if(estadoLS[0]  != estadoLS[1] ) db_message  += "<>LS=" +String.valueOf(estadoLS[0] );
			if(estadoLB[0]  != estadoLB[1] ) db_message  += "<>LB=" +String.valueOf(estadoLB[0] );
			if(estadoTL[0]  != estadoTL[1] ) db_message  += "<>TL=" +String.valueOf(estadoTL[0] );
			
			db_message += "<>";
			ACLMessage  msg = new ACLMessage(ACLMessage.INFORM);
			msg.addReceiver(db_colleague);
			msg.setConversationId("DB Light notification");
			msg.setContent(db_message);
			msg.setReplyWith("DBupdate_"+System.currentTimeMillis());
			if(!db_message.equals("<>")) // checks that there's new data to send
				myAgent.send(msg);
			templateDB = MessageTemplate.MatchConversationId("DB Light notification");	
			/*templateDB = MessageTemplate.and(	MessageTemplate.MatchConversationId("DB Light notification"),
												MessageTemplate.MatchInReplyTo(msg.getReplyWith()));*/
		}
	}
	
	/******************************************/
	/* deprecated: this if for usage with BayesFusion library, which does not run on RPi/ARM processor) // This method creates this agent's Bayesian Network
	private boolean RB_create() {
		RB1 = new Network();
		RB2 = new Network();
		
		
		
		//BAYESIAN NETWORK 1 JPD VALUES ARRAYS
		double[] RB1pWkdayT0 = {0.7, 0.3}; // p(wkdayT0 = dx) / dx = "Week day", "Weekend" respectively
		double[] RB1pWkdayT1 = {0.7, 0.3,  // p(wkdayT1 = dx | wkdayT0 = "Week day") / dx = "Week day", "Weekend" respectively
								0.5, 0.5}; // p(wkdayT1 = dx | wkdayT0 = "Weekend" ) / dx = "Week day", "Weekend" respectively
		double[] RB1pHourT0 = { 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.2, 0.2 };// p(hourT0 = hx) / hx = 0-3, 3-6, 6-9, 9-12, 12-15, 15-18, 18-21, 21-24 respectively
		double[] RB1pHourT1 = { 0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.2, 0.2,  // p(hourT1 = hx | hourT0 = 0-3)   / hx = 0-3, 3-6, 6-9, 9-12, 12-15, 15-18, 18-21, 21-24 respectively
								0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.2, 0.2,  // p(hourT1 = hx | hourT0 = 3-6)   / hx = 0-3, 3-6, 6-9, 9-12, 12-15, 15-18, 18-21, 21-24 respectively
								0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.2, 0.2,  // p(hourT1 = hx | hourT0 = 6-9)   / hx = 0-3, 3-6, 6-9, 9-12, 12-15, 15-18, 18-21, 21-24 respectively
								0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.2, 0.2,  // p(hourT1 = hx | hourT0 = 9-12)  / hx = 0-3, 3-6, 6-9, 9-12, 12-15, 15-18, 18-21, 21-24 respectively
								0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.2, 0.2,  // p(hourT1 = hx | hourT0 = 12-15) / hx = 0-3, 3-6, 6-9, 9-12, 12-15, 15-18, 18-21, 21-24 respectively
								0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.2, 0.2,  // p(hourT1 = hx | hourT0 = 15-18) / hx = 0-3, 3-6, 6-9, 9-12, 12-15, 15-18, 18-21, 21-24 respectively
								0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.2, 0.2,  // p(hourT1 = hx | hourT0 = 18-21) / hx = 0-3, 3-6, 6-9, 9-12, 12-15, 15-18, 18-21, 21-24 respectively
								0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.2, 0.2 };// p(hourT1 = hx | hourT0 = 21-24) / hx = 0-3, 3-6, 6-9, 9-12, 12-15, 15-18, 18-21, 21-24 respectively
		double[] RB1pLocatT0 = {0.65, 0.25, 0.05, 0.05}; // p(locatT0 = lx) / lx = "Idle", "Bed", "Hall", "Both" respectively
		double[] RB1pLocatT1 = {0.1, 0.4, 0.3, 0.2,   // p(locatT1 = lx | locatT0 = "Idle") / lx = "Idle", "Bed", "Hall", "Both" respectively
								0.5, 0.1, 0.3, 0.1,   // p(locatT1 = lx | locatT0 = "Bed")  / lx = "Idle", "Bed", "Hall", "Both" respectively
								0.5, 0.1, 0.1, 0.3,   // p(locatT1 = lx | locatT0 = "Hall") / lx = "Idle", "Bed", "Hall", "Both" respectively
								0.1, 0.2, 0.6, 0.1};  // p(locatT1 = lx | locatT0 = "Both") / lx = "Idle", "Bed", "Hall", "Both" respectively			
		
		//BAYESIAN NETWORK 2 JPD VALUES ARRAYS
		double[] RB2pActivT0 = {0.2, 0.2, 0.2, 0.2, 0.2}; // p(activT0 = ax) / ax = "Away", "Sleeping", "Wandering", "Reading", "Diverse" respectively
		double[] RB2pActivT1 = {0.2, 0.2, 0.2, 0.2, 0.2,   // p(activT1 = ax | activT0 = "Away"      ) / ax = "Away", "Sleeping", "Wandering", "Reading", "Diverse" respectively
								0.2, 0.2, 0.2, 0.2, 0.2,   // p(activT1 = ax | activT0 = "Sleeping"  ) / ax = "Away", "Sleeping", "Wandering", "Reading", "Diverse" respectively
								0.2, 0.2, 0.2, 0.2, 0.2,   // p(activT1 = ax | activT0 = "Wandering" ) / ax = "Away", "Sleeping", "Wandering", "Reading", "Diverse" respectively
								0.2, 0.2, 0.2, 0.2, 0.2,   // p(activT1 = ax | activT0 = "Reading"   ) / ax = "Away", "Sleeping", "Wandering", "Reading", "Diverse" respectively
								0.2, 0.2, 0.2, 0.2, 0.2 }; // p(activT1 = ax | activT0 = "Diverse"   ) / ax = "Away", "Sleeping", "Wandering", "Reading", "Diverse" respectively
		double[] RB2pWkdayT1 = {0.5, 0.5,   // p(wkdayT1 = dx | activT0 = "Away"      ) / dx = "Week day", "Weekend" respectively
								0.5, 0.5,   // p(wkdayT1 = dx | activT0 = "Sleeping"  ) / dx = "Week day", "Weekend" respectively
								0.5, 0.5,   // p(wkdayT1 = dx | activT0 = "Wandering" ) / dx = "Week day", "Weekend" respectively
								0.5, 0.5,   // p(wkdayT1 = dx | activT0 = "Reading"   ) / dx = "Week day", "Weekend" respectively
								0.5, 0.5 }; // p(wkdayT1 = dx | activT0 = "Diverse"   ) / dx = "Week day", "Weekend" respectively
		double[] RB2pHourT1  = {0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.2, 0.2,  // p(hourT1 = hx | activT0 = "Away"      ) / hx = 0-3, 3-6, 6-9, 9-12, 12-15, 15-18, 18-21, 21-24 respectively
								0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.2, 0.2,  // p(hourT1 = hx | activT0 = "Sleeping"  ) / hx = 0-3, 3-6, 6-9, 9-12, 12-15, 15-18, 18-21, 21-24 respectively
								0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.2, 0.2,  // p(hourT1 = hx | activT0 = "Wandering" ) / hx = 0-3, 3-6, 6-9, 9-12, 12-15, 15-18, 18-21, 21-24 respectively
								0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.2, 0.2,  // p(hourT1 = hx | activT0 = "Reading"   ) / hx = 0-3, 3-6, 6-9, 9-12, 12-15, 15-18, 18-21, 21-24 respectively
								0.1, 0.1, 0.1, 0.1, 0.1, 0.1, 0.2, 0.2 };// p(hourT1 = hx | activT0 = "Diverse"   ) / hx = 0-3, 3-6, 6-9, 9-12, 12-15, 15-18, 18-21, 21-24 respectively 
		double[] RB2pLocatT1 = {0.3, 0.3, 0.2, 0.2,   // p(locatT1 = lx | activT0 = "Away"      ) / lx = "Idle", "Bed", "Hall", "Both" respectively
								0.3, 0.3, 0.2, 0.2,   // p(locatT1 = lx | activT0 = "Sleeping"  ) / lx = "Idle", "Bed", "Hall", "Both" respectively
								0.3, 0.3, 0.2, 0.2,   // p(locatT1 = lx | activT0 = "Wandering" ) / lx = "Idle", "Bed", "Hall", "Both" respectively
								0.3, 0.3, 0.2, 0.2,   // p(locatT1 = lx | activT0 = "Reading"   ) / lx = "Idle", "Bed", "Hall", "Both" respectively
								0.3, 0.3, 0.2, 0.2};  // p(locatT1 = lx | activT0 = "Diverse"   ) / lx = "Idle", "Bed", "Hall", "Both" respectively
		
		// BAYESIAN NETWORK 1 CONSTRUCTION
		{ // setting weekday variable related nodes, values and edges
		RB1.addNode(Network.NodeType.Cpt, "wkdayT0");
		RB1.addNode(Network.NodeType.Cpt, "wkdayT1");
		RB1.addOutcome("wkdayT0", "Weekday"); 
		RB1.addOutcome("wkdayT0", "Weekend");
		RB1.deleteOutcome("wkdayT0", 0);
		RB1.deleteOutcome("wkdayT0", 0);
		RB1.addOutcome("wkdayT1", "Weekday"); 
		RB1.addOutcome("wkdayT1", "Weekend");
		RB1.deleteOutcome("wkdayT1", 0);
		RB1.deleteOutcome("wkdayT1", 0);
		RB1.addArc("wkdayT0", "wkdayT1");
		RB1.setNodeDefinition("wkdayT0", RB1pWkdayT0);
		RB1.setNodeDefinition("wkdayT1", RB1pWkdayT1); }
		{ // setting hour variable related nodes, values and edges
		RB1.addNode(Network.NodeType.Cpt, "hourT0");
		RB1.addNode(Network.NodeType.Cpt, "hourT1");
		RB1.addOutcome("hourT0", "0-3"); 
		RB1.addOutcome("hourT0", "3-6");
		RB1.addOutcome("hourT0", "6-9"); 
		RB1.addOutcome("hourT0", "9-12");
		RB1.addOutcome("hourT0", "12-15"); 
		RB1.addOutcome("hourT0", "15-18");
		RB1.addOutcome("hourT0", "18-21"); 
		RB1.addOutcome("hourT0", "21-24");
		RB1.deleteOutcome("hourT0", 0);
		RB1.deleteOutcome("hourT0", 0);
		RB1.addOutcome("hourT1", "0-3"); 
		RB1.addOutcome("hourT1", "3-6");
		RB1.addOutcome("hourT1", "6-9"); 
		RB1.addOutcome("hourT1", "9-12");
		RB1.addOutcome("hourT1", "12-15"); 
		RB1.addOutcome("hourT1", "15-18");
		RB1.addOutcome("hourT1", "18-21"); 
		RB1.addOutcome("hourT1", "21-24");
		RB1.deleteOutcome("hourT1", 0);
		RB1.deleteOutcome("hourT1", 0);
		RB1.addArc("hourT0", "hourT1");
		RB1.setNodeDefinition("hourT0", RB1pHourT0);
		RB1.setNodeDefinition("hourT1", RB1pHourT1); }
		{ // setting location variable related nodes, values and edges
		RB1.addNode(Network.NodeType.Cpt, "locatT0"); // location at the current moment
		RB1.addNode(Network.NodeType.Cpt, "locatT1"); // next location
		RB1.addOutcome("locatT0", "Idle");
		RB1.addOutcome("locatT0", "Bed"); 
		RB1.addOutcome("locatT0", "Hall");
		RB1.addOutcome("locatT0", "Both");
		RB1.deleteOutcome("locatT0", 0);
		RB1.deleteOutcome("locatT0", 0);
		RB1.addOutcome("locatT1", "Idle");
		RB1.addOutcome("locatT1", "Bed"); 
		RB1.addOutcome("locatT1", "Hall");
		RB1.addOutcome("locatT1", "Both");
		RB1.deleteOutcome("locatT1", 0);
		RB1.deleteOutcome("locatT1", 0);
		RB1.addArc("locatT0", "locatT1");
		RB1.setNodeDefinition("locatT0", RB1pLocatT0);
		RB1.setNodeDefinition("locatT1", RB1pLocatT1);}
		
		// BAYESIAN NETWORK 2 CONSTRUCTION
		{ // setting current activity variable related nodes, values and edges
		RB2.addNode(Network.NodeType.Cpt, "activT0");
		RB2.addOutcome("activT0", "Away");
		RB2.addOutcome("activT0", "Sleeping");
		RB2.addOutcome("activT0", "Wandering"); 
		RB2.addOutcome("activT0", "Reading");
		RB2.addOutcome("activT0", "Diverse");
		RB2.deleteOutcome("activT0", 0);
		RB2.deleteOutcome("activT0", 0);
		RB2.setNodeDefinition("activT0", RB2pActivT0);}
		{ // setting next activity variable related nodes, values and edges
		RB2.addNode(Network.NodeType.Cpt, "activT1");
		RB2.addOutcome("activT1", "Away");
		RB2.addOutcome("activT1", "Sleeping");
		RB2.addOutcome("activT1", "Wandering"); 
		RB2.addOutcome("activT1", "Reading");
		RB2.addOutcome("activT1", "Diverse");
		RB2.deleteOutcome("activT1", 0);
		RB2.deleteOutcome("activT1", 0);
		RB2.addArc("activT0", "activT1");
		RB2.setNodeDefinition("activT1", RB2pActivT1);}
		{ // setting weekday variable related nodes, values and edges
		RB2.addNode(Network.NodeType.Cpt, "wkdayT1");
		RB2.addOutcome("wkdayT1", "Weekday"); 
		RB2.addOutcome("wkdayT1", "Weekend");
		RB2.deleteOutcome("wkdayT1", 0);
		RB2.deleteOutcome("wkdayT1", 0);
		RB2.addArc("activT1", "wkdayT1");
		RB2.setNodeDefinition("wkdayT1", RB2pWkdayT1);}
		{ // setting hour variable related nodes, values and edges
		RB2.addNode(Network.NodeType.Cpt, "hourT1");
		RB2.addOutcome("hourT1", "0-3"); 
		RB2.addOutcome("hourT1", "3-6");
		RB2.addOutcome("hourT1", "6-9"); 
		RB2.addOutcome("hourT1", "9-12");
		RB2.addOutcome("hourT1", "12-15"); 
		RB2.addOutcome("hourT1", "15-18");
		RB2.addOutcome("hourT1", "18-21"); 
		RB2.addOutcome("hourT1", "21-24");
		RB2.deleteOutcome("hourT1", 0);
		RB2.deleteOutcome("hourT1", 0);
		RB2.addArc("activT1", "hourT1");
		RB2.setNodeDefinition("hourT1", RB2pHourT1);}
		{ // setting location variable related nodes, values and edges
		RB2.addNode(Network.NodeType.Cpt, "locatT1");
		RB2.addOutcome("locatT1", "Idle");
		RB2.addOutcome("locatT1", "Bed"); 
		RB2.addOutcome("locatT1", "Hall");
		RB2.addOutcome("locatT1", "Both");
		RB2.deleteOutcome("locatT1", 0);
		RB2.deleteOutcome("locatT1", 0);
		RB2.addArc("activT1", "locatT1");
		RB2.setNodeDefinition("locatT1", RB2pLocatT1);}
		
		RB1.updateBeliefs();
		RB2.updateBeliefs();
		RB1.writeFile("BayesianNetwork_1.xdsl");
		RB2.writeFile("BayesianNetwork_2.xdsl");
		
		return true;
	} */
	
	/******************************************/
	// This method interfaces with the python Bayes Network to update predictions
	private int RB_Prediction_Interface() {
		int result = -1;
		String inputLine;
		// inference variables
		String RBwkdayT1_py;
		String RBhourT1_py;
		String RBlocationT1_py;
		String RBactivityT1_py;
		try {
			long before = System.currentTimeMillis();
			
			ProcessBuilder pb = new ProcessBuilder("python", "jade/src/smartenvironment/LightAgent/pythonscriptbayesiannetwork.py", 
														RBwkdayList.get(RBwkdayT0-1), RBhourList.get(RBhourT0-1), 
														RBlocationList.get(RBlocationT0), RBactivityList.get(RBactivityT0));
														// RBwkday and RBhour start their context values at 1, whereas the indexes start at 0
			pb.redirectErrorStream(true);
			Process proc = pb.start();
			

			BufferedReader bn_return = new BufferedReader(new InputStreamReader(proc.getInputStream()));
			long after = System.currentTimeMillis();
			
			try {
				while ((inputLine = bn_return.readLine()) != null) {
					int indexbegin = inputLine.indexOf(".",0); // so that at the first iteration indexbegin+1=1
					int indexend = inputLine.indexOf(".",indexbegin+1);
					if( (indexbegin>-1) && (indexend>-1) ) { // result comes as: .RBactivityT1.RBwkdayT1.RBhourT1.RBlocationT1
						RBactivityT1_py = inputLine.substring(indexbegin+1,indexend);
						indexbegin = indexend;
						indexend = inputLine.indexOf(".",indexbegin+1);
						RBwkdayT1_py = inputLine.substring(indexbegin+1,indexend);
						indexbegin = indexend;
						indexend = inputLine.indexOf(".",indexbegin+1);
						RBhourT1_py = inputLine.substring(indexbegin+1,indexend);
						indexbegin = indexend;
						indexend = inputLine.indexOf(".",indexbegin+1);
						RBlocationT1_py = inputLine.substring(indexbegin+1,indexend);
						
						RBactivityT1 = RBactivityList.indexOf(RBactivityT1_py);
						RBlocationT1 = RBlocationList.indexOf(RBlocationT1_py);
						RBwkdayT1 	 = RBwkdayList.indexOf(RBwkdayT1_py) + 1;			
						RBhourT1 	 = RBhourList.indexOf(RBhourT1_py) 	 + 1;
						// RBwkday and RBhour start their context values at 1, whereas the indexes start at 0
						
						/*// *** OUTPUT
						
						System.out.println("wkdayT0: " + RBwkdayList.get(RBwkdayT0-1) 		+ " wkdayT1: " 	  + RBwkdayList.get(RBwkdayT1-1));
						System.out.println("hourT0:  " + RBhourList.get(RBhourT0-1) 		+ " hourT1: " 	  + RBhourList.get(RBhourT1-1));
						System.out.println("locatT0: " + RBlocationList.get(RBlocationT0) 	+ " locationT1: " + RBlocationList.get(RBlocationT1));
						System.out.println("activT0: " + RBactivityList.get(RBactivityT0) 	+ " activityT1: " + RBactivityList.get(RBactivityT1));*/
						result = 1;	// prediction process was successful
					}
				}		
			} catch(IOException e) {
				String timeStamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
				System.out.println(timeStamp + " ***** LIGHT AGENT ***** "+"IOException caught when reading line from buffered reader: "+e.getMessage());
			} finally {
				try {
					if(bn_return!=null)
						bn_return.close();
				} catch(IOException e) {
					String timeStamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
					System.out.println(timeStamp + " ***** LIGHT AGENT ***** " + "IOException caught when closing buffered reader: "+e.getMessage());
				}
			}
			
			long per = after - before;
			//System.out.println("**** Python Interface period: " + per + "ms");
		} catch(IOException e) {
			String timeStamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
			System.out.println(timeStamp + " ***** LIGHT AGENT ***** " + "**** IOEXCEPTION WHEN RUNNING PYTHON. Message: "+e.getMessage());
			/*System.out.println("Caught exception ");
			System.err.println("e: ");
			System.err.println(e);
			System.err.println("Message:");
			System.err.println(e.getMessage());
			System.err.println("LocalizedMessage");
			System.err.println(e.getLocalizedMessage());
			System.err.println("Cause");
			System.err.println(e.getCause());	*/
		} finally {
			return result;
		}
	}
	
	/******************************************/
	// This method updates this agent's current activity estimation
	private int RB_Curr_Act_estimate() {
		if(RB_Curr_Var_update())
		{
			/*System.out.println("Estimating cur. activity");
			System.out.println("Iluminacao: " + String.valueOf(estadoLS[0])  + 
			                   " Presenca: "  + String.valueOf(RBlocationT0) + 
			                   " Dia: " 	  + String.valueOf(RBwkdayT0)    + 
			                   " Horario: "   + String.valueOf(RBhourT0)	  );*/
			// sleeping:
			if( 		  				  (estadoLS[0]==0) && 
				( (RBlocationT0==0) || (RBlocationT0==1) ) && 
				( 			  ( (RBwkdayT0==1) && ( (RBhourT0==1) || (RBhourT0==2) || (RBhourT0==3) ) ) || 
							  ( (RBwkdayT0==2) && ( (RBhourT0==1) || (RBhourT0==2) || (RBhourT0==3) || (RBhourT0==4) ) ) ) )
				RBactivityT0 = 1;
			// wandering:
			if( ( ( (RBwkdayT0==1)   && ( (RBhourT0==1) || (RBhourT0==2) || (RBhourT0==3) ) ) || 
				  ( (RBwkdayT0==2)   && ( (RBhourT0==1) || (RBhourT0==2) || (RBhourT0==3) || (RBhourT0==4) ) ) ) &&
				( ( (estadoLS[0]==0) && ( (RBlocationT0==2) || (RBlocationT0==3) ) )          || 
				  ( (estadoLS[0]==1) && ( (RBlocationT0==1) || (RBlocationT0==2) || (RBlocationT0==3) ) )      )      )
				RBactivityT0 = 2;
			// reading: 
			if( ( ( (RBwkdayT0==1)   && ( (RBhourT0==4) || (RBhourT0==5) || (RBhourT0==6) || (RBhourT0==7) || (RBhourT0==8) ) ) || 
				  ( (RBwkdayT0==2)   && ( (RBhourT0==5) || (RBhourT0==6) || (RBhourT0==7) || (RBhourT0==8) ) ) 						) &&
				( estadoLS[0]==1  )  &&	( RBlocationT0==1 ) )
				RBactivityT0 = 3;
			// diverse:
			if( ( ( (RBwkdayT0==1)   && ( (RBhourT0==4) || (RBhourT0==5) || (RBhourT0==6) || (RBhourT0==7) || (RBhourT0==8) ) ) || 
				  ( (RBwkdayT0==2)   && ( (RBhourT0==5) || (RBhourT0==6) || (RBhourT0==7) || (RBhourT0==8) ) ) 						) &&
				( ( (estadoLS[0]==1) && ( (RBlocationT0==2) || (RBlocationT0==3) ) )          || 
				  ( (estadoLS[0]==0) && ( (RBlocationT0==1) || (RBlocationT0==2) || (RBlocationT0==3) ) )      )      )
				RBactivityT0 = 4;  
			// away:
			if( (RBlocationT0==0) && ( (estadoLS[0]==1) || ( (estadoLS[0]==0) &&
					( ( (RBwkdayT0==1)   && ( (RBhourT0==4) || (RBhourT0==5) || (RBhourT0==6) || (RBhourT0==7) || (RBhourT0==8) ) ) || 
					  ( (RBwkdayT0==2)   && ( (RBhourT0==5) || (RBhourT0==6) || (RBhourT0==7) || (RBhourT0==8) ) ) ) ) ) )
				RBactivityT0 = 0;  
			
			return RBactivityT0;	  
		}
		else
			return -1;
	}
	
	/******************************************/
	// This method updates this agent's bayesian variables current values
	private boolean RB_Curr_Var_update() {
		switch(Calendar.getInstance().get(Calendar.DAY_OF_WEEK)) {
			case Calendar.MONDAY:
			case Calendar.TUESDAY:
			case Calendar.WEDNESDAY:
			case Calendar.THURSDAY:
			case Calendar.FRIDAY:
				RBwkdayT0 = 1;
				break;
			case Calendar.SUNDAY:
			case Calendar.SATURDAY:
				RBwkdayT0 = 2;
				break;
			default: 
				return false;
		}
		int hr = Calendar.getInstance().get(Calendar.HOUR_OF_DAY);
		int prevhour = RBhourT0; 
		if      ( (hr >=  0) && (hr <  3) )
			RBhourT0 = 1;
		else if ( (hr >=  3) && (hr <  6) )
			RBhourT0 = 2;
		else if ( (hr >=  6) && (hr <  9) )
			RBhourT0 = 3;
		else if ( (hr >=  9) && (hr < 12) )
			RBhourT0 = 4;
		else if ( (hr >= 12) && (hr < 15) )
			RBhourT0 = 5;
		else if ( (hr >= 15) && (hr < 18) )
			RBhourT0 = 6;
		else if ( (hr >= 18) && (hr < 21) )
			RBhourT0 = 7;
		else if ( (hr >= 21) && (hr < 24) )
			RBhourT0 = 8;
		else 
			return false;
		if(RBhourT0 != prevhour) 		flag_blockautomation = false; // unblocks automation if this condition changes
		if(RBlocationT0 != my_presence) flag_blockautomation = false; // unblocks automation if this condition changes
		RBlocationT0 = my_presence;
		return true;
	}
	
	/******************************************/
	// This method updates this agent's sensors values
	private void ReadmySensors(boolean esp1flag, boolean esp2flag, String content) {
	
		/*System.out.println("");
		System.out.println("***** LIGHT AGENT ***** ");
		System.out.println("LightAgent " + getLocalName() + ": " + getBehaviourName());
		System.out.println("Light Switch is: "+((UE1==1) ? "ON":"OFF"));
		System.out.println("UE1 is: " + UE1);
		System.out.println("");*/
		int LB, UE1, UC1, TL, LS, UE2, UC2; { // initializing intermediate variables
		LB  = -1;
		UE1 = -1;
		UC1 = -1;
		TL  = -1;
		LS  = -1;
		UE2 = -1;
		UC2 = -1; }
		
		String timeStamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());

		if(esp1flag) {
			System.out.println(timeStamp + " ***** LIGHT AGENT ***** ReadmySensors MSG rcv ESP1");
			String LBUE1UC1   = content;
			//System.out.print("Readings: ESP1 = '");
			if(LBUE1UC1   != null) {
				//System.out.println(LBUE1UC1+"' ");
				int indexbegin = LBUE1UC1.indexOf("."); // so that at the first iteration indexbegin+1=1
				int indexend = LBUE1UC1.indexOf(".",indexbegin+1);
				if( (indexbegin>-1) && (indexend>-1) ) {
					String subLine;
					subLine = LBUE1UC1.substring(indexbegin+1,indexend);
					LB  = Integer.parseInt(subLine);
					indexbegin = indexend;
					indexend = LBUE1UC1.indexOf(".",indexbegin+1);
					subLine = LBUE1UC1.substring(indexbegin+1,indexend);
					UE1  = Integer.parseInt(subLine);
					indexbegin = indexend;
					indexend = LBUE1UC1.indexOf(".",indexbegin+1);
					subLine = LBUE1UC1.substring(indexbegin+1,indexend);
					UC1 = Integer.parseInt(subLine);
				}
			} else { System.out.println("Message was NULL"); } //System.out.println("***** ERROR ESP! Light bulb, user entry #1 or user change #1. Returned null. ");}
		} 
		if(esp2flag) {
			String TLLSUE2UC2 = content;
			//System.out.print("Readings: ESP2 = '");
			if(TLLSUE2UC2 != null) {
				System.out.println(timeStamp + " ***** LIGHT AGENT ***** ReadmySensors MSG rcv ESP2");
				//System.out.println(TLLSUE2UC2+"' ");
				int indexbegin = TLLSUE2UC2.indexOf("."); // so that at the first iteration indexbegin+1=1
				int indexend = TLLSUE2UC2.indexOf(".",indexbegin+1);
				if( (indexbegin>-1) && (indexend>-1) ) {
					String subLine;
					subLine = TLLSUE2UC2.substring(indexbegin+1,indexend);
					TL  = Integer.parseInt(subLine);
					indexbegin = indexend;
					indexend = TLLSUE2UC2.indexOf(".",indexbegin+1);
					subLine = TLLSUE2UC2.substring(indexbegin+1,indexend);
					LS  = Integer.parseInt(subLine);
					indexbegin = indexend;
					indexend = TLLSUE2UC2.indexOf(".",indexbegin+1);
					subLine = TLLSUE2UC2.substring(indexbegin+1,indexend);
					UE2 = Integer.parseInt(subLine);
					indexbegin = indexend;
					indexend = TLLSUE2UC2.indexOf(".",indexbegin+1);
					subLine = TLLSUE2UC2.substring(indexbegin+1,indexend);
					UC2 = Integer.parseInt(subLine);
				}
			} else { System.out.println("Message was NULL"); } //System.out.println("***** ERROR ESP! Table light, light sensor, user entry #2 or user change #1. Returned null. ");}
		}
		
		if(TL != -1) {
			estadoTL[1]  = estadoTL[0];
			estadoTL[0]  = TL;
		}
		if(LS != -1) {
			estadoLS[1]  = estadoLS[0];
			estadoLS[0]  = LS;
		}
		if(LB != -1) {
			estadoLB[1]  = estadoLB[0];
			estadoLB[0]  = LB;
		}
		if(UE1 != -1) {
			estadoUE1[1] = estadoUE1[0];
			estadoUE1[0] = UE1;
		}
		if(UE2 != -1) {
			estadoUE2[1] = estadoUE2[0];
			estadoUE2[0] = UE2;
		}
		if( (UC1!=-1) || (UC2!=-1) ) {
			usr_chnge[1] = usr_chnge[0];
			if( (UC1==1) || (UC2==1) ) {
				usr_chnge[0] = 1;
			} else {
				usr_chnge[0] = 0;
			}
		}	
	}

	/******************************************/
	// This behaviour is this agent's subscription
	private class SubscriptionBehaviourInit extends SubscriptionInitiator {
		protected SubscriptionBehaviourInit(Agent agent) {
			super(agent, new ACLMessage(ACLMessage.SUBSCRIBE));
		}

		private Vector<ACLMessage> prepareSubscriptions(ACLMessage subscription, AID receiver) {
			subscription.setProtocol(FIPANames.InteractionProtocol.FIPA_SUBSCRIBE);
			subscription.addReceiver(receiver); 
			subscription.setPerformative(subscription.SUBSCRIBE);
			subscription.setContent("Presence estimation"); 
			subscription.setConversationId("Subscription");
			Vector<ACLMessage> v = new Vector<ACLMessage>();
			v.addElement(subscription);
			/*System.out.println("");
			System.out.println("LightAgent " + getLocalName() + ": " + getBehaviourName());
			System.out.println("SUBSCRIPTION REGISTERING");
			System.out.println("");	*/	
			return v;
		}

		protected void handleInform(ACLMessage inform) {
			// handle inform messages from the subscription service that it's subscribed to
			String content = inform.getContent();
			int informed_presence = -1;
			if(content.contains("Bed")) {
				if(content.contains("Hall")) 
					informed_presence = 3; 
				else 
					informed_presence = 1;	
			} else if(content.contains("Hall"))
				informed_presence = 2;
			else if(content.contains("IDLE"))
				informed_presence = 0;
			if(informed_presence != -1)
				if(my_presence != informed_presence) {
					my_presence = informed_presence;
					//System.out.println("MUDOU PRESENCA");
					//flag_blockautomation = false; // if presence information changed, enable automation
				}
			//else System.out.println("LIGHT AGENT: INFORMED PRESENCE NOT UNDERSTOOD");
			/*System.out.println("");
			System.out.println("LightAgent " + getLocalName() + ": " + getBehaviourName());
			System.out.println("CONTENT: "+content);
			System.out.println("");*/
		}
		
		protected void handleAgree(ACLMessage agree) {
			/*System.out.println("");
			System.out.println("LightAgent " + getLocalName() + ": " + getBehaviourName());
			System.out.println("SUBSCRIPTION AGREED!");
			System.out.println("");*/
			// handle an agreement from the subscription service
		}

		protected void handleRefuse(ACLMessage refuse) {
			System.out.println("");
			System.out.println("LightAgent " + getLocalName() + ": " + getBehaviourName());
			System.out.println("SUBSCRIPTION REFUSED!");
			System.out.println("");
			// handle a refusal from the subscription service
		}
	}

	/******************************************/
	// This behaviour is this agent's social habilities
	private class SocialBehaviour extends CyclicBehaviour {

		public SocialBehaviour(Agent a) {
			super(a);
		}

		public void action() {
			ACLMessage msgEsp1 = myAgent.receive(templateESP1);
			if(msgEsp1 != null) {
				ReadmySensors(true, false, msgEsp1.getContent());
			}
			ACLMessage msgEsp2 = myAgent.receive(templateESP2);
			if(msgEsp2 != null) {
				ReadmySensors(false, true, msgEsp2.getContent());
			}
			ACLMessage  msgPres = myAgent.receive(templatePres);
			if(msgPres != null) {
				if(msgPres.getPerformative()== ACLMessage.INFORM) {
					subscription.handleInform(msgPres);
				} else if(msgPres.getPerformative()== ACLMessage.AGREE) {
					subscription.handleAgree(msgPres);
				} else if(msgPres.getPerformative()== ACLMessage.REFUSE) {
					subscription.handleRefuse(msgPres);
				}
				/*System.out.println("");
				System.out.println("LightAgent " + getLocalName() + ": " + getBehaviourName());
				System.out.println("CLASSE SOCIAL BEHAVIOUR"); *//*
				System.out.println("PERFORMATIVE: "+msg.getPerformative());
				System.out.println("CONVERSATION ID: "+msg.getConversationId());
				System.out.println("SENDER: "+msg.getSender());*/
			}
			ACLMessage  msgDB = myAgent.receive(templateDB);
			if(msgDB != null) {
				// do nothing, only take it out of the inbox
			}
			block(); // this method is blocked until the agent this behaviour belongs to receives a message
		}
	}

	/******************************************/

	
	/******************************************/
	public void setup() {
		System.out.println("Starting Agent: "+getName());

		// configuring Yellow Pages settings
		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();   
		sd.setType("Light Control"); 
		sd.setName("LightAgent " + getName());
		sd.setOwnership("smartenvironment");
		dfd.setName(getAID());
		dfd.addServices(sd);
		
		usr_chnge[1] = 0;
		estadoUE1[1] = 0;
		estadoUE2[1] = 0;
		estadoAT1[1] = 0;
		estadoAT2[1] = 0;
		estadoLS[1]  = 0;
		estadoLB[1]  = 0;
		estadoTL[1]  = 0;
		usr_chnge[0] = 0;
		estadoUE1[0] = 0;
		estadoUE2[0] = 0;
		estadoAT1[0] = 0;
		estadoAT2[0] = 0;
		estadoLS[0]  = 0;
		estadoLB[0]  = 0;
		estadoTL[0]  = 0;
		
		RB_Curr_Var_update();
			
		try {
			DFService.register(this,dfd);
			addBehaviour(new WakerBehaviour(this, 500) { // wait 0.5 seconds for handling startup time before start working
				protected void handleElapsedTimeout() {
					try {
						templateESP1 = templateESP1.MatchConversationId("ESP1");
						templateESP2 = templateESP2.MatchConversationId("ESP2");
						loop = new LightAgentLoop(myAgent,300); // 3000 -> worked, but slow on anticipation

						agmsn = new SocialBehaviour(myAgent);
						subscription = new SubscriptionBehaviourInit(myAgent);
						
						ServiceDescription sd = new ServiceDescription();
						// SEARCH FOR PRESENCE ESTIMATION AGENTS
						DFAgentDescription template = new DFAgentDescription();
						sd.setType("Presence estimation");
						template.addServices(sd);
						DFAgentDescription [] result = DFService.search(myAgent, template);
						colleagues = new AID[result.length];
						for (int i=0; i<result.length; ++i) {
							colleagues[i] = result[i].getName();
							Vector<ACLMessage> vecsubs = subscription.prepareSubscriptions(new ACLMessage(), colleagues[i]);
							ACLMessage subs = vecsubs.elementAt(0);
							send(subs);
						}
						templatePres = templatePres.MatchConversationId("Subscription");
						
						// SEARCH FOR DATABASE MANAGEMENT AGENT
						sd.setType("Data management");
						DFAgentDescription template2 = new DFAgentDescription();
						template2.addServices(sd);
						DFAgentDescription [] result2 = DFService.search(myAgent, template2);
						db_colleague = result2[0].getName();
						
						addBehaviour(loop);
						addBehaviour(agmsn);
						addBehaviour(subscription);
					} catch (FIPAException e) {
						myLogger.log(Logger.SEVERE, "Agent "+getLocalName()+" - Cannot find from DF", e);
					}
				}
			});

			System.out.println("");
			System.out.println("LightAgent " + getLocalName());
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
		System.out.println("LightAgent " + getLocalName());
		System.out.println("AGENT "+getName()+" terminating.");
		System.out.println("");
		doDelete();
	}
}
