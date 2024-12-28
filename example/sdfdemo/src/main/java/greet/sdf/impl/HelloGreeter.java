package greet.sdf.impl;

import com.simplj.di.annotations.Dependency;
import greet.sdf.Greeter;

@Dependency(id = "hello", isDefault = true)
public class HelloGreeter implements Greeter {
    @Override
    public String greet(String name) {
        return "Hello, " + name + '!';
    }
}
