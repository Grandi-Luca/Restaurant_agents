package com.example;

import java.util.Random;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class CustomerAgent extends Agent {
    // List of available waiter agents
    private AID[] waiterAgents;
    private AID receptionAgent;
    private AID waiterAgent;
    private boolean find_waiter = false;

    protected void setup() {
        System.out.println("Customer agent " + getAID().getName() + " is ready.");

        // Get the reception agent AID
        receptionAgent = new AID("reception", AID.ISLOCALNAME);

        addBehaviour(new TickerBehaviour(this, 1000) {
            protected void onTick() {
                if (find_waiter) {
                    removeBehaviour(this);
                    return;
                }
                // System.out.println("Check if there are any waiter agents available");
                // Update the list of waiter agents available
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("waiter-available");
                template.addServices(sd);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    // System.out.println("Found the following waiter agents:");
                    waiterAgents = new AID[result.length];
                    for (int i = 0; i < result.length; ++i) {
                        waiterAgents[i] = result[i].getName();
                        // System.out.println(waiterAgents[i].getName());
                    }
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }
            }
        });

        addBehaviour(new WakerBehaviour(this, new Random().nextInt(1, 8) * 1000) {
            protected void onWake() {
                addBehaviour(new RequestPerformer());
            }
        });

    }

    protected void takeDown() {
        System.out.println("Customer agent " + getAID().getName() + " terminating.");
    }

    private class RequestPerformer extends Behaviour {

        private int step = 0;
        private MessageTemplate mt; // The template to receive replies
        private int repliesCnt = 0; // The counter of replies from reception agents

        public void action() {
            switch (step) {
                case 0:
                    // Ask at the reception agent if there are tables available
                    ACLMessage request = new ACLMessage(ACLMessage.REQUEST);
                    request.addReceiver(receptionAgent);
                    request.setContent("there are tables available?");
                    request.setConversationId("table-request");
                    request.setReplyWith("request" + System.currentTimeMillis()); // Unique value
                    myAgent.send(request);
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(
                            MessageTemplate.MatchConversationId("table-request"),
                            MessageTemplate.MatchInReplyTo(request.getReplyWith()));
                    step = 1;
                    System.out.println("Customer " + getAID().getLocalName() + " is waiting for a table");
                    break;

                case 1:
                    // Manage the response from the reception agent
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.CONFIRM ||
                                reply.getPerformative() == ACLMessage.INFORM) {
                            // Table available
                            System.out.println("Table available");
                            step = 2;
                        } else if (reply.getPerformative() == ACLMessage.FAILURE) {
                            step = 9;
                        } else {
                            if (repliesCnt == 0) {
                                // Table not available
                                System.out.println("Table not available, I'll wait a bit");
                                // change the message template to wait for a table to be available
                                mt = MessageTemplate.MatchConversationId("table-available");
                                repliesCnt++;

                            } else {
                                // No table available
                                System.out.println("No table available, I'll leave");

                                // Tell to the Reception agent that I'm gonna leave
                                ACLMessage leave = new ACLMessage(ACLMessage.INFORM);
                                leave.addReceiver(receptionAgent);
                                leave.setContent("I'm leaving");
                                leave.setConversationId("customer-leaving");
                                myAgent.send(leave);
                                step = 9;
                            }
                        }
                    } else {
                        block();
                    }
                    break;

                case 2:
                    // Take a seat, check the menu
                    System.out.println("Customer " + getAID().getLocalName() + " takes a seat and check the menu");

                    // waiting time to simulate the customer reading the menu
                    doWait(10000);
                    step = 3;
                    System.out.println(
                            "Customer " + getAID().getLocalName() + " is ready to order, try to call a waiter");

                    break;

                case 3:
                    if (waiterAgents == null || waiterAgents.length == 0) {
                        System.out.println("There are no waiter agents available");
                        break;
                    }
                    // Send the request to one available waiter
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    cfp.addReceiver(waiterAgents[new Random().nextInt(waiterAgents.length)]);
                    cfp.setContent("I'm ready to order");
                    cfp.setConversationId("customer-ready-to-order");
                    cfp.setReplyWith("order " + System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("customer-ready-to-order"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 4;
                    break;

                case 4:
                    // Wait for the response from the waiter
                    ACLMessage replyWaiter = myAgent.receive(mt);
                    if (replyWaiter != null) {
                        // Reply received
                        if (replyWaiter.getPerformative() == ACLMessage.PROPOSE) {
                            // Waiter available
                            waiterAgent = replyWaiter.getSender();
                            step = 5;
                            find_waiter = true;
                        } else {
                            // Waiter was busy
                            step = 3;
                        }
                    } else {
                        block();
                    }
                    break;

                case 5:
                    System.out.println("Customer " + getAID().getName() + " is ordering");
                    // Send the order to the waiter
                    ACLMessage order = new ACLMessage(ACLMessage.INFORM);
                    order.addReceiver(waiterAgent);
                    order.setContent("pizza");
                    order.setConversationId("order-request");
                    order.setReplyWith("order " + getAID().getName()); // Unique value
                    myAgent.send(order);

                    mt = MessageTemplate.and(
                            MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                            MessageTemplate.and(
                                    MessageTemplate.MatchConversationId("order-ready"),
                                    MessageTemplate.MatchInReplyTo(order.getReplyWith())));
                    step = 6;
                    break;

                case 6:
                    // Wait for the order to be served and eat
                    ACLMessage replyOrder = myAgent.receive(mt);
                    if (replyOrder != null) {
                        // Order served
                        System.out.println("Customer " + getAID().getLocalName() + " is eating");
                        // waiting time to simulate the customer eating
                        doWait(10000);
                        System.out.println("Customer " + getAID().getLocalName() + " finished eating");
                        step = 7;

                    } else {
                        block();
                    }

                    break;

                case 7:
                    // Ask for the bill
                    System.out.println("Customer " + getAID().getLocalName() + " asks for the bill");
                    // Send the request to the reception
                    ACLMessage bill = new ACLMessage(ACLMessage.REQUEST);
                    bill.addReceiver(receptionAgent);
                    bill.setContent("I'm ready to pay");
                    bill.setConversationId("bill-request");
                    bill.setReplyWith("bill " + System.currentTimeMillis()); // Unique value
                    myAgent.send(bill);
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(
                            MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                            MessageTemplate.and(
                                    MessageTemplate.MatchConversationId("bill-request"),
                                    MessageTemplate.MatchInReplyTo(bill.getReplyWith())));
                    step = 8;
                    break;

                case 8:
                    // Wait for the bill and pay it
                    ACLMessage replyBill = myAgent.receive(mt);
                    if (replyBill != null) {
                        // Bill received
                        System.out.println("Bill received");
                        System.out.println("Customer " + getAID().getLocalName() + " pays the bill");
                        step = 9;

                    } else {
                        block();
                    }
                    break;

                default:
                    break;
            }

        }

        public boolean done() {
            if (step == 9) {
                System.out.println("Customer " + getAID().getLocalName() + " is leaving");
            }
            return step == 9;
        }

    }

}