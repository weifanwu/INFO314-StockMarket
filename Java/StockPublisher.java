import io.nats.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalTime;

/**
 * Take the NATS URL on the command-line.
 */
public class StockPublisher {
    public static void main(String... args) throws Exception {
        String natsURL = "nats://127.0.0.1:4222";
        if (args.length > 0) {
            natsURL = args[0];
        }

        System.console().writer().println("Starting stock publisher....");

        StockMarket sm1 = new StockMarket(StockPublisher::publishMessage, "AMZN", "MSFT", "GOOG", "AAPL", "FB", "NFLX", "TSLA");
        new Thread(sm1).start();
        StockMarket sm2 = new StockMarket(StockPublisher::publishMessage, "ACTV", "BLIZ", "ROVIO", "JPM", "BAC", "V", "MA");
        new Thread(sm2).start();
        StockMarket sm3 = new StockMarket(StockPublisher::publishMessage, "GE", "GMC", "FORD", "IBM", "WMT", "NVDA", "INTC");
        new Thread(sm3).start();
    }

    public synchronized static void publishDebugOutput(String symbol, int adjustment, int price) {
        System.console().writer().printf("PUBLISHING %s: %d -> %f\n", symbol, adjustment, (price / 100.f));
    }
    // When you have the NATS code here to publish a message, put "publishMessage" in
    // the above where "publishDebugOutput" currently is
    public synchronized static void publishMessage(String symbol, int adjustment, int price) {
        LocalTime currentTime = LocalTime.now();
        String url = "nats://127.0.0.1:4222";
        String message = "<message sent=\"" + currentTime + "\"><stock><name>" + symbol + "</name><adjustment>" + adjustment + "</adjustment><adjustedPrice>" + price + "</adjustedPrice></stock></message>";
        try {
            Connection connection = Nats.connect(url);
            connection.publish("company." + symbol, message.getBytes(StandardCharsets.UTF_8));
        } catch(Exception error) {
            error.printStackTrace();
        }
    }
}