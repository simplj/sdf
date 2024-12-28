package greet.sdf.service;

import com.simplj.di.annotations.Dependency;
import com.simplj.di.annotations.DynamicInvocation;
import com.simplj.di.annotations.Implicit;
import greet.sdf.Greeter;

@Dependency
public class GreeterService {
    @DynamicInvocation
    public void greet(@Implicit AuditService audit, @Implicit Greeter greeter, String firstName, String lastName) {
        audit.log("Called greet() with " + firstName + " and " + lastName);
        System.out.println(greeter.greet(firstName + " " + lastName));
    }

    @DynamicInvocation(alias = "lastNameGreet")
    public void greet1(@Implicit AuditService audit, @Implicit Greeter greeter, String firstName, String lastName) {
        audit.log("Called greet1() with " + lastName + " and " + firstName);
        System.out.println(greeter.greet(lastName + " " + firstName));
    }
}
