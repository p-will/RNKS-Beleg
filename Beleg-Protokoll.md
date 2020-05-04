# Protokoll Dateitransfer

Nachfolgend ist das Protokoll zum Beleg Dateitransfer beschrieben. Implementieren Sie dieses Protokoll exakt. Zur Belegabnahme muss der von ihnen erstellte Client zu einem beliebigen nach diesem Protokoll erstellten Server korrekt eine Datei übertragen können.

## Protokollbeschreibung
* Nutzung des ersten Paketes für die Übertragung von Dateiinformationen
* Der Server erkennt einen neuen Übertragungswunsch durch eine neue Sessionnummer und die Kennung „Start“.
* Der Client wählt eine Sessionnummer per Zufall.
* Übertragungsprotokoll: 
  * Stop-and-Wait-Protokoll
  * Bei Absendung eines Paketes wird ein Timeout [[1]](#hinweise) gestartet, welcher mit korrekter Bestätigung durch den Empfänger zurückgesetzt wird.
  * Bei Auslösung des Timeouts wird das Paket erneut gesendet. Dies wird maximal 10 mal wiederholt. Danach erfolgt ein Programmabbruch mit einer Fehlermeldung. 
  * Beachten Sie die Vorgehensweise des Protokolls bzgl. verlorener Daten / ACKs etc.
* Network-Byte-Order:  Big-Endian-Format [[2]](#hinweise)
* Die Länge eines Datagrams [[3]](#hinweise) sei beliebig innerhalb des UDP-Standards, eine sinnvolle Länge ergibt sich aus der MTU des genutzten Netzes
* CRC32-Polynom (IEEE-Standard) [[4]](#hinweise): 0x04C11DB7 für die Fehlererkennung im Startpaket und in der Gesamtdatei. 




## Paketaufbau

### Startpaket (Client -> Server)
* 16-Bit Sessionnummer (Wahl per Zufallsgenerator)
* 8-Bit Paketnummer (immer 0)
* 5-Byte Kennung „Start“  als ASCII-Zeichen
* 64-Bit Dateilänge (unsigned integer) (für Dateien > 4 GB)
* 2 Byte (unsigned integer) Länge des Dateinamens  
* 0-255 Byte Dateiname als String mit Codierung UTF-8 [[5]](#hinweise)
* 32-Bit-CRC über alle Daten des Startpaketes

### Datenpakete (Client -> Server)
* 16-Bit-Sessionnummer
* 8-Bit Paketnummer ( 1. Datenpaket hat die Nr. 1, gerechnet wird mod 2, also nur 0 und 1)
* Daten 
* 32-Bit-CRC (nur im letzten Paket vorhanden) Berechnung über Gesamtdatei, die CRC darf nicht auf mehrere UDP-Pakete aufgeteilt werden

### Bestätigungspakete (Server -> Client)
* 16-Bit-Sessionnummer
* 8-Bit Bestätigungsnummer für das zu bestätigende Paket  (ACK 0 → Paket Nr. 0 bestätigt)  
Bei einem CRC- Fehler  soll kein ACK gesendet werden



## Hinweise

1. Sinnvoll ist eine gleitende Anpassung des Timeouts an der Übertragungskanal um den Datendurchsatz bei Paketwiederholungen zu erhöhen, Berechnung siehe z.B. TCP-Protokoll

2. In Java u.U. Klasse java.nio.ByteBuffer nutzbar

3. Die Länge des Datenfeldes kann über die abfragbare UDP-Paketlänge ermittelt werden.

4. Implementierungsdetails für CRC32: gespiegeltes Polynom, Initialisierung des Registers mit 0xffffffff, Berechnung des Endwerts XOR 0xffffffff  
Test:  Codierung der ASCII-Folge 123456789  muss die CRC cbf43926  ergeben  
Siehe dazu auch http://introcs.cs.princeton.edu/java/51data/CRC32.java.html

5. Für Stringhandling in Java siehe z.B. Klassen DataInput.readUTF  bzw. DataInputStream.readUTF
Diese Klassen weichen zwar in drei Punkten vom UTF-8-Standard ab, welche aber für die Belegaufgabe unkritisch seien sollten, siehe:
 http://docs.oracle.com/javase/6/docs/api/java/io/DataInput.html#modified-utf-8

6. Nützlich sind die Javaklassen: ByteArrayInputStream, CheckedInputStream, DataInputStream

7. Für Informationen zu Java-Bitmanipulationen siehe Vorlesungsfolien oder z.B: http://sys.cs.rice.edu/course/comp314/10/p2/javabits.html


8. Whether a value in an int is signed or unsigned depends on how the bits are interpreted - java interprets bits as a signed value (doesn't have unsigned primitives).
If you have an int that you want to interpret as an unsigned value (e.g. you read an int from a DataInputStream that you know contains an unsigned value) then you can do the following trick.  

`int fourBytesIJustRead = someObject.getInt();`  
`long unsignedValue = fourBytesIJustRead & 0xffffffffl;`
