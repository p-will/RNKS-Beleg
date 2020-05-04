import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;
import java.util.TimerTask;
import java.util.Timer;

public class Beleg_Client
{

	public static final Integer ACKLENGTH   = 3;     //Länge des Bestätigungspakets
	public static final Integer MTU = 1500;   
	
	public static Long sRTT = 1000l;       //Startwert 1s
	public static Long eRTT = 3000l;
	public static Long devRTT = 1500l;

	public static DatagramSocket Client_Socket;
	public static byte Session_Number[] = new byte[2];     
	public static Long File_Length;
	public static Integer File_Name_Length;
	public static String File_Name;
	public static Boolean Approved = new Boolean(false);
	public static Integer Server_Port;
	public static InetAddress IP_Adress;
	public static Integer Tries = new Integer(0);
	public static Integer Package_Number = new Integer(0);
	public static Long Data_Transmitted_Intervall = new Long(0);
	public static Long Data_Transmitted_Full = new Long(0);
	public static Long Session_Start = new Long(0);
	public static Long Start_Intervall = new Long(0);

	public static FileInputStream fis;
	public static CheckedInputStream cis;
	public static Integer EOF;

	
	static class Timer_Help extends TimerTask
	{
		@Override
		public void run ()
		{
			Long End_Intervall = new Long(System.currentTimeMillis());
			System.out.println("Datenrate: " + (double) Math.round(((double) Data_Transmitted_Intervall/(double)((End_Intervall-Start_Intervall))*100.0)/100.0) + " kByte/s.");
			Data_Transmitted_Intervall = 0l;
			Start_Intervall = new Long(System.currentTimeMillis());

		}
	}

	public static Timer T = new Timer();
	public static TimerTask Task = new Timer_Help(); 

   
   public static void Rec_ACK(Integer Pack_Number,DatagramPacket Curr_Packet)
   {
		byte ACK_buffer[] = new byte[ACKLENGTH];    //2Byte Sessionnumber und 1Byte ACKNumber

		DatagramPacket ACK_Packet = new DatagramPacket(ACK_buffer, ACK_buffer.length);
		 
		try 
		{
			Client_Socket.receive(ACK_Packet);
		} catch (IOException e) 
		{
			Tries ++;
			Send(Curr_Packet);
			return;
		}

		Integer Session_Number_Server = (((ACK_buffer[0] & 0xff )) | ((ACK_buffer[1] & 0xff)>>8)); 
		byte Pack_ACK = ACK_buffer[2];
		
		//System.out.println("Session Number: " + Session_Number_Server +" , ACK: " + Pack_ACK );
		if(Pack_ACK != Pack_Number || Session_Number_Server.compareTo((Session_Number[0] & 0xFF) | ((Session_Number[1] & 0xFF)>>8)) != 0)
		{
		    System.out.println("Falsches Paket oder Session. Wiederholung");
			Send(Curr_Packet);
			
		}

		Package_Number = (Package_Number == 1) ?  0 : 1;
                Tries = 0;
		return;
   }

   public static int calcRTT()
   {
	   eRTT = Math.round((1-0.125) * eRTT + 0.125 * sRTT);
	   devRTT = Math.round((1-0.125) * devRTT + 0.125 * (sRTT - eRTT));
	   Integer RTO = new Integer(Math.round(eRTT + 4*devRTT));
	   RTO = RTO < 200? eRTT.intValue() : RTO;  
	   return RTO.intValue();
   }

   public static void Send(DatagramPacket Packet)
   {

		Long Start = new Long(System.currentTimeMillis());
		try {
		//Client_Socket.setSoTimeout(calcRTT());
		Client_Socket.send(Packet);
		} catch (IOException e) {
			System.out.println("Senden des Datenpakets fehlgeschlagen.");
			Long End = new Long(System.currentTimeMillis());
			sRTT = new Long(new Long(End-Start));
			return;
		}
		Rec_ACK(Package_Number,Packet);
		Long End = new Long(System.currentTimeMillis());
		sRTT = new Long(new Long(End-Start));
		Data_Transmitted_Full += Packet.getLength();
		Data_Transmitted_Intervall += Packet.getLength();
		return;

   }

   public static void Start_Package()
   {
		if(Tries.compareTo(10)==0)
		{
			System.out.println("Zu viele Fehlschläge. Abbruch der Verbindung");
			Approved = false;
			return;
		}	
	
		byte Start_Data[] = new byte[3];

		File f = new File(File_Name);
		File_Length = f.length();
		File_Length = File_Length & 0xffffffffl;      //stellt korrekte Darstellung als unsigned wert sicher
		File_Name_Length = File_Name.length();
         
	
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		try {
			
		outputStream.write( (Session_Number[0]));
		outputStream.write( (Session_Number[1]));         
		outputStream.write( new Integer(0));
		outputStream.write( new String("Start").getBytes("ASCII"));
		outputStream.write(ByteBuffer.allocate(Long.SIZE/Byte.SIZE).putLong(File_Length).array());
		outputStream.write( ((File_Name_Length & 0xff)>>8));
		outputStream.write( (File_Name_Length & 0xff));         //funktioniert?
		outputStream.write(File_Name.getBytes("UTF-8"));
		Start_Data = outputStream.toByteArray();


		CRC32 crc = new CRC32();
		crc.update(Start_Data);

		byte crc_buf[] = new byte[8];
		ByteBuffer.wrap(crc_buf).putInt((int)crc.getValue());

		outputStream.write(crc_buf, 0, 4);
		Start_Data = outputStream.toByteArray();
		

		} catch (IOException e) 
		{
			System.out.println("Erstellen des Startpakets fehlgeschlagen");	
			return;
		}
		DatagramPacket Start_Packet = new DatagramPacket(Start_Data,Start_Data.length,IP_Adress,Server_Port);
		try
		{
		Client_Socket.setSoTimeout(1000);
		} catch (Exception e) 
		{
			System.out.println("Fehler beim Setzen des Starttimeouts");
			return;
		}

		Send(Start_Packet);
		
		Approved = true;


   }
   
   public static void Data_Package()
   {
		if(Tries.compareTo(10)==0)
		{
			System.out.println("Zu viele Fehlschläge. Abbruch der Verbindung");
			Approved = false;
			return;
		}	

		byte Data[] = new byte[MTU];
		try {
			
			Data[0] = (byte) ((Session_Number[0]));  
			Data[1] = (byte) ((Session_Number[1]));  
			Data[2] =(new Integer(Package_Number).byteValue());
			EOF = new Integer(cis.read(Data, 3, Data.length-3));
		} catch (IOException e) 
		{
			System.out.println("Erstellen des Datenpakets fehlgeschlagen");	
			return;
		}
		if(EOF != -1 && EOF > MTU - 7)    //Falls eof nach lesen nicht erreicht, oder keine 4 Bytes mehr für CRC32 zur Verfügung stehen
		{
			DatagramPacket Data_Packet = new DatagramPacket(Data,Data.length,IP_Adress,Server_Port);
			Send(Data_Packet);
	    }
		else
			End_Package(Data);
   }
 
   public static void End_Package(byte [] Data)
   {	
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

		byte crc_buf[] = new byte[8];
		ByteBuffer.wrap(crc_buf).putLong(cis.getChecksum().getValue());

		outputStream.write(Data,0,EOF+3);
		outputStream.write(crc_buf, 4, 4);
		Data = outputStream.toByteArray();

		DatagramPacket Data_Packet = new DatagramPacket(Data,Data.length,IP_Adress,Server_Port);
		Send(Data_Packet);
		Approved = false;
		Long Session_End = new Long(System.currentTimeMillis());
		System.out.println("Ende der Übertragung. Insgesamt übertragene Daten: " + Data_Transmitted_Full + ". Benötigte Zeit: " + (double) Math.round((((Session_End-Session_Start)/1000)*100.0)/100.0) + "s. Datenrate: " + (double) (Math.round(((double) Data_Transmitted_Full/(double) (Session_End-Session_Start)*100.0)/100.0)) + "kByte/s.");
		return;
   }
   public static void main(String args[]) throws Exception
	{
		Session_Start = new Long(System.currentTimeMillis());
		Start_Intervall = Session_Start;
		if(args.length!=3)
		{
			System.out.println("Ungültige Parameter Anzahl. Bitte geben sie Zieladresse,Portnummer und Dateiname an.");
			return;
		}
		IP_Adress = InetAddress.getByName(args[0]);
		Server_Port = Integer.parseInt(args[1]);
		File_Name = args[2];

		try 
		{
		Client_Socket = new DatagramSocket();
		} catch (SocketException e) {
			System.out.println("Socket konnte nicht gebunden werden. Bitte versuchen sie einen andere Portnummer");
			return;
		}
		
		Random rand = new Random();
		rand.nextBytes(Session_Number);   

		
		Start_Package();
		System.out.println("Verbunden");

		if(Approved == false)
		{
			System.out.println("Fehler beim Aufbauen der Verbindung. Abbruch.");
			return;
		}

		try 
		{
			fis = new FileInputStream(File_Name);
			cis = new CheckedInputStream(fis, new CRC32());
		} catch (FileNotFoundException e) 
		{
			System.out.println("Fehler beim Öffnen der Datei");
			return;
		}
		T.scheduleAtFixedRate(Task, 0, 5000);

		while(Approved == true)
		{
			Data_Package();
		}

		fis.close();
		T.cancel();
		Client_Socket.close();

	}
}
