import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.charset.CharsetEncoder;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;
import java.util.zip.CheckedInputStream;



public class Beleg_Server {
	
   public static final Integer STARTLENGTH = 277;   //Länge des Startpakets
   public static final Integer ACKLENGTH   = 3;     //Länge des Bestätigungspakets

   public static final Integer MTU = 1500;

   public static Integer Delay = 0;
   public static Double Package_Loss = 0.0;

   public static Random random = new Random();

   public static DatagramSocket Server_Socket;
   public static Integer Session_Number;    
   public static Long File_Length;
   public static Integer File_Name_Length;
   public static String File_Name = new String();
   public static Boolean Approved = new Boolean(false);
   public static Integer Client_Port;
   public static InetAddress IP_Adress;
   public static Integer Package_Number;
   public static File F;
   public static FileOutputStream fos;
   public static File transferred_File;
   public static Integer Tries = new Integer(0);
   
   

	public static void Send_ACK(Integer Pack_Number)
	{
		byte ACK_buffer[] = new byte[ACKLENGTH];    //2Byte Sessionnumber und 1Byte ACKNumber
		ACK_buffer[0] = (byte) (Session_Number & 0xff);
		ACK_buffer[1] = (byte) ((Session_Number>>8) & 0xff);
		ACK_buffer[2] = Pack_Number.byteValue();

		DatagramPacket ACK_Packet = new DatagramPacket(ACK_buffer, ACK_buffer.length,IP_Adress,Client_Port);
		try 
		{
			Thread.sleep((int) (random.nextDouble() * Delay * 2));
		} catch (InterruptedException e) 
		{
		}

		try {
			Server_Socket.send(ACK_Packet);
		} catch (IOException e) {
			System.out.println("Fehler beim Senden des ACK");   	
		}

		Package_Number = (Package_Number == 1) ? 0 : 1;
		return;
	}
    
	public static void Start_Package()
	{
		Tries = 0;
		final Integer Start_Kennung_Pos = 3;
		final Integer Name_Pos = 18;
		
		byte Start_Data_Max[] = new byte[STARTLENGTH];
		DatagramPacket Start_Packet = new DatagramPacket(Start_Data_Max, STARTLENGTH);
		try 
		{
			Thread.sleep((int) (random.nextDouble()* Delay * 2));
		} catch (InterruptedException e) 
		{
				
		}
		
		
		try 
		{
			Server_Socket.receive(Start_Packet);
			System.out.println("Empfangen");
		} catch (IOException e) 
		{
			//System.out.println("Fehler beim Empfangen des Startpakets");
			return;   
		} 

		if(random.nextDouble() < Package_Loss)
		{
			return;
		}
		byte Start_Data[] = new byte[Start_Packet.getLength()];
		try 
		{
		System.arraycopy(Start_Data_Max, 0, Start_Data, 0, Start_Data.length);
		} catch (IndexOutOfBoundsException e) 
		{
			System.out.println("Fehler beim Auswerten der empfangenen Daten");
			return;
		}
		ByteBuffer Start_Buffer = ByteBuffer.wrap(Start_Data);      
		Package_Number = new Integer(Start_Buffer.get(2));  //Paketnummer in Startpaekt an 3. Stelle
		if(Package_Number != 0)   //Wenn Paketnummer nicht 0 ist, ist Paket nicht Startpaket entsprechend Protokoll
		{
			return;
		}
		byte Start_Check[] = new byte[5];
		Start_Buffer.position(Start_Kennung_Pos);
		Start_Buffer.get(Start_Check, 0, 5);
		Start_Buffer.position(0);
		try 
		{
			if(!( new String(Start_Check,"ASCII").contentEquals("Start")))        //"Start" Kennung nicht vorhanden -> nicht Startpaket entsprechend Protokoll
			{
				return;
			}
		} catch (IOException e) 
		{
			System.out.println("Fehler bei Konvertierung der Startkennung");
			return;
		}
		
		Session_Number = (((Start_Data[0] & 0xff )) | ((Start_Data[1] & 0xff)>>8));     

		File_Length = Start_Buffer.getLong(8);        //liest 8 Bytes
		File_Length = File_Length & 0xffffffffl;      //stellt korrekte Darstellung als unsigned wert sicher
		

		File_Name_Length = (int)Start_Buffer.getShort(16);
		File_Name_Length = File_Name_Length & 0xffff;   //unsigned Darstellung
		
                System.out.println(File_Name_Length);

		byte File[] = new byte[File_Name_Length];
		Start_Buffer.position(Name_Pos);
		Start_Buffer.get(File,0,File_Name_Length);
		Start_Buffer.position(0);
		
		try 
		{
			File_Name = new String(File,"UTF-8");
			boolean file_exists = true;
			while(file_exists)
			{
				if(new File(File_Name).exists())
				{
					Integer pos = new Integer(File_Name.lastIndexOf('.'));
					String name = new String(File,0,pos,"UTF-8");                  //Wenn Datei bereits existiert, "1" an Dateinamen anhängen,"UTF-8"
					String end = new String(File,pos,File.length-pos,"UTF-8");
					File_Name = new String(name+"1"+end);
					File = File_Name.getBytes();                                   //updaten von File, falls Schleife nochmal durchlaufen wird
				}
				else
					file_exists = false;
			}
		} catch (IOException e) 
		{
			System.out.println("Fehler beim Lesen des Dateinamens.");
			return;  
		}


		byte crc_rec_buf[] = new byte[4];
		Start_Buffer.position(Start_Data.length-4);
		Start_Buffer.get(crc_rec_buf, 0, 4);

		
		CRC32 CRC_Computed = new CRC32();
		CRC_Computed.update(Start_Data,0,Start_Data.length - 4);
		byte crc_comp_buf[] = new byte[4];
		ByteBuffer.wrap(crc_comp_buf).putInt((int)CRC_Computed.getValue());
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		bos.write(crc_comp_buf, 0, 4);
		crc_comp_buf = bos.toByteArray();
                
		if(!Arrays.equals(crc_rec_buf,crc_comp_buf))
			{
				System.out.println("CRC stimmen nicht überein. Abbruch der Verbindung");
				return;
			}
			Approved = true;
			
			System.out.println("Verbindungsanfrage bestätigt.");

			Client_Port = Start_Packet.getPort();
			IP_Adress = Start_Packet.getAddress();


			Send_ACK(0);

			transferred_File = new File(File_Name);
		        return;	

	}

	public static void Data_Package()
	{
		if(Tries.compareTo(10)== 0)
		{
			System.out.println("Zu viele Versuche. Abbruch der Verbindung");
			Approved = false;
			return;
		}
		byte Data_Max[] = new byte[MTU];
		DatagramPacket Data_Packet = new DatagramPacket(Data_Max, MTU);

		try 
		{
			Thread.sleep((int) (random.nextDouble() * Delay * 2));
		} catch (InterruptedException e) 
		{
			
		}

		try 
		{
			Server_Socket.receive(Data_Packet);
			System.out.println("Empfange Daten");
		} catch (IOException e) 
		{
			Tries++;
			System.out.println("Fehler beim Empfangen des Datenpakets");
			return;   
		} 
		Tries = 0;
		if(random.nextDouble() < Package_Loss)
	    {
		  return;
	    }


		byte Data[] = new byte[Data_Packet.getLength()];
		try 
		{
		System.arraycopy(Data_Max, 0, Data, 0, Data.length);
		} catch (IndexOutOfBoundsException e) 
		{
			System.out.println("Fehler beim Auswerten der empfangenen Daten");
			return;
		}

		Integer Session_Number_Rec = (((Data[0] & 0xff )) | ((Data[1] & 0xff)>>8));
		if(Session_Number_Rec.compareTo(Session_Number)!=0)
		{
			System.out.println("Sessionnummern stimmen nicht überein. Server: " + Session_Number + " Client: " + Session_Number_Rec);
			return;
		}

		Integer Package_Number_Rec = new Integer(Data[2]);  //Paketnummer in Startpaekt an 3. Stelle
		if(Package_Number_Rec.compareTo(Package_Number) != 0)
		{
			System.out.println("Erwartet: " + Package_Number +" Erhalten: " + Package_Number_Rec);
			System.out.println("Falsche Paketnummer.");
			return;
		}


		if(Data.length > File_Length +3 )
		{
			End_Package(Data);
		}
		else
		{
			try 
			{
				fos.write(Data, 3, Data.length -3 );
			} catch (IOException e) 
			{
				System.out.println("Fehler beim Schreiben");
				return;
			}

			Send_ACK(Package_Number);
			File_Length = File_Length - Data.length -3;    //verringern der File-Größe für Überprüfung der verbleibenden Bytes im File
		}
		}

	public static void End_Package(byte [] Data)
	{
		if(Data.length > 7)   // Wenn Data länger als 7 Bytes ist, sind zusätzlich zu CRC und Session-/Paketnummer noch weitere Informationen enthalten
		{
			try 
			{
				fos.write(Data, 3, Data.length-7);
			} catch (IOException e) 
			{
				System.out.println("Fehler beim Schreiben");
				return;
			}
		}
		try 
		{
			fos.close();
		} catch (Exception e) 
		{
			System.out.println("Fehler beim Schließen des FOS");
			return;
		}

		CRC32 crc_final = new CRC32();
		try 
		{
			CheckedInputStream cis = new CheckedInputStream(new FileInputStream(File_Name), crc_final);
			byte buf[] = new byte[128];
			while(cis.read(buf)!=-1);                        //crc wird automatisch von cis mitgeführt
		} catch (IOException e) 
		{
			System.out.println(File_Name + "nicht gefunden");
		}
		
		ByteBuffer Data_Buffer = ByteBuffer.wrap(Data);
		byte crc_rec_buf[] = new byte[4];
		Data_Buffer.position(Data.length-4);
		Data_Buffer.get(crc_rec_buf, 0, 4);
		
		byte crc_final_buf[] = new byte[8];
		ByteBuffer.wrap(crc_final_buf).putLong(crc_final.getValue());
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		bos.write(crc_final_buf, 4, 4);
		crc_final_buf = bos.toByteArray();

        if(!Arrays.equals(crc_rec_buf,crc_final_buf))
		{
			System.out.println("CRC stimmen nicht überein. Abbruch der Verbindung");
			return;
		}

		Send_ACK(Package_Number);
		System.out.println("Ende");
		Approved = false;
		return;

	}

    public static void main(String args[]) throws Exception
	{
		if(args.length!=1 & args.length != 3 )
		{
			System.out.println("Ungültige Parameter Anzahl. Bitte geben sie entweder nur die Portnummer, oder Portnummer,Paketverlustrate und Verzögerung an. ");
			return;
		} 
		Integer Server_Port = Integer.parseInt(args[0]);
		if(args.length == 3)
		{
			Package_Loss = Double.parseDouble(args[1]);
			Delay = Integer.parseInt(args[2]);
		}
		try 
		{
		Server_Socket = new DatagramSocket(Server_Port);
		Server_Socket.setSoTimeout(5000);
		} catch (SocketException e) {
			System.out.println("Socket konnte nicht gebunden werden. Bitte versuchen sie einen andere Portnummer");
			return;
		}
		while(true)
		{
			Approved = false;
			Start_Package();
			if(Approved && File_Name.length()!= 0)
			{
				try 
				{
				  F = new File((System.getProperty("user.dir"))+"/"+File_Name);
				  fos = new FileOutputStream(F);
				} catch (FileNotFoundException e) 
				{
				    System.out.println("Fehler beim Öffnen des Files " + e);	
				}
			}
			while(Approved)
			{
				Data_Package();
			}
			
		}
	}
}
