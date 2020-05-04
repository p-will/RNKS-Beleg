# Beleg Dateitransfer

Erstellen Sie ein Programm (client + server) zur Übertragung beliebiger Dateien zwischen zwei Rechnern, basierend auf dem UDP-Protokoll. Das Programm soll mit der Sprache JAVA erstellt werden und im Labor S311 unter Linux lauffähig sein und dort vorgeführt werden. Folgende Punkte sind umzusetzen:

1. **Aufruf des Clients** (Quelle) auf der Konsole mit den Parametern: Zieladresse (IP oder Hostname) + Portnummer  (Bsp.: `client-udp is311p1 3333  test.gif`)
2. **Aufruf des Servers** (Ziel) mit den Parametern: Portnummer (Bsp.: `server-udp 3333`). 
Um die Aufrufe für Client und Server so zu realisieren ist ein kleines Bash-script notwendig (z.B.: `java Clientklasse $1 $2 $3`)
3. Auf dem Zielrechner (Server) ist die Datei unter Verwendung des korrekten Dateinamens im Pfad des Servers abzuspeichern. Ist die Datei bereits vorhanden, soll an den Basisnamen der neuen Datei das Zeichen „1“ angehängt werden. Client und Server sollten auch auf dem selben Rechner im selben Pfad funktionieren.
4. Messen Sie bei der Übertragung die Datenrate und zeigen Sie am Client jede Sekunde den aktuellen Wert und am Ende den Gesamtwert an. Orientieren Sie sich hierzu an  `wget`.
5. Implementieren Sie exakt das im Dokument [Beleg-Protokoll](Beleg-Protokoll.md) vorgegebene Übertragungsprotokoll. Damit soll gewährleistet werden, dass Ihr Programm auch mit einem beliebigen anderen Programm funktioniert, welches dieses Protokoll implementiert.
6. Gehen Sie davon aus, dass im Labor Pakete fehlerhaft übertragen werden können, verloren gehen können oder in ihrer Reihenfolge vertauscht werden können (beide Richtungen!). Implementieren Sie eine  entsprechende Fehlerkorrektur.
7. **Testen** Sie Ihr Programm ausgiebig, hierzu sind Debug-Ausgaben sinnvoll. Entwerfen Sie eine Testumgebung als Bestandteil des Servers, mittels derer Sie eine bestimmte Paketverlustwahrscheinlichkeit und Paketverzögerung für **beide Übertragungsrichtungen** simulieren können. Sinnvollerweise sollten diese Parameter über die Konsole konfigurierbar sein, z.B: `server-udp 3333 0.1 150` für 10% Paketverluste und 150 ms mittlere Verzögerung für **beide** Richtungen. Bei der Vorführung im Labor werden wir einen Netzsimulator nutzen, welcher eine entsprechende Netzqualität simuliert.
8. Bestimmen Sie den theoretisch max. erzielbaren Durchsatz bei 10% Paketverlust und 10 ms Verzögerung mit dem SW-Protokoll und vergleichen diesen mit Ihrem Programm. Begründen Sie die Unterschiede.
9. Dokumentieren Sie die Funktion Ihres Programms unter Nutzung von Latex. Notwendig ist mindestens ein Zustandsdiagramm für Client und Server. Geben Sie Probleme/Limitierungen/Verbesserungsvorschläge für das verwendete Protokoll an.
10. Der Abgabetermin ist auf der Website des Fachs zu finden, die Vorführung der Aufgabe findet dann zu den angekündigten Praktikumszeiten statt. Die Abgabe des Belegs erfolgt als tar-Archiv mit einem vorgegebenen Aufbau, Informationen hierzu werden im Dokument [Beleg-Abgabeformat](Beleg-Abgabeformat.md) bereitgestellt. **Plagiate werden mit Note 5** bewertet!
11. **Optional**:
    * Umsetzung des Clients in C und Nachweis der Funktionsfähigkeit durch Datenübertragung zum Java-Server.
    * Bei Interesse können Sie zusätzlich das GbN- oder Selective-Repeat-Protokoll implementieren.
    * Bei Interesse können Sie einzelne Klassen oder den Beleg auch in Kotlin programmieren.
