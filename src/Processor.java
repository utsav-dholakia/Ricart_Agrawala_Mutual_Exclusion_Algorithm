import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Iterator;


public class Processor extends Thread{

    @Override
    public void run() {
        try {
                Message inMessage = RicartAgrawalaMutex.messagesToBeProcessed.take();
                System.out.println("Message - " + inMessage.getMessageType() + " from node - " + inMessage.getSrcNodeID() + " at time - " + inMessage.getTimeStamp());
                switch(inMessage.getMessageType()) {
                    case Request:
                        updateClockValue(inMessage);
                        //Check if my request is pending or not in the requestQueue
                        Boolean ownRequestFound = false;
                        Iterator<RequestObject> itr = RicartAgrawalaMutex.requestQueue.iterator();
                        while(itr.hasNext()){
                            RequestObject obj = itr.next();
                            if(obj.getNodeId().equals(CriticalSection.self.getNodeId())){
                                //If my pending request has timestamp larger than the current request
                                if(inMessage.getTimeStamp() < obj.getTimeStamp()) {
                                    //Send reply to go ahead with execution
                                    sendReplyMessage(inMessage);
                                }
                                //Tie-breaker, if same timestamp, then reply if incoming message is from smaller nodeID
                                else if((inMessage.getTimeStamp() == obj.getTimeStamp()) &&
                                        (inMessage.getSrcNodeID() < CriticalSection.self.getNodeId())) {
                                    //Send reply to go ahead with execution
                                    sendReplyMessage(inMessage);
                                }
                                else{
                                    //Deferring incoming request to my requestQueue
                                    RequestObject requestObject = new RequestObject(inMessage.getTimeStamp(), inMessage.getSrcNodeID());
                                    RicartAgrawalaMutex.requestQueue.add(requestObject);
                                }
                                ownRequestFound = true;
                                break;
                            }
                        }
                        //If my request is not pending
                        if(!ownRequestFound){
                            //Send reply to go ahead with execution
                            sendReplyMessage(inMessage);
                        }
                        break;
                    case Reply:
                        updateClockValue(inMessage);
                        verifyConditions(inMessage);
                        break;
                    default:
                        break;
                }
            } catch (Exception e) {
            System.out.println("Exception in handling incoming message");
            e.printStackTrace();
        }

    }

    public static void verifyConditions(Message inMessage){
        //If all neighbours have replied
        if(RicartAgrawalaMutex.replyPending != null) {
            //Reply received from a neighbour, remove it from reply pending list
            if (RicartAgrawalaMutex.replyPending.contains(inMessage.getSrcNodeID())) {
                RicartAgrawalaMutex.replyPending.remove(inMessage.getSrcNodeID());
            }
            if (RicartAgrawalaMutex.replyPending.size() == 0 && RicartAgrawalaMutex.requestQueue.size() > 0) {
                Iterator<RequestObject> itr = RicartAgrawalaMutex.requestQueue.iterator();
                while(itr.hasNext()){
                    RequestObject obj = itr.next();
                    if(obj.getNodeId().equals(CriticalSection.self.getNodeId())){
                        RicartAgrawalaMutex.requestQueue.remove(obj);
                        System.out.println("Removed my request from requestQueue at time - " + RicartAgrawalaMutex.scalarClock);
                        break;
                    }
                }
                //Enter critical section, L1,L2 conditions are true
                RicartAgrawalaMutex.isExecutingCS = true;
            }
        }
    }

    public static void updateClockValue(Message inMessage){
        //Take max of local clock value and inMessage clock value
        Integer maxValue = Math.max(RicartAgrawalaMutex.scalarClock, inMessage.getTimeStamp());
        RicartAgrawalaMutex.scalarClock = maxValue + 1;

    }

    public static void sendReplyMessage(Message inMessage) {
        //Update scalar clock to mark a send event
        RicartAgrawalaMutex.scalarClock = RicartAgrawalaMutex.scalarClock + 1;
        System.out.println("Sending Reply src node - " + CriticalSection.self.getNodeId() + " time - " + RicartAgrawalaMutex.scalarClock);
        Message replyMessage = new Message(MessageType.Reply, CriticalSection.self.getNodeId(), RicartAgrawalaMutex.scalarClock);	//control msg to 0 saying I am permanently passive
        try{
            Socket socket = new Socket(CriticalSection.nodeMap.get(inMessage.getSrcNodeID()).getNodeAddr(), CriticalSection.nodeMap.get(inMessage.getSrcNodeID()).getPort());
            ObjectOutputStream outMessage = new ObjectOutputStream(socket.getOutputStream());
            outMessage.writeObject(replyMessage);
            socket.close();
        }catch(Exception e){
            System.out.println("Exception in sending reply message");
            e.printStackTrace();
        }
    }
}