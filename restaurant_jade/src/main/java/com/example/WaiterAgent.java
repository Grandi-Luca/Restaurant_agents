package com.example;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class WaiterAgent extends Agent {
    private boolean busy = false;

    protected void setup() {
        System.out.println("Waiter agent " + getAID().getName() + " is ready.");

        doWait(10000);

        // Register the waiter service in the yellow pages
        registerWaiter();

        // Add the behaviour handling the handshaking with the customer agent
        addBehaviour(new HandShakeCustomerBehaviour());

        // Add the behaviour serve finished order
        addBehaviour(new ServeOrder());
    }

    private class HandShakeCustomerBehaviour extends CyclicBehaviour {
        public void action() {
            // Receive the message from the customer agent
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.MatchPerformative(ACLMessage.CFP),
                    MessageTemplate.MatchConversationId("customer-ready-to-order"));
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                ACLMessage reply = msg.createReply();
                if (busy) {
                    reply.setPerformative(ACLMessage.REFUSE);
                } else {
                    reply.setPerformative(ACLMessage.PROPOSE);
                    deregisterWaiter();
                    addBehaviour(new OrderBehaviour(msg.getSender()));
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }

    }

    private class OrderBehaviour extends Behaviour {
        private AID currentCustomer;

        public OrderBehaviour(AID currentCustomer) {
            this.currentCustomer = currentCustomer;
        }

        public void action() {
            // Receive the message from the customer agent
            MessageTemplate mt = MessageTemplate.and(
                    MessageTemplate.and(
                            MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                            MessageTemplate.MatchConversationId("order-request")),
                    MessageTemplate.MatchSender(this.currentCustomer));
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // Send the message to the chef agent
                ACLMessage order = new ACLMessage(ACLMessage.INFORM);
                order.addReceiver(new AID("chef", AID.ISLOCALNAME));
                order.setConversationId("order-request");
                order.setContent("Order-" + msg.getContent() + "-for-" + msg.getSender().getName());
                myAgent.send(order);
                registerWaiter();
            } else {
                block();
            }
        }

        @Override
        public boolean done() {
            return !busy;
        }
    }

    private class ServeOrder extends CyclicBehaviour {
        private int step = 0;
        private MessageTemplate mt;
        private ACLMessage msg;

        public void action() {
            switch (step) {
                case 0:
                    // Receive the message from the chef agent
                    mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.CFP),
                            MessageTemplate.MatchConversationId("order-ready"));
                    msg = myAgent.receive(mt);
                    if (msg != null) {
                        ACLMessage reply = msg.createReply();
                        if (busy) {
                            reply.setPerformative(ACLMessage.REFUSE);
                        } else {
                            reply.setPerformative(ACLMessage.PROPOSE);
                            deregisterWaiter();
                            mt = MessageTemplate.and(MessageTemplate.MatchConversationId("order-ready"),
                                    MessageTemplate.MatchSender(msg.getSender()));
                            step = 1;
                        }
                        myAgent.send(reply);
                    } else {
                        block();
                    }
                case 1:
                    // Receive the message from the chef agent
                    msg = myAgent.receive(mt);
                    if (msg != null) {
                        // Simulate the serving time
                        doWait(2000);
                        // Send the message to the customer agent
                        ACLMessage order = new ACLMessage(ACLMessage.INFORM);
                        order.addReceiver(new AID(msg.getContent(), AID.ISGUID));
                        order.setConversationId("order-ready");
                        order.setInReplyTo("order " + msg.getContent());
                        myAgent.send(order);
                        registerWaiter();
                        step = 0;

                    } else {
                        block();
                    }
                    break;

                default:
                    break;
            }

        }

    }

    private void registerWaiter() {
        busy = false;
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());
        ServiceDescription sd = new ServiceDescription();
        sd.setType("waiter-available");
        sd.setName("JADE-water-available");
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
            System.out.println("Waiter " + getAID().getName() + " registered in DF.");
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

    private void deregisterWaiter() {
        busy = true;
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
    }

}
