import io.nats.client.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.time.LocalTime;
import java.util.concurrent.*;
import java.io.BufferedWriter;
import java.io.FileWriter;
import org.w3c.dom.Document;
import javax.print.Doc;
import javax.security.auth.callback.ChoiceCallback;
import javax.swing.Action;
import javax.xml.crypto.dsig.keyinfo.RetrievalMethod;
import javax.xml.parsers.*;
import java.io.StringReader;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class Client1 {
    public static void main(String[] args) throws Exception {
        Map<String, Integer> recording = new HashMap<>();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Map<String, int[]> rules = new HashMap<>();
        initalizeStrategy(recording, builder, "portfolio1.xml");
        initalizePortfolio(rules, builder, "strategy1.xml");
        Connection connection = Nats.connect("nats://127.0.0.1:4222");
        Dispatcher dispatcher = connection.createDispatcher((message) -> {
            try {
                String mes = new String(message.getData(), StandardCharsets.UTF_8);
                Document doc = builder.parse(new InputSource(new StringReader(mes)));
                NodeList elementName = doc.getElementsByTagName("name");
                Node name = elementName.item(0);
                String symbol = name.getTextContent();
                NodeList elementAdjustedPrice = doc.getElementsByTagName("adjustedPrice");
                Node adjustedPrice = elementAdjustedPrice.item(0);
                int price = Integer.parseInt(adjustedPrice.getTextContent());
                Checking check = check(recording, rules, symbol, price);
                if (check.checkAction()) {
                    order(connection, symbol, check.getAction(), check.getAmount());
                    if (check.getAction().equals("buy")) {
                        recording.put(symbol, recording.get(symbol) + check.getAmount());
                    } else {
                        recording.put(symbol, recording.get(symbol) - check.getAmount());
                    }
                    update(recording);
                }
            } catch(Exception error) {
                error.printStackTrace();
            }
        });
        dispatcher.subscribe("company.*");
    }

    public static void initalizePortfolio(Map<String, Integer> recording, DocumentBuilder builder, String name) throws Exception {
        Document document = builder.parse(name);
        NodeList nodes = document.getElementsByTagName("stock");
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            Element nodeElement = (Element) node;
            recording.put(nodeElement.getAttribute("symbol"), Integer.parseInt(node.getTextContent()));
        }
    }

    public static void initalizeStrategy(Map<String, int[]> rules, DocumentBuilder builder, String file) throws Exception {
        Document strategy = builder.parse(file);
        NodeList strategies = strategy.getElementsByTagName("when");
        for (int i = 0; i < strategies.getLength(); i++) {
            int[] information = new int[4];
            Element rule = (Element) strategies.item(i);
            NodeList stock = rule.getElementsByTagName("stock");
            String name = stock.item(0).getTextContent();
            NodeList above = rule.getElementsByTagName("above");
            // index at 0 represents above price, if don't have limit set it to be -1
            if (above.getLength() == 0) {
                information[0] = -1;
            } else {
                information[0] = Integer.parseInt(above.item(0).getTextContent());
            }
            NodeList below = rule.getElementsByTagName("below");
            // index at 1 represents below price, if don't have limit set it to be -1
            if (below.getLength() == 0) {
                information[1] = -1;
            } else {
                information[1] = Integer.parseInt(below.item(0).getTextContent());
            }
            NodeList buy = rule.getElementsByTagName("buy");
            // index at 2 represents buy amount, if not buying anything set it to be -1
            if (buy.getLength() == 0) {
                information[2] = -1;
            } else {
                information[2] = Integer.parseInt(buy.item(0).getTextContent());
            }
            NodeList sell = rule.getElementsByTagName("sell");
            // index at 2 represents sell amount, if sell all set it to -1
            if (sell.getLength() == 0) {
                information[2] = 0;
            } else if (sell.item(0).getTextContent().isEmpty()) {
                information[2] = -1;
            } else {
                information[2] = Integer.parseInt(sell.item(0).getTextContent());
            }
            rules.put(name, information);
        }
    }

    public static void update(Map<String, Integer> recording) throws Exception {     
        StringBuilder result = new StringBuilder();
        result.append("<portfolio>\n");
        for (Map.Entry<String, Integer> pairs : recording.entrySet()) {
            result.append("    <stock symbol=\"" + pairs.getKey() + "\">" + pairs.getValue() + "</stock>\n");            
        }
        result.append("</portfolio>");
        String filePath = "portfolio1.xml";
        BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, false));
        writer.write(result.toString());
        writer.flush();
        writer.close();
    }

    public static Checking check(Map<String, Integer> recording, Map<String, int[]> rules, String symbol, int price) {
        if (recording.get(symbol) == 0) {
            return new Checking(symbol, false, 0);
        }
        if (!rules.containsKey(symbol)) {
            return new Checking(symbol, false, 0);
        }
        int[] information = rules.get(symbol);
        if (information[0] != -1) {
            if (information[0] >= price) {
                return new Checking("nothing", false, 0);
            }
        }
        if (information[1] != -1) {
            if (information[1] <= price) {
                return new Checking("nothing", false, 0);
            }
        }
        int money;
        String action;
        if (information[2] != -1) {
            money = information[2];
            action = "buy";
            return new Checking(action, true, money);
        } else {
            if (information[3] == 0) {
                money = recording.get(symbol);
                action = "sell";
            } else {
                money = information[3];
                action = "sell";
            }
        }
        Checking check = new Checking(action, true, money);
        return check;
    }

    public static void order(Connection connection, String symbol, String action, int amount) throws Exception {
        String message = "<order><" + action + " symbol=\"" + symbol + "\" amount=\"" + amount + "\" /></order>";
        Future<Message> incoming = connection.request("alex", message.getBytes(StandardCharsets.UTF_8));
        Message msg = incoming.get(8, TimeUnit.SECONDS);
        String response = new String(msg.getData(), StandardCharsets.UTF_8);
        System.out.println("Response received: " + response);
    }

    public static class Checking {
        String action;
        boolean check;
        int amount;
        public Checking(String action, boolean check, int amount) {
            this.action = action;
            this.check = check;
            this.amount = amount;
        }
        public String getAction()  {
            return this.action;
        }

        public boolean checkAction() {
            return this.check;
        }

        public int getAmount() {
            return this.amount;
        }
    }
}