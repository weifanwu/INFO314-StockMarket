import java.io.BufferedWriter;
import java.io.FileWriter;
public class SEC {
    String client;
    String broker;
    String action;
    String time;
    public SEC(String client, String broker, String action, String time) {
        this.client = client;
        this.broker = broker;
        this.action = action;
        this.time = time;
    }
    public void check(int amount) throws Exception {
        StringBuffer result = new StringBuffer();
        if (500000 < amount) {
            result.append("Time: " + this.time + "\n");
            result.append("Client: " + this.client + "\n");
            result.append("Broker: " + this.broker + "\n");
            result.append("Action: " + this.action + "\n");
            result.append("Amount: " + amount + "\n\n");
            String filePath = "suspicions.log";
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true));
            writer.write(result.toString());
            writer.flush();
            writer.close();
        }
    }
}
