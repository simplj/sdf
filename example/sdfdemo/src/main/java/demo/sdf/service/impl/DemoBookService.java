package demo.sdf.service.impl;

import com.simplj.di.annotations.Bind;
import com.simplj.di.annotations.Dependency;
import demo.sdf.helper.IStore;
import demo.sdf.helper.IdGenerator;
import demo.sdf.model.Book;
import demo.sdf.model.Record;
import demo.sdf.service.DemoService;

import java.io.*;
import java.util.Base64;

@Dependency
public class DemoBookService extends DemoService<Book> {
    private final IdGenerator idGenerator;
    private final IStore store;

    public DemoBookService(@Bind(id = "${id.generator}") IdGenerator idGenerator, IStore store) {
        this.idGenerator = idGenerator;
        this.store = store;
    }

    @Override
    public String create(Book obj) throws IOException {
        byte[] bArr = serialize(obj);
        String id = idGenerator.generateId();
        String b64 = Base64.getEncoder().encodeToString(bArr);

        store.store(id, b64);
        return id;
    }

    @Override
    public Book fetch(String id) throws IOException {
        String b64 = store.retrieve(id);
        byte[] bArr = Base64.getDecoder().decode(b64);

        return deserialize(bArr, Book.class);
    }
}
