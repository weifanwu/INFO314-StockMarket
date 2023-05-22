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
import java.io.BufferedReader;
import org.xml.sax.InputSource;
import java.io.FileReader;


public class Alex {
    public static void main(String[] args) {
        String natsURL = "nats://127.0.0.1:4222";
        try {
            Connection nc = Nats.connect(natsURL);
            Dispatcher dispatcher = nc.createDispatcher((msg) -> {
                try {
                    String message = new String(msg.getData(), StandardCharsets.UTF_8);
                    DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    DocumentBuilder builder = factory.newDocumentBuilder();
                    Document doc = builder.parse(new InputSource(new StringReader(message)));
                    NodeList order = doc.getElementsByTagName("buy");
                    String name = "buy";
                    if (order.getLength() == 0) {
                        order = doc.getElementsByTagName("sell");
                        name = "sell";
                    }
                    Element element = (Element) order.item(0);
                    int amount = Integer.parseInt(element.getAttribute("amount"));
                    String client = element.getAttribute("client");
                    BufferedReader reader = new BufferedReader(new FileReader("../Consumer/" + element.getAttribute("symbol") + "-price.log"));
                    String line;
                    String lastLine = null;
                    // Read the file line by line until reaching the end
                    while ((line = reader.readLine()) != null && !line.isEmpty()) {
                        lastLine = line;
                    }
                    // Print the last line
                    if (lastLine != null) {
                        double price = Integer.parseInt(lastLine.split(" ")[1]) * amount;
                        double deducation = Integer.parseInt(lastLine.split(" ")[1]) * amount * 0.1;
                        double total;
                        if (name.equals("sell")) {
                            total = price - deducation;
                        } else {
                            total = price + deducation;
                        }
                        SEC security = new SEC(client, "Alex", name, LocalTime.now().toString());
                        security.check((int) total);
                        String response = "<orderReceipt><" + name + " symbol=\"" + element.getAttribute("symbol") + "\" amount=\"" + amount + "\" /><complete amount=\"" + total + "\" /></orderReceipt>";
                        nc.publish(msg.getReplyTo(), response.getBytes());
                    } else {
                        System.out.println("The file is empty."); // Handle case when the file is empty
                    }
                } catch(Exception error) {
                    error.printStackTrace();
                }            });
            dispatcher.subscribe("alex");
        } catch(Exception errorException) {
            errorException.printStackTrace();
        }
    }
}