package MyServer;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

class Node{
   String sender="";
   String receiver="";
   String message="";
   Node next=null;
}

class MyQueue{
    int size;
    Node first;
    Node last;

    MyQueue(){
     size=0;
     first=last=null;
    }

    synchronized void inqueue(String message, String sender, String receiver){
        Node myNode=new Node();
         if(first==null)
         {
             first=last=myNode;
             first.message=message;
             first.sender=sender;
             first.receiver=receiver;
             first.next=null;
         }
         else{
             Node oldLast=last;
             last=myNode;
             last.message=message;
             last.sender=sender;
             last.receiver=receiver;
             last.next=null;
             oldLast.next=last;
             
         }
         size++;
    }

    synchronized Node dequeue(){
        if(size==0 || first==null)
           return null;
         size--;
         Node temp=first;
         first=temp.next;
         return temp;
    }

    synchronized Node getFirst(){
            return first;
    }

    synchronized boolean isEmpty(){
        if(first==null)
           return true;
        else
           return false;
    }
}

//This class dispataches message to right clients queue for further sending.
class DispatchMessage implements Runnable{
        public int stop=1;
        Thread t;
        DispatchMessage(){
            t=new Thread(this, "Dispatch Message");
            t.start();
        }
    public void run()
        {
            while(stop==1){
                if(SuperServer.msg_distributor.isEmpty()){
                    try{
                        Thread.sleep(2000);
                       } catch(InterruptedException e){
                           System.out.println("Thread interrupted");
                       }
                }
                else
                {
                    //wriite code to distribute the message.
                    Node dpMsg=SuperServer.msg_distributor.dequeue();
                    System.out.println("Printing Receiver: "+dpMsg.receiver+dpMsg.message+dpMsg.sender);
                    
                    try {
                        int dpMsgId= Integer.parseInt(dpMsg.receiver);
                    } catch (Exception e) {
                        //TODO: handle exception.
                    }

                    NewConnection target= SuperServer.client_list[dpMsgId];
                    target.out_message.inqueue(dpMsg.message, dpMsg.sender, dpMsg.receiver);
                }
            }
         System.out.println("Dipatch Message Service Closed");
        }
}

//This class is for waiting, accepting and providing new thread for each connection.
class AcceptConnection implements Runnable{
    int stop=1;
    Thread t;
    Socket s;
    ServerSocket ss=null;
    OutputStream tempOutPut;

    AcceptConnection(){
        t= new Thread(this,"AcceptConnection");
        t.start();
    }
    public void run(){
        //Creating Socket that have port numbe 50800
        try{
            ss = new ServerSocket(50080);
        }catch(IOException e){
            // Write error upai.
        }

        //loop to accept connections....
        while(stop==1){
           if(SuperServer.serverStatus==1){//Checking command for controlinning Server.
               try{
                    s= ss.accept();
                 } catch(IOException e){
                     ///Write..
                 }
              if(SuperServer.total_active_connection<109) //checking capacity..
                {   int n;
                    if(SuperServer.total_connected_connection<109)
                         {
                             SuperServer.Client_id++;
                         }
                    else
                        {
                            for(n=0; n<109; n++)
                                {
                                    if(SuperServer.client_list[n]==null)
                                       {SuperServer.Client_id=n; break;}
                                }
                        } 
                    SuperServer.total_connected_connection++;
                    SuperServer.total_active_connection++;
                    NewConnection new_connection= new NewConnection(SuperServer.Client_id.toString(), s);
                    SuperServer.client_list[SuperServer.Client_id]=new_connection; 
                }
              else{
                   //Sending sorry message for connection limit crossed.
                   try{
                      tempOutPut= s.getOutputStream();
                     }catch(IOException e){
                         //Write how to handle.
                     }
                   String tempMessage="Sorry for inconvenience. Currently connection limit is full. Please try sometime later. Thank you.";
                   byte[] tempBuff=tempMessage.getBytes();
                   try{
                       tempOutPut.write(tempBuff);
                   } catch(IOException e){}
                   try{
                   s.close();
                   } catch(IOException e){}
                }
           }
        else
           {
               try{
               Thread.sleep(2000);
               } catch(InterruptedException e){}
           } 
    }

   
    System.out.println("Accept Connection Service Closed");
}
}

//This class will send message to client...
class SendMessage implements Runnable{
    int stop=1;
    Thread t;
    Socket s;
    String client_id;
    MyQueue out_message;
    
    OutputStream out;

    SendMessage(Socket socket, String client, MyQueue out_msg){
         s=socket;
         out_message=out_msg;
         client_id=client;
         try {
            out=s.getOutputStream();
         } catch (Exception e) {
             //TODO: handle exception
         }
         t=new Thread(this, client_id);
         t.start();
    }
    public void run(){
           while(stop==1){
               if(!out_message.isEmpty())
                  {
                      try{
                      byte[] buff=out_message.getFirst().message.getBytes();
                      out.write(buff);
                      out.write(10);
                      buff=out_message.dequeue().sender.getBytes();

                     // out.write(buff);
                      //out.write(10);
                      } catch(IOException e){}
                  }
                else
                try {
                    Thread.sleep(2000);
                } catch (Exception e) {
                    //TODO: handle exception
                }
           }
        
    }
}

//Each object of this class is a new connection..
class NewConnection implements Runnable{
    int stop=1;
    String client_name="";
    String client_Ip;
    String client_id;
    Socket s;
    Thread t;
    InputStream in;
    OutputStream out;
    MyQueue out_message;
    SendMessage sendMessage;

    NewConnection(String clientId, Socket socket){
          client_id=clientId;
          s=socket;
          out_message= new MyQueue();
          sendMessage= new SendMessage(s, client_id,out_message);
          t = new Thread(this, client_id);
          t.start();
    }

    @Override
    public void run() {
        Integer c;
        int d;
        try{
        //Obtain input and output streams
        in = s.getInputStream();
        out = s.getOutputStream();
        System.out.println("New Connection Started");
        
        //Taking basic details..
        while((c=in.read())!=10)
             {
                 client_name=client_name+c.toString();
             }
        String response=client_id+"\n";
        byte[] out_buff=response.getBytes();
        out.write(out_buff);
         
        char[] receive=new char[400];
        int count=0;
        int n=0;
        String[] msg=new String[3];
        
         //break the connection check..
        while(stop==1)
        {
            for(int s=0; s<2; s++){
            while((d=in.read()) !=10){
               receive[n] =((char)d);
               n++;
            } 

            msg[count]=new String(receive,0,n);
            n=0;
            count++;
        }
        
              
                  count=0;
                  SuperServer.msg_distributor.inqueue(msg[0],client_id, msg[1]);
               
        }
    } catch(Exception e){}
    sendMessage.stop=0;

    SuperServer.client_list[Integer.parseInt(client_id)]=null;
    SuperServer.total_active_connection--;
    System.out.println("Closed connection with "+client_name);
    }

}

public class SuperServer {
    public static  MyQueue msg_distributor=new MyQueue();
    public static int total_connected_connection=0; // Recording total connected connections active or not active.
    public static int total_active_connection=0;
    public static Integer Client_id=-1;   // It also shows the postion on arraey client_list.
    public static NewConnection[] client_list= new NewConnection[111];
    public static int serverStatus=0;
    public static void main(String[] args) {
         System.out.println("WELCOME TO SERVER");
         System.out.println("-------*-------*--------*");
         System.out.println("Follow below instructions to operate this server :-");
         System.out.println("0 : Start accepting Connections\n");
         System.out.println("1 : Stop accepting Connections\n");
         System.out.println("2 : Stop Server\n");
         int pass=1;

         AcceptConnection ac= new AcceptConnection(); //Listening on by server started on port 50080.

         DispatchMessage dm= new DispatchMessage(); // Thread for dispatch message stared.

         java.util.Scanner input= new java.util.Scanner(System.in);
         while(pass==1)
          {
              System.out.println("Please give instruction");
               int command=input.nextInt();
              //parsing instruction and executing
              switch(command){
                  case 0: {
                         SuperServer.serverStatus=1;
                         System.out.println("Started accepting connections");
                         
                  }
                  break;
                  case 1:{
                         SuperServer.serverStatus=0;
                         System.out.println("Stoped accepting connections");
                  }
                  break;
                  case 2:{
                          pass=0;
                  }
                  break;
                  case 3:{}
                  break;
                  case 4: {}
                  break;
                  default :{}
              }

          }
           //Closing all other tthreads...
           input.close();
           ac.stop=0;
           try {
            ac.ss.close();
           } catch (Exception e) {
               //TODO: handle exception
           }
           dm.stop=0;
           for(int e=0; e<111; e++)
              {
                  if(client_list[e] != null)
                     {
                         client_list[e].stop=0;
                         try {
                            client_list[e].s.close();
                         } catch (Exception g) {
                             //TODO: handle exception
                         }
                     }
              }
           
        }
      


    }

