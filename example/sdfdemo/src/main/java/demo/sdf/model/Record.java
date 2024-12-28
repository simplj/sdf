package demo.sdf.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public abstract class Record implements Serializable {
    protected final String type;
    protected final String name;
    protected final Map<String, String> attributes;

    private String id;

    public Record(String type, String name) {
        this(type, name, new HashMap<>());
    }
    public Record(String type, String name, Map<String, String> attributes) {
        this.type = type;
        this.name = name;
        this.attributes = attributes;
    }

    public void addAttribute(String key, String value) {
        attributes.put(key, value);
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
