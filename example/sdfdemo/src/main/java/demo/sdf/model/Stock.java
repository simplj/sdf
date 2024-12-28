package demo.sdf.model;

import java.util.HashMap;
import java.util.Map;

public class Stock extends Record {

    public Stock(String name) {
        this(name, new HashMap<>());
    }
    public Stock(String name, Map<String, String> attributes) {
        super("Stock", name, attributes);
    }

    @Override
    public String toString() {
        return "Stock: " +
                "name='" + name + '\'' +
                "\n\tattributes=" + attributes +
                '}';
    }
}
