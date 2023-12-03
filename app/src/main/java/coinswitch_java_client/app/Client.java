package coinswitch_java_client.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.bouncycastle.crypto.signers.Ed25519Signer;
import org.bouncycastle.util.encoders.Hex;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.*;


public class Client {
    String apiKey, secretKey;
    String exchange = "coinswitchx";
    Client(String apiKey, String secretKey){
        this.apiKey = apiKey;
        this.secretKey = secretKey;
    }
    Client(String apiKey, String secretKey, String exchange){
        this.apiKey = apiKey;
        this.secretKey = secretKey;
        this.exchange = exchange;
    }
    public String ValidateKeys() throws Exception {
        HashMap<String, String> parameters = new HashMap<>();
        HashMap<String, Object> payload = new HashMap<>();

        return this.makeRequest("GET", "/trade/api/v2/validate/keys", payload, parameters);
    }

    public String ping() throws Exception {
        HashMap<String, String> parameters = new HashMap<>();
        HashMap<String, Object> payload = new HashMap<>();
        return this.makeRequest("GET", "/trade/api/v2/ping", payload, parameters);
    }

    public String makeRequest(String method, String endpoint, HashMap<String, Object> payload, HashMap<String, String> params) throws Exception {


        String decodedEndpoint = endpoint;
        if (method.equals("GET") && !params.isEmpty()) {
            String query = new URI(endpoint).getQuery();
            endpoint += (query == null || query.isEmpty()) ? "?" : "&";
            endpoint += URLEncoder.encode(paramsToString(params), "UTF-8");
            decodedEndpoint = URLDecoder.decode(endpoint, "UTF-8");
        }

        String signatureMsg = signatureMessage(method, decodedEndpoint, payload);
        String signature = getSignatureOfRequest(secretKey, signatureMsg);

        Map<String, String> headers = new HashMap<>();
        String url = "https://coinswitch.co" + endpoint;

        headers.put("X-AUTH-SIGNATURE", signature);
        headers.put("X-AUTH-APIKEY", apiKey);
        if (method.equals("GET")){
            HttpResponse<String> response = callGetApi(url, headers, payload);
            return response.body().toString();
        }
        if (method.equals("POST") || method.equals("PUT") || method.equals("DELETE")){
            ObjectMapper objectMapper = new ObjectMapper();
            String json = objectMapper.writeValueAsString(payload);
            URL apiUrl = new URL(url);
            HttpURLConnection connection = (HttpURLConnection) apiUrl.openConnection();

            // Set the request method to POST
            connection.setRequestMethod(method);
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("X-AUTH-SIGNATURE", signature);
            connection.setRequestProperty("X-AUTH-APIKEY", apiKey);
            // Enable output and set the request body
            connection.setDoOutput(true);
            connection.getOutputStream().write(json.getBytes());

            // Get the response code
            int responseCode = connection.getResponseCode();

            // Read the response
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();

            // Close the connection
            connection.disconnect();
            return  response.toString();
        };
        return "SUCCESS";
    }

    private String paramsToString(Map<String, String> params) throws Exception {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (sb.length() > 0) {
                sb.append("&");
            }
            sb.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
            sb.append("=");
            sb.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
        }
        return sb.toString();
    }

    private String signatureMessage(String method, String endpoint, HashMap<String, Object> payload) throws Exception {
        TreeMap<String, Object> treeMap = new TreeMap<>(payload);
        String sortedPayloadJson = new ObjectMapper().writeValueAsString(treeMap);
        return method + endpoint + sortedPayloadJson;
    }

    public String getSignatureOfRequest(String secretKey, String requestString) {
        byte[] requestBytes = requestString.getBytes(StandardCharsets.UTF_8);
        byte[] secretKeyBytes = Hex.decode(secretKey);

        // Generate private key
        Ed25519PrivateKeyParameters privateKey = new Ed25519PrivateKeyParameters(secretKeyBytes, 0);

        // Sign the request
        Ed25519Signer signer = new Ed25519Signer();
        signer.init(true, privateKey);
        signer.update(requestBytes, 0, requestBytes.length);
        byte[] signatureBytes = signer.generateSignature();

        String signatureHex = Hex.toHexString(signatureBytes);
        return signatureHex;
    }
    private HttpResponse<String> callGetApi(String url, Map<String, String> headers, HashMap<String, Object> payload) throws Exception {
        HttpClient client = HttpClient.newBuilder().build();

        HttpRequest.Builder requestBuilder;

        requestBuilder = HttpRequest.newBuilder()
                .GET()
                .uri(new URI(url));


        for (Map.Entry<String, String> entry : headers.entrySet()) {
            requestBuilder.header(entry.getKey(), entry.getValue());
        }

        HttpRequest request = requestBuilder.build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    public ArrayList<String> getCoins() throws Exception{

        HashMap<String, String> parameters = new HashMap<>();
        HashMap<String, Object> payload = new HashMap<>();

        String path = "?&exchange=" + URLEncoder.encode(exchange, "UTF-8");
        path = path.replaceAll("%2C", ",");
        path = path.replaceAll("%2F", "/");

        String response = this.makeRequest("GET", "/trade/api/v2/coins" + path, payload, parameters);
        Gson gson = new Gson();
        JsonObject json = gson.fromJson(response, JsonObject.class);


        JsonObject data = json.get("data").getAsJsonObject();
        JsonArray coinswitchx = data.get(exchange).getAsJsonArray();

        ArrayList<String> coins = new Gson().fromJson(coinswitchx, new TypeToken<ArrayList<String>>() {}.getType());
        return coins;

    }

    public ArrayList<Coin> getPortfolio() throws Exception {
        HashMap<String, String> parameters = new HashMap<>();
        HashMap<String, Object> payload = new HashMap<>();

        String response =  this.makeRequest("GET", "/trade/api/v2/user/portfolio", payload, parameters);

        Gson gson = new Gson();
        JsonObject json = gson.fromJson(response, JsonObject.class);
        JsonArray array = json.get("data").getAsJsonArray();
        ArrayList<Coin> coins = new Gson().fromJson(array, new TypeToken<ArrayList<Coin>>(){}.getType());

        return coins;

    }

    public ArrayList<CandleStick> getCandleStickData(Long start, Long end, Integer interval, String symbol) throws Exception {
        HashMap<String, String> parameters = new HashMap<>();
        HashMap<String, Object> payload = new HashMap<>();
        String path = "?end_time=" + URLEncoder.encode(end.toString(), "UTF-8")
                + "&start_time=" + URLEncoder.encode(start.toString(), "UTF-8")
                + "&symbol=" + URLEncoder.encode(symbol, "UTF-8")+
                "&interval=" + URLEncoder.encode(interval.toString(), "UTF-8")+
                "&exchange=" + URLEncoder.encode(exchange, "UTF-8")
                ;
        path = path.replaceAll("%2F", "/");
        String response = this.makeRequest("GET","/trade/api/v2/candles"+path, payload, parameters);

        Gson gson = new Gson();
        JsonObject json = gson.fromJson(response, JsonObject.class);
        JsonArray array = json.get("data").getAsJsonArray();
        ArrayList<CandleStick> candleSticks = new Gson().fromJson(array, new TypeToken<ArrayList<CandleStick>>(){}.getType());
        return new ArrayList<>(candleSticks);

    }



}
