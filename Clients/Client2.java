import io.nats.client.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;
import java.util.concurrent.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import org.w3c.dom.Document;
import javax.xml.parsers.*;
import java.io.StringReader;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class Client2 {
    public static void main(String[] args) {
        String natsURL = "nats://127.0.0.1:4222";
        try(Connection nc = Nats.connect(natsURL)) {
            String name = "GOOG";
            String amount = "50";
            String message = "<order><sell symbol=\"" + name + "\" amount=\"" + amount + "\" /></order>";
            Future<Message> incoming = nc.request("Kevin", message.getBytes(StandardCharsets.UTF_8));
            Message msg = incoming.get(5, TimeUnit.SECONDS);
            String response = new String(msg.getData(), StandardCharsets.UTF_8);
            System.out.println("Response received: " + response);
        } catch(Exception error) {

        }
    }
}