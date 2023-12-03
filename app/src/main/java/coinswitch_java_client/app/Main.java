package coinswitch_java_client.app;

import java.util.List;

public class Main{

    public static void main(String[] args) throws Exception {

        String secretKey = System.getenv("COINSWITCH_SECRET");
        String apiKey =  System.getenv("COINSWITCH_API");

        Client client = new Client(apiKey, secretKey);
        Coin first = PortfolioFiltering(client).getFirst();
        System.out.println(first);
    }

    public static List<Coin> PortfolioFiltering(Client client) throws Exception {

        return client.getPortfolio();
        
    }
}