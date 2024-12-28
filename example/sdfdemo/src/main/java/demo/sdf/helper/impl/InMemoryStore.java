package demo.sdf.helper.impl;

import demo.sdf.helper.IStore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryStore implements IStore {

    private final Map<String, String> store;

    public InMemoryStore() {
        this.store = new ConcurrentHashMap<>();
    }

    public void store(String id, String base64) {
        System.out.println("Stored in InMemory for id: " + id);
        store.put(id, base64);
    }

    public String retrieve(String id) {
        System.out.println("Retrieving from InMemory for id: " + id);
        return store.get(id);
    }
}
