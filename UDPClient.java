import java.io.*;
import java.net.*;
import java.util.Arrays;
import java.util.Random;

class UDPClient{
   static Random rand = new Random();
   public static void main(String[] args) throws Exception 
   {
      Random ge = new Random();
      String sentence = "Get TestFile.html HTTP/1.0";
      
      DatagramSocket clientSocket = new DatagramSocket();
      
      InetAddress IPAddress = InetAddress.getByName("Apple.local");
      
      byte[] sendData = new byte[128];
      byte[] receiveData = new byte[128];
      
      sendData = sentence.getBytes();
      DatagramPacket sendPacket = 
         new DatagramPacket(sendData, sendData.length, IPAddress, 10022);
        
      clientSocket.send(sendPacket);
      System.out.println("Request have been sent, wait for transmiting...");
      
      DatagramPacket receivePacket =
         new DatagramPacket(receiveData, receiveData.length);
       
      String line;
      
      File file = new File("ServerDataBig.txt");
      BufferedWriter toFile = new BufferedWriter(new FileWriter(file));
      
      byte totalPacket[] = new byte[128];
      byte byteSum[] = new byte[5];
      int serverSideSum = 0, clientSideSum = 0, beez = 0;
      String serverStringSum = "";
      boolean corruption = false;
      
      int header;
      int[] seq = {10, 11, 12, 13};
      String ACK = "";
      String[] buffer = new String[42];
      for(int i=10; i<42; i++){
         buffer[i] = "";
      }
      String[] ack = {"0", "0", "0", "0"};//this is just a transition in order to get String ACK
      
      clientSocket.receive(receivePacket);
      /*totalPacket = receivePacket.getData();
      for (int i = 123; i < 128; i++)
      {
         byteSum[beez] = totalPacket[i];
         totalPacket[i] = 0;
         beez++;
      }
      beez = 0;
      serverStringSum = new String(byteSum);
      serverSideSum = Integer.parseInt(serverStringSum);
      
      totalPacket = gremlin(totalPacket, 123, .5);//possible corruption
      clientSideSum = checkSum(totalPacket, 123);//get new checkSum
      if (serverSideSum != clientSideSum)//determine if packet data is bad now
         corruption = true; */  
      int drop;
      
      byte[] message = new byte[121];
      message = Arrays.copyOfRange(receivePacket.getData(), 2, 123);
      
      //while loop condition variable, just generate a null String.
      byte[] loopControl = new byte[121];
      String end = new String(loopControl);
      while(!((line = new String(message)).equals(end))){
         int index = 0;
         //try   
         header = Integer.parseInt(new String(Arrays.copyOfRange(receivePacket.getData(), 0, 2)));
         //catch (NumberFormatException e){
            //System.out.println("Header value was corrupted");}  
         
         for(int i=0; i<4; i++){
            if (seq[i]==header){
               index = i;
               break;
            }
         }
         
         if(ge.nextInt(51)>1){/*if condition is ture, that means corrutpiton happend. 
            I am not sure how my partner's gremlin funtion works, So instead I just use a method in random class.
            ge.nestInt(51)>1 means loss rate is 49/51 = 0.96*/
            ack[index] = "-";
            corruption = false;
            System.out.println(header + "corruption happend");           
         }//error detect
         if(index == 0){
            if(!ack[index].equals("-")){
               toFile.write(line);
               ack[0] = "+";
            
               for(int i=0; i<4; i++){
                  if(seq[i]==41){seq[i] = 10;}
                  else{seq[i]++;}
                        
               }  
               System.out.println(header + " OK");
            
            }
            
            for(int i=0; i<4; i++){
               ACK = ACK + ack[i];
            }
            sendData = ACK.getBytes();
            sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 10022);
            clientSocket.send(sendPacket);
            ACK = "";//reset after one transmission;
            if(ack[index].equals("+")){
               for(int i=0; i<3; i++){
                  ack[i] = ack[i+1];
                  
               }
               ack[3]="0";
            
            }
                        
            for (int i=0; i<4; i++)
            {
               if (ack[i].equals("-"))
               {
                  ack[i] = "0";
               }
            }
                
            
         }
         else{       
            if(!ack[index].equals("-")){
               buffer[header] = line;
               ack[index] = "+";  
               System.out.println(header +"Saved in buffer"); }                                 
         }
         while(ack[0].equals("+")){
            toFile.write(buffer[seq[0]]);
            for(int i=0; i<4; i++){
               if(seq[i]==41){seq[i] = 10;}
               else{seq[i]++;}                   
            }  
         
            for(int i=0; i<3; i++){
               ack[i] = ack[i+1];
            }
            ack[3]="0";
         }
         clientSocket.receive(receivePacket);
         
         drop = ge.nextInt(10);
         while(drop >8){
            System.out.println("Droping happened!");
            clientSocket.receive(receivePacket);
            drop = ge.nextInt(10);
         }
         /*totalPacket = receivePacket.getData();
         for (int i = 123; i < 128; i++)
         {
            byteSum[beez] = totalPacket[i];
            totalPacket[i] = 0;
            beez++;
         }
         beez = 0;
         serverStringSum = new String(byteSum);
         try
         {serverSideSum = Integer.parseInt(serverStringSum);}
         catch (NumberFormatException e){
            System.out.println("Num error line 139");
            serverSideSum = 0;}
      
         totalPacket = gremlin(totalPacket, 123, .5);//possible corruption
         clientSideSum = checkSum(totalPacket, 123);//get new checkSum
         if (serverSideSum != clientSideSum)//determine if packet data is bad now
            corruption = true;*/
         
         message = Arrays.copyOfRange(receivePacket.getData(), 2, 123);
             
      }
      sendData = "+000".getBytes();
      sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress, 10022);
      clientSocket.send(sendPacket);
     
      toFile.close();      
      System.out.println("Packet is received correctly");
      clientSocket.close();
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
   
   //Allow corruption probablity to be settable
   //Flip one bit = .5
   //Flip two bits = .3
   //Flip three bits = .2
   //Prob of lossing framents = .3 **not shown yet
   /*
   *Function to corrupt bits in a byte array, mra0016
   *@param incArray is the array passed to be considered for corruption
   *@param length is the length of the byte array sent in
   *@param prob is the probability of corruption to occur (between 0 and 1.0)
   *be sure to import java.util.Random; and instantiate Random rand = new Random()
   *NOT YET ACCOUNTED FOR is complete packet loss!!!!!
   */
   private static byte[] gremlin(byte[] incArray, int length, double prob)
   {
      int chance = (int) (prob * 100);
      int failChance = rand.nextInt(100) + 1;
   
      if (chance >= failChance)
      {
         int probability = rand.nextInt(10);
         int byteSelect, bitSelect, loopCount = 1;
         byte theChange;
      
         if (probability >= 5 && probability < 8)
            loopCount = 2;
         if (probability >= 8)
            loopCount = 3;      
      
      //to change one bit in the whole array of bytes
      //loop number of times based on probability
         for (int i = 0; i < loopCount; i++) 
         {
            byteSelect = rand.nextInt(length);
            if (byteSelect == 0 || byteSelect == 1)
               byteSelect = 2;
            bitSelect = rand.nextInt(8);
         
            theChange = incArray[byteSelect];
            theChange ^= (1 << bitSelect);
            incArray[byteSelect] = theChange;
         } 
         return incArray; 
      }
      else //lucky enough to not be corrupted.
         return incArray;  
   }
}