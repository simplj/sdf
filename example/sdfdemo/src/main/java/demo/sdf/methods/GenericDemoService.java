package demo.sdf.methods;

import com.simplj.di.annotations.Dependency;
import com.simplj.di.annotations.DynamicInvocation;
import com.simplj.di.annotations.Implicit;
import demo.sdf.helper.IStore;
import demo.sdf.helper.IdGenerator;
import demo.sdf.model.Record;

import java.io.*;
import java.util.Base64;

public class GenericDemoService<T extends Record> {
    public String create(IdGenerator idGenerator, IStore store, T obj) throws IOException {
        byte[] bArr = serialize(obj);
        String id = idGenerator.generateId();
        String b64 = Base64.getEncoder().encodeToString(bArr);

        store.store(id, b64);
        return id;
    }

    public T fetch(IStore store, String id, Class<T> clazz) throws IOException {
        String b64 = store.retrieve(id);
        byte[] bArr = Base64.getDecoder().decode(b64);

        return deserialize(bArr, clazz);
    }

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
