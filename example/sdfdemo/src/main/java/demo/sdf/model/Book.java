package demo.sdf.model;

import java.util.HashMap;
import java.util.Map;

public class Book extends Record {

    public Book(String name) {
        this(name, new HashMap<>());
    }
    public Book(String name, Map<String, String> attributes) {
        super("Book", name, attributes);
    }

    @Override
    public String toString() {
        return "Book: " +
                "name='" + name + '\'' +
                "\n\tattributes=" + attributes +
                '}';
    }
}
