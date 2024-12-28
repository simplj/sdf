package demo.sdf.helper.impl;

import demo.sdf.helper.IdGenerator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public class DateTimeIdGenerator implements IdGenerator {
    private final DateTimeFormatter formatter;

    public DateTimeIdGenerator(String pattern) {
        formatter = DateTimeFormatter.ofPattern(pattern);
    }

    @Override
    public String generateId() {
        return LocalDateTime.now().format(formatter);
    }
}
