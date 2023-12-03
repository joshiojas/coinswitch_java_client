package coinswitch_java_client.app;

public class CandleStick {

    public Double o;
    public Double h;
    public Double l;
    public Double c;
    String symbol;
    Long close_time;
    Double volume;
    Long start_time;
    Integer interval;

    @Override
    public String toString(){
        return "Symbol: " + symbol + "\t Close: " + c;
    }

}
