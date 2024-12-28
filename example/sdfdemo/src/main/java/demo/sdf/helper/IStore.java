package demo.sdf.helper;

import java.io.IOException;

public interface IStore {
    void store(String id, String base64) throws IOException;
    String retrieve(String id) throws IOException;
}
