package demo.sdf;

import com.simplj.di.annotations.Bind;
import com.simplj.di.annotations.DependencyProvider;
import demo.sdf.helper.IStore;
import demo.sdf.helper.IdGenerator;
import demo.sdf.helper.impl.*;

import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DependencyProviders {

    @DependencyProvider(id = "fileStore", isDefault = true)
    public IStore getObjectStore(@Bind(id = "objects") String path) {
        return new ObjectStore(path);
    }

    @DependencyProvider(id = "inMemoryStore", profiles = "test")
    public IStore getInMemoryStore() {
        return new InMemoryStore();
    }

    @DependencyProvider(isDefault = true, id = "uuid")
    public IdGenerator getUUIDGenerator() {
        return new UUIDGenerator();
    }

    @DependencyProvider(id = "dateTime")
    public IdGenerator getDateTimeIdGenerator(@Bind(id = "fullDateTime") String pattern) {
        return new DateTimeIdGenerator(pattern);
    }

    @DependencyProvider(id = "random")
    public <T extends Number> RandomIdGenerator<T> getDateTimeIdGenerator(Set<T> numbers) {
        return new RandomIdGenerator<>(numbers);
    }

    @DependencyProvider(singleton = false)
    public Set<Long> randomNumbers() {
        Random r = new Random();
        return IntStream.range(0, 5)
                .mapToLong(x -> r.nextLong())
                .map(l -> Math.abs(l) % 1_000_000)
                .boxed().collect(Collectors.toSet());
    }
}
