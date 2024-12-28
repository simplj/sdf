package demo.sdf.helper.impl;

import com.simplj.di.annotations.Dependency;
import demo.sdf.helper.IdGenerator;

import java.util.UUID;

public class UUIDGenerator implements IdGenerator {
    @Override
    public String generateId() {
        return UUID.randomUUID().toString();
    }
}
