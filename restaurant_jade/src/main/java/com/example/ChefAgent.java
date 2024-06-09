package com.example;

import java.util.Random;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.core.behaviours.WakerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

public class ChefAgent extends Agent {
    private AID[] waiterAgents;

    protected void setup() {
        System.out.println("Chef agent " + getAID().getName() + " is ready.");

        // Search for available waiter agents
        addBehaviour(new TickerBehaviour(this, 1000) {
            protected void onTick() {
                // System.out.println("Check if there are any waiter agents available");
                // Update the list of waiter agents available
                DFAgentDescription template = new DFAgentDescription();
                ServiceDescription sd = new ServiceDescription();
                sd.setType("waiter-available");
                sd.setName("JADE-water-available");
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

        // Add the behaviour serving incoming requests for order
        addBehaviour(new OrderBehaviour());
    }

    private class OrderBehaviour extends CyclicBehaviour {
        public void action() {
            // Receive the message from the waiter agent
            MessageTemplate mt = MessageTemplate.and(MessageTemplate.MatchPerformative(ACLMessage.INFORM),
                    MessageTemplate.MatchConversationId("order-request"));
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                String order = msg.getContent().split("-")[1];
                String customerAID = msg.getContent().split("-")[3];
                System.out
                        .println("Chef is preparing " + order + " for customer: " + customerAID);

                addBehaviour(new WakerBehaviour(myAgent, (new Random().nextInt(1, 7) * 1000)) {
                    protected void onWake() {
                        System.out.println("Chef has finished preparing the order for customer " + customerAID);
                        addBehaviour(new ServeOrderBehaviour(msg, customerAID));
                    }
                });
            } else {
                block();
            }
        }
    }

    private class ServeOrderBehaviour extends Behaviour {
        private int step = 0;
        private MessageTemplate mt;
        private ACLMessage msg;
        private String customerAID;
        private static final int REQUEST_PHASE = 0;
        private static final int RESPONSE_PHASE = 1;

        public ServeOrderBehaviour(ACLMessage msg, String customerAID) {
            this.msg = msg;
            this.customerAID = customerAID;
        }

        @Override
        public void action() {
            switch (step) {
                case REQUEST_PHASE:
                    if (waiterAgents == null || waiterAgents.length == 0) {
                        break;
                    }
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    cfp.addReceiver(waiterAgents[new Random().nextInt(waiterAgents.length)]);
                    cfp.setConversationId("order-ready");
                    cfp.setReplyWith("order-ready " + System.currentTimeMillis());
                    myAgent.send(cfp);
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("order-ready"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = RESPONSE_PHASE;
                    break;

                case RESPONSE_PHASE:
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            ACLMessage order = msg.createReply();
                            order.setPerformative(ACLMessage.INFORM);
                            order.setConversationId("serve-order");
                            order.setContent(customerAID);
                            myAgent.send(order);
                            step = RESPONSE_PHASE + 1;
                        } else {
                            // Ask another waiter
                            step = REQUEST_PHASE;
                        }
                    } else {
                        block();
                    }
                    break;

                default:
                    break;
            }
        }

        @Override
        public boolean done() {
            return step == 2;
        }

    }
}
