package greet.sdf.impl;

import com.simplj.di.annotations.Dependency;
import greet.sdf.Greeter;

@Dependency(id = "hi")
public class HiGreeter implements Greeter {
    @Override
    public String greet(String name) {
        return "Hi, " + name + '!';
    }
}
