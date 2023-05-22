import io.nats.client.*;
import java.nio.charset.StandardCharsets;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import org.w3c.dom.Document;
import javax.xml.parsers.*;
import java.io.StringReader;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class ConsumerListenner {
    public static void main(String[] args) throws Exception, ParserConfigurationException {
        String url = "nats://127.0.0.1:4222";
        Connection connection = Nats.connect(url);
        Dispatcher dispatcher = connection.createDispatcher((message) -> {
            try {
                String mes = new String(message.getData(), StandardCharsets.UTF_8);
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new InputSource(new StringReader(mes)));
                NodeList elementMessage = doc.getElementsByTagName("message");
                NodeList elementName = doc.getElementsByTagName("name");
                NodeList elementAdjustment = doc.getElementsByTagName("adjustment");
                NodeList elementAdjustedPrice = doc.getElementsByTagName("adjustedPrice");
                Element sent = (Element) elementMessage.item(0);
                Node name = elementName.item(0);
                Node adjustment = elementAdjustment.item(0);
                Node adjustedPrice = elementAdjustedPrice.item(0);
                String filePath = name.getTextContent() + "-price.log";
                String textToWriter = "timestamp: " + sent.getAttribute("sent") + "\n" + "adjustment: " + adjustment.getTextContent() + "\n" + "adjustedPrice: " + adjustedPrice.getTextContent() + "\n\n";
                BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true));
                writer.write(textToWriter);
                writer.flush();
            } catch(Exception error) {
                error.printStackTrace();
            }
        });
        if (args.length == 0) {
            dispatcher.subscribe("company.*");
        } else {
            for (String symbol : args) {
                dispatcher.subscribe(symbol);
            }
        }
    }
}