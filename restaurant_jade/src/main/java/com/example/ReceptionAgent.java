package com.example;

import java.util.Queue;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class ReceptionAgent extends Agent {
    private int availableTables = 3;
    private Queue<AID> waitingCustomers;

    protected void setup() {
        System.out.println("Reception agent " + getAID().getName() + " is ready.");

        // Add the behaviour serving incoming requests for table
        addBehaviour(new TableBehaviour());

        // Add the behaviour serving incoming requests for bill
        addBehaviour(new BillBehaviour());

        // Add the behaviour serving incoming requests for waiting customer leaving
        addBehaviour(new WaitingCustomerLeaving());
    }

    private class TableBehaviour extends CyclicBehaviour {
        public void action() {
            // Receive the message from the customer agent
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.REQUEST);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // Process the message
                ACLMessage reply = msg.createReply();

                // Check if there is any table available
                if (availableTables > 0) {
                    reply.setPerformative(ACLMessage.CONFIRM);
                    availableTables--;
                } else {
                    // Add the customer agent to the waiting list
                    waitingCustomers.add(msg.getSender());
                    reply.setPerformative(ACLMessage.REFUSE);
                }

                myAgent.send(reply);
            } else {
                block();
            }

        }
    }

    private class BillBehaviour extends CyclicBehaviour {
        public void action() {
            // Receive the message from the customer agent
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.REQUEST),
                    MessageTemplate.MatchConversationId("bill-request"));
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // Process the message
                ACLMessage reply = msg.createReply();

                // Send the message to the customer agent
                reply.setPerformative(ACLMessage.INFORM);
                reply.setContent("Price: 5$");
                myAgent.send(reply);

                // Check if there is any customer in the waiting list
                if (!waitingCustomers.isEmpty()) {
                    // Call the first customer in the waiting list
                    ACLMessage callMsg = new ACLMessage(ACLMessage.INFORM);
                    callMsg.addReceiver(waitingCustomers.poll());
                    callMsg.setConversationId("table-available");
                    myAgent.send(callMsg);
                } else {
                    availableTables++;
                }
            } else {
                block();
            }

        }
    }

    private class WaitingCustomerLeaving extends CyclicBehaviour {
        public void action() {
            // Receive the message from the customer agent
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchConversationId("customer-leaving"));
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // Remove the customer agent from the waiting list
                waitingCustomers.remove(msg.getSender());
            } else {
                block();
            }

        }
    }

}
