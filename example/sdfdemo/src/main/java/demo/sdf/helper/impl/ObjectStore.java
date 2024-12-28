package demo.sdf.helper.impl;

import com.simplj.di.annotations.Bind;
import demo.sdf.helper.IStore;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class ObjectStore implements IStore {

    private final String path;

    public ObjectStore(String path) {
        this.path = path;
        File f = new File(path);
        if (!f.exists()) {
            f.mkdirs();
        }
    }

    public void store(String id, String base64) throws IOException {
        Files.write(Paths.get(path, id), base64.getBytes());
    }

    public String retrieve(String id) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(path, id));
        return String.join("", lines);
    }
}
