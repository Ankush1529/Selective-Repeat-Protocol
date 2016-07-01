import java.io.*;
import java.net.*;
import java.util.Timer;
import java.util.TimerTask;
import java.lang.Runnable;

class UDPServer{
   public static void main(String[] args) throws Exception 
   {
      DatagramSocket serverSocket = new DatagramSocket(10022);
      byte[] receiveData = new byte[128];
      
      String[] ackArray = {"0", "0", "0", "0"}; //ack will be save in this array. 
        
            
      while(true){
         DatagramPacket receivePacket = 
            new DatagramPacket(receiveData, receiveData.length);
         System.out.println("Wait for request...");
         
         serverSocket.receive(receivePacket); 
        
         String inSentence = new String(receivePacket.getData());
         receiveData = new byte[4];
                 
         InetAddress IPAddress = receivePacket.getAddress();
         int port = receivePacket.getPort();
         receivePacket = 
            new DatagramPacket(receiveData, receiveData.length);
      
         
         File inFile = new File("TestFile.html");
         FileInputStream fin =  new FileInputStream(inFile);
         byte fileContent[] = new byte[(int)inFile.length()]; //buffer for whole file in byte array	     
         fin.read(fileContent);
      	        
         String headerContent = "HTTP/1.0 200 Document Follows\r\n"
            + "Conten-Type: text/plian\r\nContent-Length: "+ fileContent.length/1024 + "Kbytes\r\n\r\n";
         byte[] headerByte = headerContent.getBytes();
         
         //merge two arrays
         byte output[] = new byte[headerByte.length + fileContent.length];        
         for(int i=0; i<headerByte.length; i++){
            output[i] =  headerByte[i];
         }
         
         int x = 0;
         for(int i=headerByte.length; i< (headerByte.length + fileContent.length); i++){
            output[i] = fileContent[x];
            x++;
         }
         
         int serverCheckSum = 0, beez = 0;
         byte byteSum[] = new byte[5];
         String checkString = "";
                 
         int hold = output.length;
         int j=0,decide = 0, N = 4;
         DatagramPacket sendPacket;
         int nextseqnum = 0;
         int header = 10;
         byte[] byteHeader = new byte[2];
         int numWait = 0;
         int base = 0;
         Timer[] timerArray = new Timer[4];
         DatagramPacket[] packetArray = new DatagramPacket[4];
         int[] h = new int[4];
         
         long start = System.currentTimeMillis(); 
         
         while(decide == 0 || numWait != 0) {
                      
            if(nextseqnum < base + N && decide == 0){
               byte[] sendData = new byte[128];
               byteHeader = Integer.toString(header).getBytes();
               for(int i=0; i < 123;i++){
                  if(j < hold) {
                     if(i<2){sendData[i]=byteHeader[i];}
                     else{sendData[i]=output[j];
                        j++;
                     }
                  }
                  else {
                     decide = 1;
                     break;
                  }
               }
               //calculate check sum on the header and data stored within the 123 bytes
               //last 5 bytes of the 128 byte array is to store the checksum value
               serverCheckSum = checkSum(sendData, 123);
               checkString = String.valueOf(serverCheckSum);
               checkString = String.format("%5s", checkString).replace(' ', '0');
               byteSum = checkString.getBytes();
               
               for (int i = 123; i < 128; i++)
               {
                  sendData[i] = byteSum[beez];
                  beez++;
               }
               beez = 0;
               
               sendPacket = 
                  new DatagramPacket(sendData, sendData.length, IPAddress,
                  port);
               serverSocket.send(sendPacket);
               packetArray[numWait]=sendPacket;
               
               timerArray[numWait]= new Timer();
               timerArray[numWait].schedule(new srTimer(sendPacket, serverSocket), 100, 100);
               h[numWait]=header;
               
               numWait++;                             
               nextseqnum++;
               
               if(header == 41){header = 10;}
               else{header++;}                                                 
            }
            if(nextseqnum > 3){
               if(ackArray[0].equals("+")){                           
                  System.out.println(h[0]+ " ack receive!!!!");
                  for(int i=0; i<3; i++){
                     ackArray[i]=ackArray[i+1];
                     timerArray[i]=timerArray[i+1];
                     packetArray[i]=packetArray[i+1];
                     h[i]=h[i+1];
                  }
                  
                  numWait--;
                  ackArray[numWait] = "0";
                  base++;
               }
               
               else{
                  serverSocket.receive(receivePacket);
                  String re = new String(receivePacket.getData());//wait for ACK
               
                  for(int i=0; i<4; i++){
                     ackArray[i]=re.substring(i, i+1); //copy acks to universal buffer that holds acks
                  }
                  for(int i=0; i<4; i++){
                     if(ackArray[i].equals("+")){
                        timerArray[i].cancel();
                     }
                  }
               
                 
                  if(ackArray[0].equals("+")){           
                     System.out.println(h[0]+ " ack receive!");
                     for(int i=0; i<3; i++){
                        ackArray[i]=ackArray[i+1];
                        timerArray[i]=timerArray[i+1];
                        packetArray[i]=packetArray[i+1];
                        h[i]=h[i+1];
                     }
                                              
                     numWait--;
                     ackArray[numWait] = "0";
                     base++;
                  
                  }
               }
               for(int i=0; i<4; i++){   
                  if(ackArray[i].equals("-")){
                     timerArray[i].cancel();
                     serverSocket.send(packetArray[i]);
                     ackArray[i]="0";
                     timerArray[i] = new Timer();
                     timerArray[i].schedule(new srTimer(packetArray[i], serverSocket), 100, 100);
                  }
               }
            }
         }
         
         /* make a prepare for last transmint
         */
         byte[] lastOne = new byte[128];
         byteHeader = Integer.toString(header).getBytes();
         lastOne[0] = byteHeader[0];
         lastOne[1] = byteHeader[1];
         
         sendPacket = 
            new DatagramPacket(lastOne, lastOne.length, IPAddress,
               port);
         serverSocket.send(sendPacket);  
         
         timerArray[numWait] =new Timer();
         timerArray[numWait].schedule(new srTimer(sendPacket, serverSocket), 100, 100);
         
         serverSocket.receive(receivePacket);
         timerArray[0].cancel();
      
         long end = System.currentTimeMillis();
         System.out.println("Packets is sent correctly " + (end-start)); 
        
      }
    
   }
   
   //Do a checksum of the int byte values of the array
   private static int checkSum(byte[] theArray, int length)
   {
      int checkSum = 0;
      for (int i = 0; i < length; i++)
      {
         checkSum += theArray[i];
      }
      return checkSum;
   }
}

class srTimer extends TimerTask {
   private DatagramPacket sendPacket;
   private DatagramSocket serverSocket;
  
   public srTimer(DatagramPacket sendPacketIn, DatagramSocket serverSocketIn) { 
      sendPacket = sendPacketIn;
      serverSocket = serverSocketIn;
   }
   
   public void run() {
      try{
         serverSocket.send(sendPacket); 
      }
      
      catch(Exception e){
         System.out.print("Send error!");
      }
   
   }    
}

 
