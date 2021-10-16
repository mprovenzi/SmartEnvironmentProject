package smartenvironment.PresenceAgent;

// necessary packages
import jade.core.*;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.domain.FIPANames;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.proto.SubscriptionResponder;
import java.util.Calendar;
import java.text.SimpleDateFormat;
import jade.util.Logger;
import smartenvironment.devices._interface.ESP8266;
import java.lang.String;
import java.util.*;
import jade.lang.acl.MessageTemplate;

public class PresenceAgent extends Agent {
	private Logger myLogger = Logger.getMyLogger(getClass().getName());
	private final static int[] myESP1slist = {2,3};
	private final static int[] myESP2slist = {2,3};
	private int[] estadoM1 = new int[2]; // index 0 = current state, index 1 = previous state
	private int[] estadoP1 = new int[2];
	private int[] estadoM2 = new int[2];
	private int[] estadoP2 = new int[2];
	private final static String pwd = "xxx";
	private String[] colleagues = new String[10];
	private String[] colleagues_info = new String[50];
	private String my_presence = "";
	private boolean has_subscriber = false;
	private PresenceAgentLoop loop;
	private SubscriptionBehaviourResp subscription_behaviour;
	private SocialBehaviour agmsn;
	private AID db_colleague = null;
	private MessageTemplate templateDB;
	private MessageTemplate templateESP1;
	private MessageTemplate templateESP2;
	

	/******************************************/
	// This behaviour is this agent's main
	private class PresenceAgentLoop extends TickerBehaviour {
		// Initialize here this behaviour's variables
		protected PresenceAgentLoop(Agent a, long period) {
			super(a, period); // this MUST be this constructor's first line
		}

		protected void onTick() {
			this.updateDB();
		}
		
		private void updateDB() {		
			//System.out.println("***** PRESENCE AGENT ***** ");
			//System.out.println("updateDB");
			String db_message = "";
			if(estadoM1[0] != estadoM1[1]) db_message += "<>M1="+String.valueOf(estadoM1[0]); //System.out.println("MUDOU: M1[0] = "+ String.valueOf(estadoM1[0])+"; M1[1] = "+String.valueOf(estadoM1[1]));}
			if(estadoP1[0] != estadoP1[1]) db_message += "<>P1="+String.valueOf(estadoP1[0]); //System.out.println("MUDOU: P1[0] = "+ String.valueOf(estadoP1[0])+"; P1[1] = "+String.valueOf(estadoP1[1]));}
			if(estadoM2[0] != estadoM2[1]) db_message += "<>M2="+String.valueOf(estadoM2[0]); //System.out.println("MUDOU: M2[0] = "+ String.valueOf(estadoM2[0])+"; M2[1] = "+String.valueOf(estadoM2[1]));}
			if(estadoP2[0] != estadoP2[1]) db_message += "<>P2="+String.valueOf(estadoP2[0]); //System.out.println("MUDOU: P2[0] = "+ String.valueOf(estadoP2[0])+"; P2[1] = "+String.valueOf(estadoP2[1]));}
			if( (estadoP1[0]!= estadoP1[1]) || 
				(estadoM1[0]!= estadoM1[1]) || 
				(estadoP2[0]!= estadoP2[1]) || 
				(estadoM2[0]!= estadoM2[1]) 	 ) 
						db_message += "<> PR=" + my_presence; //System.out.println("MUDOU: my_presence: " + my_presence);}
			
			db_message += "<>";
			//System.out.println("db_message is: "+db_message);
			ACLMessage  msg = new ACLMessage(ACLMessage.INFORM);
			msg.addReceiver(db_colleague);
			msg.setConversationId("DB Presence notification");
			msg.setContent(db_message);
			//msg.setReplyWith("DBupdate_"+System.currentTimeMillis());
			if(!db_message.equals("<>")) // checks that there's new data to send
				myAgent.send(msg); 
			templateDB = MessageTemplate.MatchConversationId("DB Presence notification");
			/*templateDB = MessageTemplate.and(	MessageTemplate.MatchConversationId("DB Presence notification"),
												MessageTemplate.MatchInReplyTo(msg.getReplyWith()));*/
		}
	}
	
	/******************************************/
	// This method updates this agent's sensors values
	private void ReadmySensors(boolean esp1flag, boolean esp2flag, String content) {
		int M1, M2, P1, P2; {// initializing intermediate variables
		M1 = -1;
		M2 = -1;
		P1 = -1;
		P2 = -1; }

		String timeStamp = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS").format(Calendar.getInstance().getTime());
		
		if(esp1flag) {
			System.out.println(timeStamp + " ***** PRESENCE AGENT ***** ReadmySensors MSG rcv ESP1");
			String M1P1   = content;
			//System.out.print("Readings: ESP1 = '");
			if(M1P1   != null) {
				//System.out.println(M1P1+"' ");
				int indexbegin = M1P1.indexOf("."); // so that at the first iteration indexbegin+1=1
				int indexend = M1P1.indexOf(".",indexbegin+1);
				if( (indexbegin>-1) && (indexend>-1) ) {
					String subLine;
					subLine = M1P1.substring(indexbegin+1,indexend);
					M1  = Integer.parseInt(subLine);
					indexbegin = indexend;
					indexend = M1P1.indexOf(".",indexbegin+1);
					subLine = M1P1.substring(indexbegin+1,indexend);
					P1  = Integer.parseInt(subLine);
				}
			} else { System.out.print("Message was NULL"); } //System.out.println("***** ERROR ESP! Movement #1 or Presence #1 Returned null. ");}
		} 
		if(esp2flag) {
			System.out.println(timeStamp + " ***** PRESENCE AGENT ***** ReadmySensors MSG rcv ESP2");
			String M2P2   = content;
			//System.out.print("Readings: ESP2 = '");
			if(M2P2   != null) {
				//System.out.println(M2P2+"' ");
				int indexbegin = M2P2.indexOf("."); // so that at the first iteration indexbegin+1=1
				int indexend = M2P2.indexOf(".",indexbegin+1);
				if( (indexbegin>-1) && (indexend>-1) ) {
					String subLine;
					subLine = M2P2.substring(indexbegin+1,indexend);
					M2  = Integer.parseInt(subLine);
					indexbegin = indexend;
					indexend = M2P2.indexOf(".",indexbegin+1);
					subLine = M2P2.substring(indexbegin+1,indexend);
					P2  = Integer.parseInt(subLine);
				}
			} else { System.out.print("Message was NULL"); } //System.out.println("***** ERROR ESP! Movement #2 or Presence #2 Returned null. ");}
		} 
		// update previous's readings and update currents's readings
		if(M1 != -1) {
			estadoM1[1] = estadoM1[0];
			estadoM1[0] = M1;
		}
		if(P1 != -1) {
			estadoP1[1] = estadoP1[0];
			estadoP1[0] = P1;
		}
		if(M2 != -1) {
			estadoM2[1] = estadoM2[0];
			estadoM2[0] = M2; 
		}
		if(P2 != -1) {
			estadoP2[1] = estadoP2[0];
			estadoP2[0] = P2;
		}
	
		int presence_numeric = estadoP2[0]<<6 + estadoM2[0]<<4 + estadoP1[0]<<2 + estadoM1[0]<<0;

		String presence = "";
		if(estadoP1[0]==1) presence += "Hall ";
		if(estadoM1[0]==1) presence += "Moving@Hall ";
		if(estadoP2[0]==1) presence += "Bed ";
		if(estadoM2[0]==1) presence += "Moving@Bed ";
		if( (estadoP1[0]==0) && 
			(estadoM1[0]==0) && 
			(estadoP2[0]==0) && 
			(estadoM2[0]==0) 	 ) 
				presence = "IDLE";

		//System.out.println("");
		//System.out.println("Presence is: "+ presence);
		//System.out.println("Presence calculated is: "+ presence_numeric);
		
		// updates the subscriber with new info asap
		if(!my_presence.equals(presence)) {
			//System.out.println("Presence changed from ~" + my_presence + "~ to ~" + presence + "~.");
			my_presence = presence;
			if(has_subscriber) {
				//System.out.println("Sending the inform to the subscribers.");
				ACLMessage  subsc_msg = new ACLMessage(ACLMessage.INFORM);
				subsc_msg.setContent(my_presence);
				subscription_behaviour.subscription_notify(subsc_msg);
			}
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
			ACLMessage  msgDB = myAgent.receive(templateDB);
			if(msgDB != null) {
				/*System.out.println("");
				System.out.println("***** PRESENCE AGENT ***** ");
				System.out.println("Message from DB Agent: ");
				if(msgDB.getPerformative()== ACLMessage.CONFIRM) {
					System.out.println("CONFIRM");
				}
				else if(msgDB.getPerformative()== ACLMessage.FAILURE) {
					System.out.println("FAILURE");
				}
				System.out.println("");*/
			}
			
			block(); // this method is blocked until the agent this behaviour belongs to receives a message
		}
	}

	/******************************************/
	// This behaviour is this agent's subscription
	private class SubscriptionBehaviourResp extends SubscriptionResponder {
		private SubscriptionBehaviourResp(Agent a) {
			super(a, MessageTemplate.and(
						 MessageTemplate.and(
						 	 MessageTemplate.or(MessageTemplate.MatchPerformative(ACLMessage.SUBSCRIBE),
						 						MessageTemplate.MatchPerformative(ACLMessage.CANCEL)),
						 	 MessageTemplate.MatchProtocol(FIPANames.InteractionProtocol.FIPA_SUBSCRIBE)	), 
						 MessageTemplate.MatchConversationId("Subscription")									)	);
			has_subscriber = true;
		}
	
		protected ACLMessage handleSubscription(ACLMessage subscription_req) { // handle a subscription request
			/*System.out.println("");
			System.out.println("PresenceAgent " + getLocalName());
			System.out.println("SUBSCRIPTION REQUESTED.");*/
			String content = subscription_req.getContent();
			ACLMessage reply = new ACLMessage();
			reply.addReceiver(subscription_req.getSender());
			if (content != null) {
				if(content.indexOf("Presence estimation") != -1) {
					createSubscription(subscription_req);
					//System.out.println("SUBSCRIPTION AGREED.");
					reply.setPerformative(reply.AGREE);
				} else {
					reply.setPerformative(reply.REFUSE);
				}	// if successful, should answer (return) with AGREE;	otherwise with REFUSE or NOT_UNDERSTOOD
			} else {
				 reply.setPerformative(reply.NOT_UNDERSTOOD);
			}
			//System.out.println("");		
			return reply;
		}
	
		protected void subscription_notify(ACLMessage inform) {	
			Vector subs = getSubscriptions();
			for(int i=0; i<subs.size(); i++)
				((SubscriptionResponder.Subscription)subs.elementAt(i)).notify(inform);
		}
	}

	/******************************************/
	public void setup() {
		System.out.println("Starting Agent: "+getName());


		// configuring Yellow Pages settings
		DFAgentDescription dfd = new DFAgentDescription();
		ServiceDescription sd = new ServiceDescription();   
		sd.setType("Presence estimation"); 
		sd.setName(getName());
		sd.setOwnership("smartenvironment");
		dfd.setName(getAID());
		dfd.addServices(sd);

		estadoM1[1] = 0;
		estadoP1[1] = 0;
		estadoM2[1] = 0;
		estadoP2[1] = 0;
		estadoM1[0] = 0; 
		estadoP1[0] = 0;
		estadoM2[0] = 0; 
		estadoP2[0] = 0;
		
		try {
			DFService.register(this,dfd);

			subscription_behaviour = new SubscriptionBehaviourResp(this);
			addBehaviour(subscription_behaviour);	// ready to receive subscription requests right from start up
			
			addBehaviour(new WakerBehaviour(this, 500) { // wait 0.5 seconds for handling startup time before start working
				protected void handleElapsedTimeout() {
					try {
						templateESP1 = templateESP1.MatchConversationId("ESP1");
						templateESP2 = templateESP2.MatchConversationId("ESP2");
						loop = new PresenceAgentLoop(myAgent, 300); // 3000 -> worked, but slow on anticipation
						agmsn = new SocialBehaviour(myAgent);		
						
						// SEARCH FOR DATABASE MANAGEMENT AGENT
						ServiceDescription sd = new ServiceDescription();   
						sd.setType("Data management");
						DFAgentDescription template2 = new DFAgentDescription();
						template2.addServices(sd);
						DFAgentDescription [] result2 = DFService.search(myAgent, template2);
						db_colleague = result2[0].getName();
						//System.out.println("");
						//System.out.println("***** PRESENCE AGENT ***** ");
						//System.out.println("Found DB Agent: "+db_colleague.getName());
						//System.out.println("");
						
						addBehaviour(loop);
						addBehaviour(agmsn);
					} catch (FIPAException e) {
						myLogger.log(Logger.SEVERE, "Agent "+getLocalName()+" - Cannot find from DF", e);
					}
				}
			});

			System.out.println("");
			System.out.println("PresenceAgent " + getLocalName());
			System.out.println("SETUP COMPLETE.");
			System.out.println("");			

		} catch (FIPAException e) {
			myLogger.log(Logger.SEVERE, "Agent "+getLocalName()+" - Cannot register with DF", e);
			doDelete();
		}
	}

	/******************************************/
	protected void takeDown() {
		System.out.println("");
		System.out.println("PresenceAgent " + getLocalName());
		System.out.println("AGENT "+getName()+" terminating.");
		System.out.println("");
		doDelete();
	}
}
