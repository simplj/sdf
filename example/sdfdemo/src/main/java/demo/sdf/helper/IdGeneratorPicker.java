package demo.sdf.helper;

import com.simplj.di.annotations.Dependency;
import com.simplj.di.annotations.Realtime;
import com.simplj.di.annotations.SubTypes;

import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Dependency
public class IdGeneratorPicker {
    private final Supplier<Integer> idxSupplier;
    private final List<IdGenerator> generators;

    public IdGeneratorPicker(@SubTypes List<IdGenerator> generators, @Realtime(key = "idxSupplier") Supplier<Integer> idxSupplier) {
        this.idxSupplier = idxSupplier;
        this.generators = generators;
        System.out.println("Resolved IdGenerators:\n\t" + generators.stream().map(g -> g.getClass().getName()).collect(Collectors.joining("\n\t")));
    }

    public IdGenerator pickIdGenerator() {
        int idx = Math.abs(idxSupplier.get()) % generators.size();
        System.out.println("Picking IdGenerator from index: " + idx);
        return generators.get(idx);
    }
}
