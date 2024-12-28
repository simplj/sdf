package demo.sdf.service;

import demo.sdf.model.Record;

import java.io.*;

public abstract class DemoService<T extends Record> {
    /**
     * Sets an id to the Record object and creates the object as per implementation.
     * @param obj - Object to be stored
     * @return the associated id
     */
    public abstract String create(T obj) throws IOException;

    /**
     * Fetches a Record object for the given id
     * @param id    - retrieves the Record type for this value
     * @return the Record object if found otherwise null
     */
    public abstract T fetch(String id) throws IOException;

    protected byte[] serialize(Object obj) {
        byte[] arr;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bos)) {
            out.writeObject(obj);
            out.flush();
            arr = bos.toByteArray();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
        return arr;
    }

    protected T deserialize(byte[] bytes, Class<T> clazz) {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        try (ObjectInput in = new ObjectInputStream(bis)) {
            Object obj = in.readObject();
            if (clazz.isAssignableFrom(obj.getClass())) {
                return clazz.cast(obj);
            } else {
                throw new IllegalAccessException("Expected Record class but found " + obj.getClass().getName());
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}
