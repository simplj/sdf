package demo.sdf.helper.impl;

import demo.sdf.helper.IdGenerator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Set;

public class RandomIdGenerator<T extends Number> implements IdGenerator {
    private final Set<T> numbers;

    public RandomIdGenerator(Set<T> numbers) {
        this.numbers = numbers;
    }

    @Override
    public String generateId() {
        return "rand-" + numbers.stream().mapToLong(Number::longValue).sum();
    }
}
