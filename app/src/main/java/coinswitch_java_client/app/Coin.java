package coinswitch_java_client.app;

public class Coin {

    String currency;
    Double blocked_balance_order;
    Double main_balance;
    Double buy_average_price;
    Double invested_value;
    Double invested_value_excluding_fee;
    Double current_value;
    Double sell_rate;
    Double buy_rate;
    Boolean is_average_price_available;
    String name;
    Boolean is_delisted_coin;
    Double net = 0.0;
    Double percentage = 0.0;

    @Override
    public String toString(){
        return this.name + " " + this.currency + " " + this.current_value + " " + net + " " + percentage;
    }

    public void calculateNet(){
        net = current_value - invested_value;
    }

    public void calculateAll(){
        calculateNet();
        percentage = (net/invested_value) * 100;
    }
}
