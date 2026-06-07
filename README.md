# SDF – Simple Dependency Injection Framework [![Maven Central](https://img.shields.io/maven-central/v/com.simplj.di/sdf.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22com.simplj.di%22%20AND%20a:%22sdf%22) [![javadoc](https://javadoc.io/badge2/com.simplj.di/sdf/javadoc.svg)](https://javadoc.io/doc/com.simplj.di/sdf)

## Table of Contents

1. [Overview](#overview)
2. [Installation & Setup](#installation--setup)
3. [Quick Start](#quick-start)
4. [Defining Dependencies with `@Dependency`](#defining-dependencies-with-dependency)
5. [Providing Dependencies with `@DependencyProvider`](#providing-dependencies-with-dependencyprovider)
6. [Providing Constants](#providing-constants)
7. [Binding Parameters with `@Bind`](#binding-parameters-with-bind)
8. [Injecting Subtypes with `@SubTypes`](#injecting-subtypes-with-subtypes)
9. [Runtime Dependencies with `@Realtime`](#runtime-dependencies-with-realtime)
10. [Tagged Dependencies](#tagged-dependencies)
11. [Profile-Based Dependency Resolution](#profile-based-dependency-resolution)
12. [Contextual Dependency Resolution](#contextual-dependency-resolution)
13. [Generic Type Dependencies](#generic-type-dependencies)
14. [Dynamic Method Invocation](#dynamic-method-invocation)
15. [Implicit Type Conversions with `TypeConversions`](#implicit-type-conversions-with-typeconversions)
16. [`DependencyResolver` API](#dependencyresolver-api)
17. [`DependencyResolverFactory` API](#dependencyresolverfactory-api)
18. [`DependencyResolverConfig.Builder` Reference](#dependencyresolverconfig-builder-reference)
19. [`TypeClass<T>` – Generic Type Token](#typeclasst--generic-type-token)
20. [`DynamicMethod` and `Argument` – Introspection](#dynamicmethod-and-argument--introspection)
21. [Error Handling & Exceptions](#error-handling--exceptions)
22. [Configuration Reference](#configuration-reference)
23. [Thread Safety & Concurrency Notes](#thread-safety--concurrency-notes)
24. [Type Substitutions](#type-substitutions)
25. [Constraints & Restrictions](#constraints--restrictions)
26. [FAQ / Troubleshooting](#faq--troubleshooting)

---

## Overview

SDF (**S**imple **D**ependency injection **F**ramework) is a lightweight, zero-dependency Java library that manages object creation and wiring automatically. You annotate your classes, configure the framework once at startup, and then request fully assembled instances from a central `DependencyResolver`.

**Design philosophy**

- **Simple** – a single annotation (`@Dependency`) is all you need for the common case.
- **Lightweight** – one jar, no transitive dependencies.
- **Powerful** – supports generics, wildcards, type variables, profiles, multiple contexts, tagged dependencies, runtime parameters, and dynamic method dispatch.

**When to use SDF**

Use SDF when you want the productivity of dependency injection without pulling in a heavyweight container. It is particularly suited to standalone Java applications, microservices, and libraries that cannot afford a large classpath.

---

## Installation & Setup

**Minimum Java version:** Java 8

**Maven**

```xml
<dependency>
    <groupId>com.simplj.di</groupId>
    <artifactId>sdf</artifactId>
    <version>2.3</version>
</dependency>
```

**Gradle**

```groovy
implementation 'com.simplj.di:sdf:2.3'
```

No additional configuration files are required. All wiring is driven by annotations and a single builder call at startup.

---

## Quick Start

This end-to-end example shows how to wire two classes together and resolve an instance in under five minutes.

```java
import com.simplj.di.annotations.Dependency;
import com.simplj.di.core.DependencyResolver;
import com.simplj.di.core.DependencyResolverConfig;
import com.simplj.di.core.DependencyResolverFactory;

// Step 1 – annotate your classes
@Dependency
public class GreetingService {
    public String greet(String name) {
        return "Hello, " + name + "!";
    }
}

@Dependency
public class AppRunner {
    private final GreetingService greetingService;

    // SDF injects GreetingService via the constructor
    public AppRunner(GreetingService greetingService) {
        this.greetingService = greetingService;
    }

    public void run() {
        System.out.println(greetingService.greet("World"));
    }
}

// Step 2 – bootstrap once at application startup
public class Main {
    public static void main(String[] args) {
        DependencyResolverFactory.configureDefaultResolver(
            DependencyResolverConfig.builder()
                .setBasePackages("com.example.myapp")  // root package(s) containing @Dependency classes
                .build()
        );

        // Step 3 – resolve and use
        DependencyResolver resolver = DependencyResolverFactory.defaultResolver();
        AppRunner runner = resolver.resolve(AppRunner.class);
        runner.run();  // prints "Hello, World!"
    }
}
```

Both `GreetingService` and `AppRunner` are singletons by default. Call `resolver.resolve(AppRunner.class)` as many times as you like – you always get the same instance.

---

## Defining Dependencies with `@Dependency`

### What it does

`@Dependency` tells SDF to manage the lifecycle of a class. SDF scans the packages you specify, discovers every `@Dependency` class, resolves its constructor or factory-method arguments from the already-registered dependency pool, and stores the result (singleton by default).

### Annotation reference

```java
package com.simplj.di.annotations;

@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Dependency {
    String   id()         default "";     // optional unique id
    String   initMethod() default "";     // static factory method name (if not using constructor)
    String[] tags()       default {};     // logical grouping tags
    String[] profiles()   default {};     // active-profile filter
    boolean  singleton()  default true;   // false = new instance per resolve
    boolean  isDefault()  default false;  // true = preferred when multiple impls exist
}
```

| Field | Type | Default | Purpose |
|-------|------|---------|---------|
| `id` | `String` | `""` | Unique name for `@Bind` or `resolver.resolve(id, Type.class)` |
| `initMethod` | `String` | `""` | Name of a `static` factory method on the same class |
| `tags` | `String[]` | `{}` | Tag names for grouped resolution (see [Tagged Dependencies](#tagged-dependencies)) |
| `profiles` | `String[]` | `{}` | Load only when the active profile matches (see [Profile-Based Dependency Resolution](#profile-based-dependency-resolution)) |
| `singleton` | `boolean` | `true` | `false` creates a new instance on every resolve |
| `isDefault` | `boolean` | `false` | Default implementation when multiple impls share a parent type |

### Via constructor

```java
package com.example.app.service;

import com.simplj.di.annotations.Dependency;

@Dependency
public class EmailService {
    public void send(String to, String body) { /* ... */ }
}

@Dependency
public class NotificationService {
    private final EmailService emailService;

    // SDF resolves EmailService and passes it here
    public NotificationService(EmailService emailService) {
        this.emailService = emailService;
    }
}
```

**Constraint:** exactly one `public` constructor must exist when `initMethod` is not set.

### Via static factory method

```java
import com.simplj.di.annotations.Dependency;

@Dependency(initMethod = "create")
public class DatabasePool {
    private final String url;

    private DatabasePool(String url) {    // private constructor is fine
        this.url = url;
    }

    public static DatabasePool create() {
        return new DatabasePool("jdbc:mysql://localhost/mydb");
    }
}

@Dependency(initMethod = "create")
public class UserRepository {
    private final DatabasePool pool;

    private UserRepository(DatabasePool pool) {
        this.pool = pool;
    }

    // SDF resolves DatabasePool and passes it to the factory method
    public static UserRepository create(DatabasePool pool) {
        return new UserRepository(pool);
    }
}
```

**Constraint:** `initMethod` must be a `static` method declared in the same class.

### Non-singleton example

```java
@Dependency(singleton = false)
public class RequestContext {
    private final long createdAt = System.currentTimeMillis();
    // Each resolver.resolve(RequestContext.class) returns a fresh object
}
```

### Multiple implementations with a default

```java
public interface PaymentGateway { boolean charge(double amount); }

@Dependency(id = "stripe", isDefault = true)
public class StripeGateway implements PaymentGateway { /* ... */ }

@Dependency(id = "paypal")
public class PayPalGateway implements PaymentGateway { /* ... */ }

@Dependency
public class CheckoutService {
    private final PaymentGateway gateway;

    // Receives StripeGateway because isDefault = true
    public CheckoutService(PaymentGateway gateway) {
        this.gateway = gateway;
    }
}
```

**See Also:** [Binding Parameters with `@Bind`](#binding-parameters-with-bind), [Tagged Dependencies](#tagged-dependencies), [Profile-Based Dependency Resolution](#profile-based-dependency-resolution)

---

## Providing Dependencies with `@DependencyProvider`

### What it does

`@DependencyProvider` lets you define dependencies in a plain Java class through regular methods instead of annotating the target class directly. This is useful when:

- The class you want to wire comes from an external library and you cannot add annotations to it.
- You want to centralize all instantiation logic in one or more "module" classes.

You create an instance of the provider class and pass it to `DependencyResolverConfig.Builder.setDependencyProviders()`.

### Annotation reference

```java
package com.simplj.di.annotations;

@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DependencyProvider {
    String   id()        default "";
    String[] tags()      default {};
    String[] profiles()  default {};
    boolean  singleton() default true;
    boolean  isDefault() default false;
}
```

| Field | Type | Default | Purpose |
|-------|------|---------|---------|
| `id` | `String` | `""` | Unique name for `@Bind` or `resolver.resolve(id, Type.class)` |
| `tags` | `String[]` | `{}` | Tag names (see [Tagged Dependencies](#tagged-dependencies)) |
| `profiles` | `String[]` | `{}` | Active-profile filter |
| `singleton` | `boolean` | `true` | `false` creates a new instance on every resolve |
| `isDefault` | `boolean` | `false` | Default implementation for the return type |

### Example

```java
import com.simplj.di.annotations.Bind;
import com.simplj.di.annotations.DependencyProvider;

// Classes from an external library – no @Dependency possible
public class HttpConnection { /* ... */ }
public class HttpClient {
    public HttpClient(HttpConnection conn) { /* ... */ }
}

// Provider class – you own this
public class HttpModule {

    @DependencyProvider
    public HttpConnection httpConnection() {
        return new HttpConnection();
    }

    @DependencyProvider
    public HttpClient httpClient(HttpConnection conn) {
        // SDF resolves HttpConnection and passes it here
        return new HttpClient(conn);
    }
}

// Bootstrap
DependencyResolverFactory.configureDefaultResolver(
    DependencyResolverConfig.builder()
        .setDependencyProviders(new HttpModule())   // pass an INSTANCE, not a Class object
        .build()
);
```

### Multiple providers with the same return type

When two methods return the same type, assign distinct ids:

```java
public class ConnectionModule {

    @DependencyProvider(id = "primary", isDefault = true)
    public DataSource primaryDataSource() {
        return buildDataSource("jdbc:mysql://primary/db");
    }

    @DependencyProvider(id = "replica")
    public DataSource replicaDataSource() {
        return buildDataSource("jdbc:mysql://replica/db");
    }
}
```

**Gotcha:** If a class is annotated with `@Dependency` and also appears as the return type of a `@DependencyProvider` method, SDF throws an ambiguity error at startup.

**See Also:** [`DependencyResolverConfig.Builder` Reference](#dependencyresolverconfig-builder-reference), [Binding Parameters with `@Bind`](#binding-parameters-with-bind)

---

## Providing Constants

Constants are scalar values (strings, numbers, etc.) that SDF tracks by an id rather than by type. Three mechanisms exist for supplying them.

### Using `@Constant`

`@Constant` is a method-level annotation placed inside a provider class. The method's return value becomes the constant.

```java
package com.simplj.di.annotations;

@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Constant {
    String   id();
    String[] tags()     default {};
    String[] profiles() default {};
}
```

| Field | Type | Required | Purpose |
|-------|------|----------|---------|
| `id` | `String` | yes | The key used to look up or bind this constant |
| `tags` | `String[]` | no | Tag names |
| `profiles` | `String[]` | no | Active-profile filter |

```java
import com.simplj.di.annotations.Bind;
import com.simplj.di.annotations.Constant;
import com.simplj.di.annotations.DependencyProvider;

public class AppConfig {

    @Constant(id = "db.url")
    public String dbUrl() {
        return "jdbc:mysql://localhost/app";
    }

    @Constant(id = "db.pool.size")
    public int poolSize() {
        return 10;
    }

    // A constant that depends on another constant
    @Constant(id = "db.backup.path")
    public String backupPath(@Bind(id = "db.url") String url) {
        return "/backup/" + url.hashCode();
    }

    // A dependency that uses a constant
    @DependencyProvider
    public ConnectionPool connectionPool(
            @Bind(id = "db.url")       String url,
            @Bind(id = "db.pool.size") int    size) {
        return new ConnectionPool(url, size);
    }
}

// Bootstrap
DependencyResolverFactory.configureDefaultResolver(
    DependencyResolverConfig.builder()
        .setDependencyProviders(new AppConfig())
        .build()
);

// Resolve a constant directly
DependencyResolver resolver = DependencyResolverFactory.defaultResolver();
String url = resolver.resolve("db.url", String.class);
```

Primitive return types (`int`, `long`, `float`, `double`) and their wrappers are both supported. SDF performs auto-boxing/unboxing as needed.

### Through `java.util.Properties`

```java
Properties props = new Properties();
props.setProperty("server.host", "localhost");
props.setProperty("server.port", "8080");

DependencyResolverFactory.configureDefaultResolver(
    DependencyResolverConfig.builder()
        .setProperties(props)
        .build()
);

// Inject via @Bind
@Dependency
public class WebServer {
    private final String host;
    private final String port;

    public WebServer(
            @Bind(id = "server.host") String host,
            @Bind(id = "server.port") String port) {
        this.host = host;
        this.port = port;
    }
}
```

Property values are always `String`. Use `@Constant` methods if you need typed parsing.

### Through JVM arguments

Pass constants with the prefix `sdf_consts.`:

```
-Dsdf_consts.env.name=production
-Dsdf_consts.feature.flag=true
```

The constant id is the part after the prefix: `env.name`, `feature.flag`.

```java
// Resolved just like any other constant
String env = resolver.resolve("env.name", String.class);
```

**Note:** JVM-argument constants override any constant with the same id supplied through `@Constant` or `Properties`.

**See Also:** [Binding Parameters with `@Bind`](#binding-parameters-with-bind)

---

## Binding Parameters with `@Bind`

### What it does

`@Bind` is a parameter annotation that tells SDF which specific dependency or constant to inject into a constructor or provider-method parameter. Without `@Bind`, SDF resolves a parameter by its type. With `@Bind`, it resolves by id.

### Annotation reference

```java
package com.simplj.di.annotations;

@Documented
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Bind {
    String id();   // the dependency or constant id
}
```

### Static id

```java
public interface Cache { /* ... */ }

@Dependency(id = "local")
public class LocalCache  implements Cache { /* ... */ }

@Dependency(id = "remote", isDefault = true)
public class RemoteCache implements Cache { /* ... */ }

@Dependency
public class ReportService {
    private final Cache local;
    private final Cache remote;

    public ReportService(
            @Bind(id = "local")  Cache local,
            @Bind(id = "remote") Cache remote) {
        this.local  = local;
        this.remote = remote;
    }
}
```

### Variable id (resolved from constants)

Wrap the constant key in `${` and `}`:

```java
@Dependency
public class AdapterRouter {
    private final Cache cache;

    // The constant "preferred.cache" must have value "local" or "remote"
    public AdapterRouter(@Bind(id = "${preferred.cache}") Cache cache) {
        this.cache = cache;
    }
}
```

SDF substitutes `${preferred.cache}` at startup using the constant whose id is `preferred.cache`. The variable constant must be of type `String`.

**See Also:** [Providing Constants](#providing-constants), [Runtime Dependencies with `@Realtime`](#runtime-dependencies-with-realtime)

---

## Injecting Subtypes with `@SubTypes`

### What it does

`@SubTypes` marks a `List<T>` or `Map<String, T>` parameter so that SDF injects every registered implementation of `T` at once. This is the standard "collect all plugins of an interface" pattern.

### Annotation reference

```java
package com.simplj.di.annotations;

@Documented
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface SubTypes {
    boolean emptyIfNotFound() default true;
}
```

| Field | Type | Default | Purpose |
|-------|------|---------|---------|
| `emptyIfNotFound` | `boolean` | `true` | When `true`, an empty collection is injected if no subtypes are found; when `false`, an `SdfException` is thrown |

### List injection

```java
public interface Validator { boolean validate(Object input); }

@Dependency
public class NullValidator   implements Validator { /* ... */ }
@Dependency
public class RangeValidator  implements Validator { /* ... */ }
@Dependency
public class FormatValidator implements Validator { /* ... */ }

@Dependency
public class ValidationPipeline {
    private final List<Validator> validators;

    public ValidationPipeline(@SubTypes List<Validator> validators) {
        this.validators = validators;
        // validators contains NullValidator, RangeValidator, FormatValidator
    }

    public boolean validate(Object input) {
        return validators.stream().allMatch(v -> v.validate(input));
    }
}
```

### Map injection

When `Map<String, T>` is used, the map key is the dependency `id`. Dependencies without an explicit id use the class simple name.

```java
@Dependency
public class EventDispatcher {
    private final Map<String, Validator> validatorMap;

    public EventDispatcher(@SubTypes Map<String, Validator> validatorMap) {
        this.validatorMap = validatorMap;
    }
}
```

### Generic subtype injection

SDF resolves generic subtypes precisely:

```java
public interface Handler<T> { void handle(T event); }

@Dependency
public class StringHandler implements Handler<String>  { /* ... */ }
@Dependency
public class IntegerHandler implements Handler<Integer> { /* ... */ }

@Dependency
public class StringRouter {
    private final List<Handler<String>> handlers;

    // Only StringHandler is injected; IntegerHandler is excluded
    public StringRouter(@SubTypes List<Handler<String>> handlers) {
        this.handlers = handlers;
    }
}
```

**Constraint:** Only `List` and `Map` collection types are supported.

---

## Runtime Dependencies with `@Realtime`

### What it does

`@Realtime` marks a constructor or provider-method parameter whose value is not known at startup – it is supplied at the call site via a `Map<String, Object>`. This lets you create instances that share framework-managed dependencies but vary on a per-request value.

### Annotation reference

```java
package com.simplj.di.annotations;

@Documented
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Realtime {
    String  key();               // Map key used to look up the value at runtime
    boolean nullable() default false;  // true = allow null if key is absent
}
```

| Field | Type | Default | Purpose |
|-------|------|---------|---------|
| `key` | `String` | required | Key in the runtime map passed to `dynamicResolve()` |
| `nullable` | `boolean` | `false` | When `true`, `null` is injected if the key is missing from the map |

The `key` supports the variable notation `${constantId}` just like `@Bind`.

### Example

```java
import com.simplj.di.annotations.Dependency;
import com.simplj.di.annotations.Realtime;

@Dependency(singleton = false)
public class OrderProcessor {
    private final PaymentGateway gateway;  // resolved by SDF
    private final Order order;             // supplied at runtime

    public OrderProcessor(
            PaymentGateway gateway,
            @Realtime(key = "order") Order order) {
        this.gateway = gateway;
        this.order   = order;
    }

    public void process() { gateway.charge(order.total()); }
}

// Resolving with a runtime value
Order currentOrder = getOrderFromRequest();
Map<String, Object> rtArgs = Collections.singletonMap("order", currentOrder);

OrderProcessor processor = resolver.dynamicResolve(OrderProcessor.class, rtArgs);
processor.process();
```

Each `dynamicResolve` call with the same `rtArgs` map returns the same instance (realtime singleton). A different map (even with equal values) may return a different instance depending on internal keying.

**Constraint:** A class with `@Realtime` parameters cannot itself be declared as a dependency for another class, because its constructor arguments are not fully resolvable at startup.

**See Also:** [`DependencyResolver` API](#dependencyresolver-api)

---

## Tagged Dependencies

### What it does

Tags let you group multiple implementations under a logical name and select among them at the injection site or at resolve time without knowing concrete class names.

### Declaring tags

Tags are declared on `@Dependency` or `@DependencyProvider`:

```java
public interface Notifier { String name(); }

@Dependency(tags = {"email", "async"})
public class EmailNotifier implements Notifier {
    public String name() { return "email"; }
}

@Dependency(tags = {"sms", "async"})
public class SmsNotifier implements Notifier {
    public String name() { return "sms"; }
}

@Dependency(tags = {"push", "async"}, isDefault = true)
public class PushNotifier implements Notifier {
    public String name() { return "push"; }
}
```

### Injecting with `@Tag`

```java
package com.simplj.di.annotations;

@Documented
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Tag {
    String value();  // the tag name to filter by
}
```

```java
@Dependency
public class AlertService {
    private final Notifier notifier;

    // All three implement "async"; PushNotifier is isDefault, so it is injected
    public AlertService(@Tag("async") Notifier notifier) {
        this.notifier = notifier;
    }
}
```

When the tag matches exactly one implementation, that one is used. When multiple match, the `isDefault` one is used. When multiple match and none is default, SDF throws an ambiguity error.

### Resolving by tag manually

```java
DependencyResolver resolver = DependencyResolverFactory.defaultResolver();

Notifier email = resolver.resolve(Notifier.class, "email");   // EmailNotifier
Notifier sms   = resolver.resolve(Notifier.class, "sms");     // SmsNotifier
Notifier async = resolver.resolve(Notifier.class, "async");   // PushNotifier (default)
```

**Gotcha:** Resolving `resolver.resolve(Notifier.class)` without a tag when all implementations have tags throws `SdfException`, because none is the global default.

---

## Profile-Based Dependency Resolution

### What it does

Profiles allow different implementations of an interface to be loaded depending on a runtime flag – for example, loading a real database adapter in production and an in-memory stub in tests.

### Declaring profiles

```java
public interface Repository { void save(Object entity); }

@Dependency(profiles = "prod", isDefault = true)
public class JpaRepository implements Repository {
    public void save(Object entity) { /* write to DB */ }
}

@Dependency(profiles = "test", isDefault = true)
public class InMemoryRepository implements Repository {
    private final List<Object> store = new ArrayList<>();
    public void save(Object entity) { store.add(entity); }
}
```

A class without a `profiles` entry is always loaded regardless of the active profile.

### Activating a profile via builder

```java
// Production bootstrap
DependencyResolverFactory.configureDefaultResolver(
    DependencyResolverConfig.builder()
        .setBasePackages("com.example.app")
        .setProfile("prod")
        .build()
);

// Test bootstrap
DependencyResolverFactory.configureResolver("test-ctx",
    DependencyResolverConfig.builder()
        .setBasePackages("com.example.app")
        .setProfile("test")
        .build()
);
```

### Activating a profile via JVM argument

```
-Dsdf.<context>.profile=<profileName>
```

For the default context: `-Dsdf.default.profile=prod`

**JVM argument takes precedence** over the value supplied in `DependencyResolverConfig.Builder.setProfile()`.

### Behavior rules

- Dependencies whose `profiles` array is empty load in every profile.
- Dependencies whose `profiles` array is non-empty load only when the active profile matches at least one entry.
- When no profile is set, only no-profile dependencies are loaded; profile-filtered ones are ignored.
- If two implementations match the active profile and neither is `isDefault`, SDF throws an ambiguity error.

**See Also:** [Contextual Dependency Resolution](#contextual-dependency-resolution)

---

## Contextual Dependency Resolution

### What it does

SDF supports multiple named resolver contexts within the same JVM. Each context has its own independent dependency pool, allowing different subsystems to load different sets of classes.

### Example

```java
// Context A – reporting subsystem
DependencyResolverFactory.configureResolver("reporting",
    DependencyResolverConfig.builder()
        .setBasePackages("com.example.reporting")
        .build()
);

// Context B – billing subsystem
DependencyResolverFactory.configureResolver("billing",
    DependencyResolverConfig.builder()
        .setBasePackages("com.example.billing")
        .build()
);

DependencyResolver reportingResolver = DependencyResolverFactory.resolver("reporting");
DependencyResolver billingResolver   = DependencyResolverFactory.resolver("billing");

ReportGenerator rg = reportingResolver.resolve(ReportGenerator.class);
InvoiceService  is = billingResolver.resolve(InvoiceService.class);
```

The "default" context is a named context called `"default"` – `configureDefaultResolver(...)` and `defaultResolver()` are just convenience wrappers for that name.

**Gotcha:** Calling `DependencyResolverFactory.resolver("unknown-ctx")` before configuring it throws `SdfException`. Always configure before accessing.

**Gotcha:** Attempting to configure the same context name twice produces a warning and the second call is silently ignored.

---

## Generic Type Dependencies

### What it does

SDF resolves dependencies with generic type parameters precisely, distinguishing `Service<String>` from `Service<Integer>`. This section covers the full spectrum of supported generic patterns.

### Basic generic injection

```java
abstract class Repository<T> {}

@Dependency
class UserRepository extends Repository<User> {}

@Dependency
class OrderRepository extends Repository<Order> {}

@Dependency
class DataExporter {
    private final Repository<User> userRepo;

    // SDF injects UserRepository because it extends Repository<User>
    public DataExporter(Repository<User> userRepo) {
        this.userRepo = userRepo;
    }
}
```

### Resolving generic types with `TypeClass<T>`

Java erasure prevents writing `resolver.resolve(Repository<User>.class)`. Use `TypeClass<T>` as the generic type token instead:

```java
import com.simplj.di.core.TypeClass;

// Create an anonymous subclass – this captures the type argument at runtime
TypeClass<Repository<User>> userRepoType = new TypeClass<Repository<User>>() {};
Repository<User> repo = resolver.resolve(userRepoType);
```

**See Also:** [`TypeClass<T>` – Generic Type Token](#typeclasst--generic-type-token)

### Upper-bound wildcard subtypes

```java
abstract class Animal<T extends FoodType> {}

@Dependency
class Deer  extends Animal<Herbivorous> {}
@Dependency
class Tiger extends Animal<Carnivorous> {}

@Dependency
class Zoo {
    private final List<Animal<? extends FoodType>> animals;

    // SDF collects all Animal subtypes
    public Zoo(@SubTypes List<Animal<? extends FoodType>> animals) {
        this.animals = animals;
    }
}
```

SDF deduces `? extends FoodType` into all concrete food types and resolves matching animal implementations accordingly.

### Lower-bound wildcard

An instance of `Number` can be assigned to `? super Integer`.

### Type variables with `@Bind` or `@Realtime`

Naked type variables (`<T>` without a bound) are not allowed. They must be resolved via `@Bind` or `@Realtime`:

```java
public class DependencyModule {

    @DependencyProvider(id = "child")
    public Child child() { return new Child(); }

    // T is pinned to Child because the @Bind resolves to a Child instance
    @DependencyProvider
    public <T> Set<T> studentSet(@Bind(id = "child") T value) {
        return Collections.singleton(value);
    }

    @DependencyProvider
    public <T extends Student> School<T> school(Set<T> students) {
        return new School<>(students);
    }
}
```

SDF substitutes `T = Child`, which cascades through the return type `Set<Child>`, and that in turn satisfies `Set<T>` in `school()`.

---

## Dynamic Method Invocation

### What it does

SDF can locate and call methods annotated with `@DynamicInvocation` by name (or alias) at runtime, injecting their `@Implicit` dependencies automatically and accepting remaining arguments from the caller. This allows loosely coupled command dispatch, plugin execution, or scripting-layer integration.

### `@DynamicInvocation`

```java
package com.simplj.di.annotations;

@Documented
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DynamicInvocation {
    String   alias()       default "";     // alternative invoke name; method name used if empty
    boolean  disable()     default false;  // true = exclude from registration
    String   description() default "";     // human-readable description for introspection
    String[] profiles()    default {};     // active-profile filter
}
```

| Field | Type | Default | Purpose |
|-------|------|---------|---------|
| `alias` | `String` | `""` | Name passed to `resolver.invoke(alias, ...)` |
| `disable` | `boolean` | `false` | Exclude this method from dynamic registration |
| `description` | `String` | `""` | Surfaced in `DynamicMethod.description()` |
| `profiles` | `String[]` | `{}` | Load only when the active profile matches |

### `@Implicit`

Marks a parameter that SDF resolves from its dependency container automatically (the caller does not supply it):

```java
package com.simplj.di.annotations;

@Documented
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Implicit {
    String  id()       default "";    // bind to a specific dependency id; type-based if empty
    boolean nullable() default false; // allow null if not found
}
```

The `id` field supports the variable notation `${constantId}`.

### `@Label`

Assigns a human-readable label to a parameter for introspection via `Argument.label()`:

```java
package com.simplj.di.annotations;

@Documented
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Label {
    String value();
}
```

### Full example

```java
import com.simplj.di.annotations.DynamicInvocation;
import com.simplj.di.annotations.Implicit;
import com.simplj.di.annotations.Label;
import com.simplj.di.annotations.Realtime;

public class MathOperations {

    // Alias "sum"; SDF resolves int[] from the container (@Implicit), caller adds varargs
    @DynamicInvocation(alias = "sum")
    public int add(@Implicit int[] base, int... extra) {
        return IntStream.concat(IntStream.of(base), IntStream.of(extra)).sum();
    }

    @DynamicInvocation(description = "Multiply two integers")
    public int multiply(int a, int b) {
        return a * b;
    }

    // Mixes @Implicit, @Realtime, and positional args
    @DynamicInvocation
    public String format(
            @Implicit EmailService        emailService,   // from container
            @Realtime(key = "template")  String template, // from rtParams map
            String subject) {                             // from varargs
        return String.format(template, subject);
    }
}

// Registration
DependencyResolverFactory.configureDefaultResolver(
    DependencyResolverConfig.builder()
        .setDependencyInstances(new MathOperations())
        .setDependencyProviders(new AppConfig())
        .withDynamicMethods()
        .build()
);

DependencyResolver resolver = DependencyResolverFactory.defaultResolver();

// Invoke by alias
int result = resolver.invoke("sum", int.class, new int[]{1, 2}, 3);   // 6
int product = resolver.invoke("multiply", int.class, 4, 5);           // 20

// Invoke with runtime params
Map<String, Object> rt = Collections.singletonMap("template", "Hello %s");
String msg = resolver.invoke("format", String.class, rt, "World");    // "Hello World"
```

### Registering methods via `setDependencyInstances`

Pass pre-built instances of classes that contain `@DynamicInvocation` methods using `setDependencyInstances()`. These instances are both dependency providers and method hosts.

### Overriding aliases

Alias names can be overridden at bootstrap time using a map keyed in the format `"sjf.<CanonicalClassName>.<methodName>.alias"`:

```java
Map<String, String> aliases = Collections.singletonMap(
    "sjf.com.example.MathOperations.add.alias", "addition"
);

DependencyResolverConfig.builder()
    .setDependencyInstances(new MathOperations())
    .withDynamicMethods(aliases)
    .build();

// Now invoke with the overridden name
resolver.invoke("addition", int.class, 2, 3);
```

**See Also:** [Implicit Type Conversions with `TypeConversions`](#implicit-type-conversions-with-typeconversions), [`DynamicMethod` and `Argument` – Introspection](#dynamicmethod-and-argument--introspection)

---

## Implicit Type Conversions with `TypeConversions`

### What it does

When invoking dynamic methods, the caller may supply arguments of a type that does not exactly match the method's parameter type. `TypeConversions` lets you register converter functions so SDF can coerce values automatically.

### API

```java
package com.simplj.di.core;

public class TypeConversions {

    public static Builder builder();
    public static TypeConversions none();          // empty (no conversions)

    // Look up a converter
    public <A, B> Optional<Function<A, B>> getConverter(Class<A> from, Class<B> to);
    public <A, B> Optional<Function<A, B>> getConverter(TypeClass<A> from, TypeClass<B> to);
    public Optional<Function<Object, ?>>   getConverter(Type from, Type to);
    public Optional<Function<Object, ?>>   getConverter(TypeRef from, TypeRef to);

    // Apply a converter
    public <A, B> Optional<B>   convert(Class<A> from, Class<B> to, Object val);
    public <A, B> Optional<B>   convert(TypeClass<A> from, TypeClass<B> to, Object val);
    public Optional<Object>     convert(Type from, Type to, Object val);
    public Optional<Object>     convert(TypeRef from, TypeRef to, Object val);

    public static class Builder {
        public <A, B> Builder register(Class<A> from, Class<B> to, Function<A, B> converterF);
        public <A, B> Builder register(TypeClass<A> from, TypeClass<B> to, Function<A, B> converterF);
        public <A, B> Builder register(Type from, Type to, Function<A, B> converterF);
        public        Builder register(TypeRef from, TypeRef to, Function<Object, ?> converterF);
        public TypeConversions build();
    }
}
```

### Example

```java
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

TypeConversions conversions = TypeConversions.builder()
    .register(String.class, Integer.class, Integer::parseInt)
    .register(String.class, int.class,     String::length)
    .register(String.class, LocalDate.class,
        s -> LocalDate.parse(s, DateTimeFormatter.ofPattern("yyyy.MM.dd")))
    .build();

DependencyResolverFactory.configureDefaultResolver(
    DependencyResolverConfig.builder()
        .setDependencyInstances(new MathOperations())
        .withDynamicMethods(conversions)
        .build()
);

// The method signature is add(int a, int b)
// Strings are converted to int via the registered converter
int result = resolver.invoke("add", int.class, "3", "7");  // 10
```

Conversions are applied only during `resolver.invoke()` calls, not during regular `resolver.resolve()`.

---

## `DependencyResolver` API

The `DependencyResolver` is the central point for retrieving managed objects. Obtain an instance via `DependencyResolverFactory`.

### `resolve` – standard resolution

```java
// By Class type
public <T> T resolve(Class<T> clazz) throws SdfException;

// By Class type and tag
public <T> T resolve(Class<T> clazz, String tag) throws SdfException;

// By TypeClass (for generic types)
public <T> T resolve(TypeClass<T> type) throws SdfException;

// By TypeClass and tag
public <T> T resolve(TypeClass<T> type, String tag) throws SdfException;

// By id and Class type
public <T> T resolve(String id, Class<T> type) throws SdfException;

// By id and TypeClass
public <T> T resolve(String id, TypeClass<T> type) throws SdfException;
```

| Parameter | Description |
|-----------|-------------|
| `clazz` | The exact class or interface to resolve |
| `tag` | Optional tag to narrow the resolution |
| `type` | `TypeClass<T>` token for parameterized types |
| `id` | Explicit id assigned via `@Dependency.id`, `@DependencyProvider.id`, or `@Constant.id` |

**Throws** `SdfException` if the resolver is not configured, the type is not registered, or there is an ambiguity.

**Examples**

```java
DependencyResolver resolver = DependencyResolverFactory.defaultResolver();

// Resolve by type
MyService svc = resolver.resolve(MyService.class);

// Resolve by id
MyService specific = resolver.resolve("myServiceId", MyService.class);

// Resolve a generic type
TypeClass<List<MyService>> listType = new TypeClass<List<MyService>>() {};
List<MyService> services = resolver.resolve(listType);

// Resolve with tag
Notifier email = resolver.resolve(Notifier.class, "email");

// Resolve a constant
String host = resolver.resolve("server.host", String.class);
```

### `dynamicResolve` – resolution with runtime parameters

```java
// By Class
public <T> T dynamicResolve(Class<T> clazz, Map<String, Object> rtArgs) throws SdfException;

// By Class and tag
public <T> T dynamicResolve(Class<T> clazz, String tag, Map<String, Object> rtArgs) throws SdfException;

// By TypeClass
public <T> T dynamicResolve(TypeClass<T> type, Map<String, Object> rtArgs) throws SdfException;

// By TypeClass and tag
public <T> T dynamicResolve(TypeClass<T> type, String tag, Map<String, Object> rtArgs) throws SdfException;

// By id and Class
public <T> T dynamicResolve(String id, Class<T> type, Map<String, Object> rtArgs) throws SdfException;

// By id and TypeClass
public <T> T dynamicResolve(String id, TypeClass<T> type, Map<String, Object> rtArgs) throws SdfException;
```

| Parameter | Description |
|-----------|-------------|
| `rtArgs` | Map from `@Realtime.key` to the runtime value |

```java
// The class has @Realtime(key = "user") in its constructor
Map<String, Object> rt = Collections.singletonMap("user", currentUser);
UserSession session = resolver.dynamicResolve(UserSession.class, rt);
```

### `invoke` – dynamic method invocation

```java
// Return type as Class
public <T> T invoke(String method, Class<T> returnType, Object... args) throws SdfException;

// Return type as TypeClass
public <T> T invoke(String method, TypeClass<T> returnType, Object... args) throws SdfException;

// With runtime params and Class return type
public <T> T invoke(String method, Class<T> returnType, Map<String, Object> rtParams, Object... args) throws SdfException;

// With runtime params and TypeClass return type
public <T> T invoke(String method, TypeClass<T> returnType, Map<String, Object> rtParams, Object... args) throws SdfException;

// Raw Object return
public Object invoke(String method, Object... args) throws SdfException;

// Raw Object return with runtime params
public Object invoke(String method, Map<String, Object> rtParams, Object... args) throws SdfException;
```

| Parameter | Description |
|-----------|-------------|
| `method` | The `@DynamicInvocation` method name or alias |
| `returnType` | Expected return type for casting |
| `rtParams` | Values for `@Realtime` parameters in the method |
| `args` | Positional arguments for non-implicit, non-realtime parameters (varargs) |

```java
// Method: public int add(int a, int b)
int sum = resolver.invoke("add", int.class, 3, 5);  // 8

// Method: public String format(@Realtime(key="tpl") String tpl, String name)
Map<String, Object> rt = Collections.singletonMap("tpl", "Hello %s!");
String greeting = resolver.invoke("format", String.class, rt, "Alice");
```

### `resolvedDynamicMethods`

```java
public Map<String, DynamicMethod> resolvedDynamicMethods();
```

Returns a map from method alias/name to its `DynamicMethod` descriptor. Useful for building help text or validating available operations.

```java
Map<String, DynamicMethod> methods = resolver.resolvedDynamicMethods();
methods.forEach((name, m) -> System.out.println(name + " -> " + m));
```

---

## `DependencyResolverFactory` API

`DependencyResolverFactory` is a static utility class (singleton internally) that creates and stores `DependencyResolver` instances by context name.

```java
package com.simplj.di.core;

public class DependencyResolverFactory {

    // Configure the default ("default") context
    public static void configureDefaultResolver(DependencyResolverConfig config);

    // Configure a named context
    public static void configureResolver(String context, DependencyResolverConfig config);

    // Retrieve the default resolver (must be configured first)
    public static DependencyResolver defaultResolver();

    // Retrieve a named resolver (must be configured first)
    public static DependencyResolver resolver(String context);
}
```

| Method | Throws | Notes |
|--------|--------|-------|
| `configureDefaultResolver` | `SdfException` | Must be called before `defaultResolver()` |
| `configureResolver` | `SdfException` | Silently no-ops if context already exists |
| `defaultResolver` | `SdfException` | Throws if default context not configured |
| `resolver(context)` | `SdfException` | Throws if named context not configured |

**Thread safety:** `configureResolver` is safe to call from multiple threads. If two threads race to configure the same context, only the first call takes effect.

---

## `DependencyResolverConfig.Builder` Reference

All wiring options flow through `DependencyResolverConfig.builder()`.

```java
public static class Builder {

    // Register provider class instances containing @DependencyProvider / @Constant methods
    public Builder setDependencyProviders(Object... dependencyProviders);

    // Set base packages to scan for @Dependency classes
    public Builder setBasePackages(String... packages);

    // Supply constants via java.util.Properties
    public Builder setProperties(Properties props);

    // Supply pre-built instances as dependencies
    public Builder setDependencyInstances(Object... instances);

    // Set the active profile name
    public Builder setProfile(String profile);

    // Set a custom logging function (defaults to System.out::println)
    public Builder setLoggerF(Consumer<String> loggerF);

    // Enable dynamic method scanning (no conversions, no alias overrides)
    public Builder withDynamicMethods();

    // Enable dynamic methods with alias overrides
    public Builder withDynamicMethods(Map<String, String> overriddenAliases);

    // Enable dynamic methods with implicit type conversions
    public Builder withDynamicMethods(TypeConversions conversions);

    // Enable dynamic methods with both
    public Builder withDynamicMethods(Map<String, String> overriddenAliases, TypeConversions conversions);

    public DependencyResolverConfig build();
}
```

| Method | Required? | Description |
|--------|-----------|-------------|
| `setDependencyProviders` | if using `@DependencyProvider`/`@Constant` | Instances of classes with provider methods |
| `setBasePackages` | if using `@Dependency` on classes | Packages to scan for annotated classes |
| `setProperties` | no | Key/value constants from a Properties object |
| `setDependencyInstances` | no | Pre-constructed objects to register as dependencies |
| `setProfile` | no | Active profile; empty string is same as no profile |
| `setLoggerF` | no | Custom log sink; defaults to `System.out::println` |
| `withDynamicMethods` | if using `@DynamicInvocation` | Activates dynamic method scanning |

**Validation:** `setDependencyProviders` throws `IllegalArgumentException` if a supplied instance's class contains no `@DependencyProvider` or `@Constant` methods.

---

## `TypeClass<T>` – Generic Type Token

### What it does

Java's type erasure prevents writing `MyClass<String>.class`. `TypeClass<T>` captures the full parameterized type at compile time via an anonymous subclass. Use it anywhere `Class<T>` would lose the generic argument.

### API

```java
package com.simplj.di.core;

public abstract class TypeClass<T> {

    public TypeClass() throws ClassNotFoundException;  // used by anonymous subclasses

    public final String   getName();     // e.g. "java.util.List<java.lang.String>"
    public final Class<T> getRawType();  // e.g. List.class
    public final Type     getType();     // the full parameterized Type
}
```

### Usage

```java
import com.simplj.di.core.TypeClass;

// Capturing List<String>
TypeClass<List<String>> listType = new TypeClass<List<String>>() {};
List<String> strings = resolver.resolve(listType);

// Capturing a nested generic
TypeClass<Map<String, List<Integer>>> mapType = new TypeClass<Map<String, List<Integer>>>() {};
Map<String, List<Integer>> map = resolver.resolve(mapType);
```

Always use the anonymous-subclass pattern `new TypeClass<X>() {}` – never instantiate `TypeClass` directly.

**Throws** `ClassNotFoundException` from the constructor if the raw class cannot be found on the classpath (rare in practice).

---

## `DynamicMethod` and `Argument` – Introspection

These model classes expose metadata about dynamically registered methods. They are returned by `resolver.resolvedDynamicMethods()`.

### `DynamicMethod`

```java
package com.simplj.di.model;

public class DynamicMethod {

    public Class<?>       containerClass(); // the class that declares the method
    public String         className();      // canonical name of the container class
    public String         methodName();     // the @DynamicInvocation method name
    public TypeRef        returnType();     // return type descriptor
    public String         description();   // @DynamicInvocation.description value
    public List<Argument> arguments();     // non-implicit, non-realtime parameters

    @Override
    public String toString();
    // format: "ClassName#methodName(type1 label1, type2 label2)"
}
```

### `Argument`

```java
package com.simplj.di.model;

public class Argument {

    public String  label();      // @Label value or parameter name
    public String  typeName();   // fully qualified type name (e.g. "java.util.List<java.lang.Integer>")
    public TypeRef type();       // type descriptor
    public boolean isNullable(); // true if @Realtime(nullable=true) or @Implicit(nullable=true)

    @Override
    public String toString();
    // format: "typeName label [(O) if nullable]"
}
```

### Example

```java
Map<String, DynamicMethod> methods = resolver.resolvedDynamicMethods();

DynamicMethod m = methods.get("flatSum");
System.out.println(m.className());              // dynamic.methods.DynamicMethods
System.out.println(m.returnType().name());      // int
System.out.println(m.description());           // "takes a list of list of integers..."
System.out.println(m.arguments().get(0).typeName());
// java.util.List<java.util.List<java.lang.Integer>>

// Build a signature string
String sig = m.className() + ": "
    + m.returnType().name() + " "
    + m.methodName() + "("
    + m.arguments().stream()
          .map(a -> a.label() + " :: " + a.type().name())
          .collect(Collectors.joining(", "))
    + ")";
```

---

## Error Handling & Exceptions

### `SdfException`

```java
package com.simplj.di.exceptions;

public class SdfException extends RuntimeException {
    public SdfException();
    public SdfException(String message);
    public SdfException(String message, Throwable cause);
    public SdfException(Throwable cause);
}
```

`SdfException` is an unchecked exception. It is the root of all SDF errors. Common triggers:

| Trigger | Message hint |
|---------|-------------|
| Resolver not configured | `"DependencyResolver is not configured!"` |
| Context not found | `"DependencyResolver not found for '<ctx>' context!"` |
| Type not registered | varies |
| Ambiguous implementations | contains "ambiguity" |
| `resolve()` on a tagged type without passing a tag | `SdfException` |
| Library initialization failure | `"Could not initialize SDF!"` |

**Handling example**

```java
try {
    MyService svc = resolver.resolve(MyService.class);
} catch (SdfException e) {
    // Log and fail fast at startup; SdfException at resolve time is usually a config error
    throw new RuntimeException("DI failure: " + e.getMessage(), e);
}
```

### `CircularDependencyException`

```java
package com.simplj.di.exceptions;

public class CircularDependencyException extends SdfException {
    public CircularDependencyException(String className);
    public CircularDependencyException(String message, String className);
    public CircularDependencyException addChain(String className);

    @Override
    public String getMessage();
    // format: "Circular Dependency Found - Chain: A -> B -> C -> A"
}
```

Thrown when SDF detects a cycle during dependency loading. The `getMessage()` output includes the full dependency chain to identify the cycle quickly.

**Example message:** `"Circular Dependency Found - Chain: OrderService -> PaymentService -> OrderService"`

SDF automatically avoids self-referential cycles for `@SubTypes` injection (an `Animal<Carnivorous>` that collects all `Animal<? extends FoodType>` subtypes does not inject itself).

---

## Configuration Reference

This table summarises every configuration option and runtime knob in one place.

### `DependencyResolverConfig.Builder` options

| Builder method | Type | Default | Description |
|----------------|------|---------|-------------|
| `setBasePackages` | `String...` | none | Packages scanned for `@Dependency` classes |
| `setDependencyProviders` | `Object...` | none | Instances with `@DependencyProvider`/`@Constant` methods |
| `setProperties` | `Properties` | none | String constants from a Properties object |
| `setDependencyInstances` | `Object...` | none | Pre-built objects registered as dependencies |
| `setProfile` | `String` | `""` (none) | Active profile name |
| `setLoggerF` | `Consumer<String>` | `System.out::println` | Log sink |
| `withDynamicMethods()` | – | disabled | Enable `@DynamicInvocation` scanning |
| `withDynamicMethods(aliases)` | `Map<String,String>` | `{}` | Alias overrides in `sjf.<Class>.<method>.alias` format |
| `withDynamicMethods(conversions)` | `TypeConversions` | none | Implicit argument type coercions |

### JVM system properties

| Property key | Description |
|--------------|-------------|
| `sdf_consts.<id>` | Constant value for `<id>`. Example: `-Dsdf_consts.env.name=dev` |
| `sdf.<context>.profile` | Active profile for context `<context>`. Example: `-Dsdf.default.profile=prod` |

### `@Dependency` fields

| Field | Type | Default |
|-------|------|---------|
| `id` | `String` | `""` |
| `initMethod` | `String` | `""` |
| `tags` | `String[]` | `{}` |
| `profiles` | `String[]` | `{}` |
| `singleton` | `boolean` | `true` |
| `isDefault` | `boolean` | `false` |

### `@DependencyProvider` fields

| Field | Type | Default |
|-------|------|---------|
| `id` | `String` | `""` |
| `tags` | `String[]` | `{}` |
| `profiles` | `String[]` | `{}` |
| `singleton` | `boolean` | `true` |
| `isDefault` | `boolean` | `false` |

### `@Constant` fields

| Field | Type | Required |
|-------|------|----------|
| `id` | `String` | yes |
| `tags` | `String[]` | no |
| `profiles` | `String[]` | no |

### `@DynamicInvocation` fields

| Field | Type | Default |
|-------|------|---------|
| `alias` | `String` | `""` (uses method name) |
| `disable` | `boolean` | `false` |
| `description` | `String` | `""` |
| `profiles` | `String[]` | `{}` |

### Parameter annotation fields

| Annotation | Fields |
|------------|--------|
| `@Bind` | `id` (required) |
| `@SubTypes` | `emptyIfNotFound` (default `true`) |
| `@Realtime` | `key` (required), `nullable` (default `false`) |
| `@Implicit` | `id` (default `""`), `nullable` (default `false`) |
| `@Tag` | `value` (required) |
| `@Label` | `value` (required) |

---

## Thread Safety & Concurrency Notes

| Component | Thread-safe? | Notes |
|-----------|-------------|-------|
| `DependencyResolverFactory` | Yes | Uses `ConcurrentHashMap` internally; `configureResolver` uses `putIfAbsent` to prevent duplicate setup |
| `DependencyResolver.setup` | Yes | Double-checked locking with `synchronized`; safe to call from multiple threads |
| `DependencyResolver.resolve` | Yes after setup | The loader is immutable after setup; resolving is read-only |
| `DependencyResolver.dynamicResolve` | Depends | Safe if the resolved class is singleton; non-singleton creates new instances, relying on the constructor |
| `DependencyResolver.invoke` | Depends | Thread-safety is determined by the `@DynamicInvocation` method implementation itself |
| `DependencyResolverConfig` | Immutable | Built once by the Builder; safe to share |
| `TypeConversions` | Immutable | Built once by the Builder |
| `TypeClass<T>` | Effectively immutable | Fields are set in the constructor and never modified |
| `DynamicMethod` / `Argument` | Immutable | All fields final |

**Best practice:** Call `configureDefaultResolver` (or `configureResolver`) exactly once at application startup, before any thread starts resolving dependencies. The resolver itself is safe for concurrent use after that point.

---

## Type Substitutions

SDF applies generalized type substitution when matching an available dependency against a required type. The following table shows which assignments SDF accepts, using `Map<String, X>` as the example.

| Available type | Required type | Assignable | Reason |
|----------------|---------------|:----------:|--------|
| `Map<String, ? extends Integer>` | `Map<String, ? extends Integer>` | ✅ | Exact |
| `Map<String, ? extends Integer>` | `Map<String, Integer>` | ✅ | |
| `Map<String, ? extends Integer>` | `Map<String, ? extends Number>` | ✅ | Integer ⊂ Number |
| `Map<String, ? extends Integer>` | `Map<String, Number>` | ✅ | |
| `Map<String, ? extends Integer>` | `Map` | ✅ | Raw type accepts any |
| `Map<String, Integer>` | `Map<String, ? extends Integer>` | ✅ | |
| `Map<String, Integer>` | `Map<String, Integer>` | ✅ | Exact |
| `Map<String, Integer>` | `Map<String, ? extends Number>` | ✅ | |
| `Map<String, Integer>` | `Map<String, Number>` | ✅ | |
| `Map<String, Integer>` | `Map` | ✅ | |
| `Map<String, ? extends Number>` | `Map<String, ? extends Integer>` | ❌ | `? extends Number` ⊄ `? extends Integer` |
| `Map<String, ? extends Number>` | `Map<String, Integer>` | ❌ | `? extends Number` cannot be assigned to `Integer` |
| `Map<String, ? extends Number>` | `Map<String, ? extends Number>` | ✅ | Exact |
| `Map<String, ? extends Number>` | `Map<String, Number>` | ✅ | |
| `Map<String, ? extends Number>` | `Map` | ✅ | |
| `Map<String, Number>` | `Map<String, ? extends Integer>` | ❌ | `Number` ⊄ `? extends Integer` |
| `Map<String, Number>` | `Map<String, Integer>` | ❌ | `Number` cannot be assigned to `Integer` |
| `Map<String, Number>` | `Map<String, ? extends Number>` | ✅ | |
| `Map<String, Number>` | `Map<String, Number>` | ✅ | Exact |
| `Map<String, Number>` | `Map` | ✅ | |
| `Map` (raw) | `Map<String, ? extends Integer>` | ❌ | Raw `Map` = `Map<Object, Object>`; `Object` ⊄ `String` |
| `Map` (raw) | `Map<String, Integer>` | ❌ | |
| `Map` (raw) | `Map<String, ? extends Number>` | ❌ | |
| `Map` (raw) | `Map<String, Number>` | ❌ | |
| `Map` (raw) | `Map` | ✅ | Exact |

**Summary rules:**

1. A specific type (e.g. `Integer`) is assignable to a covariant wildcard that covers it (`? extends Number`).
2. A wildcard (`? extends X`) is NOT assignable to a more specific wildcard or concrete type.
3. A raw type is treated as `Object`-parameterized and rarely matches parameterized requirements.

---

## Constraints & Restrictions

1. **Single public constructor:** When using `@Dependency` without `initMethod`, the class must have exactly one `public` constructor.
2. **Static `initMethod`:** The factory method named in `initMethod` must be `static` and declared in the same class.
3. **No abstract or interface `@Dependency`:** Only concrete classes can bear `@Dependency`.
4. **No `java.lang.*` as dependency type:** Classes from `java.lang` (e.g., `String`, `Integer`) cannot be returned by `@DependencyProvider`. Use `@Constant` instead.
5. **JVM-argument constant prefix:** Constants passed via JVM must start with `sdf_consts.`. Example: `-Dsdf_consts.env=dev`.
6. **`@Bind` variable id must resolve to `String`:** When using `${constantId}` in `@Bind`, `@Realtime`, or `@Implicit`, the referenced constant must be of type `String`.
7. **Configure before resolve:** `DependencyResolver` must be configured via the factory before calling `resolve()` or `dynamicResolve()`.
8. **Provider class instances, not class objects:** `setDependencyProviders()` accepts *instances* of provider classes, not `Class<?>` objects.
9. **Naked type variables forbidden:** A method's type variable must be constrained by `@Bind` or `@Realtime`. An unconstrained `T` leads to ambiguity and an `SdfException`.
10. **`@Realtime` classes cannot be wired as dependencies:** A class whose constructor has `@Realtime` parameters cannot be used as a constructor argument of another managed class.
11. **Only `List` and `Map` for `@SubTypes`:** Other collection types (e.g., `Set`, `Queue`) are not supported.
12. **Ambiguity error:** If multiple implementations qualify for a type (by type, tag, or profile) and none is `isDefault`, SDF throws an `SdfException` at startup.

---

## FAQ / Troubleshooting

**Q: I get `SdfException: DependencyResolver is not configured!`**

You called `resolver.resolve()` before calling `DependencyResolverFactory.configureDefaultResolver()`. Ensure bootstrap code runs before the first resolve call.

---

**Q: SDF throws an ambiguity error for my interface.**

More than one `@Dependency` (or `@DependencyProvider`) implementation matches the requested type and none is `isDefault`. Fix options:
- Add `isDefault = true` to the preferred implementation.
- Assign distinct `id` values and use `@Bind` at injection sites.
- Use `tags` and `@Tag` to separate implementations.

---

**Q: My `@DependencyProvider` class passes validation but the dependency isn't found at runtime.**

Check that you passed an **instance** to `setDependencyProviders()`, not a `.class` reference:

```java
// WRONG
.setDependencyProviders(MyProviders.class)

// CORRECT
.setDependencyProviders(new MyProviders())
```

---

**Q: Constants from `Properties` aren't being injected.**

Verify that the property key exactly matches the `@Bind(id = "...")` value, and that `setProperties(props)` is called on the builder before `build()`.

---

**Q: I receive `IllegalArgumentException: Invalid Dependency Provider`.**

The class you passed to `setDependencyProviders()` has no methods annotated with `@DependencyProvider` or `@Constant`. Either annotate at least one method or remove the class from the call.

---

**Q: My `@Realtime` class is being reported as an error when injected into another class.**

Classes with `@Realtime` constructor parameters cannot be used as framework-managed dependencies of other classes. Extract the static dependencies into a separate service class, and use `dynamicResolve()` to build the realtime instance directly.

---

**Q: How do I use SDF in unit tests with a mock implementation?**

Use profiles:

```java
// In test setup
DependencyResolverFactory.configureResolver("test",
    DependencyResolverConfig.builder()
        .setBasePackages("com.example.app")
        .setProfile("test")
        .build()
);
DependencyResolver resolver = DependencyResolverFactory.resolver("test");
```

Mark real implementations with `@Dependency(profiles = "prod")` and mocks with `@Dependency(profiles = "test")`.

---

**Q: `CircularDependencyException` is thrown. How do I find the cycle?**

Read the `getMessage()` output: it prints the full chain, e.g. `A -> B -> C -> A`. Break the cycle by introducing an interface, using `singleton = false`, or restructuring the dependency graph.

---

**Q: `resolver.resolve(TagService.class)` throws even though one implementation is `isDefault`.**

When all implementations of an interface have at least one tag, SDF requires a tag to be supplied. Pass the tag to the resolve call:

```java
resolver.resolve(TagService.class, "myTag");
```

or inject with `@Tag("myTag")`.

---

**Q: My `@DynamicInvocation` method has the same name as another method. Which one is called?**

Method resolution uses the name or alias and the number and types of non-implicit, non-realtime arguments. If two methods share the same alias and the argument count is the same, provide distinct `alias` values in the annotation or override them via `withDynamicMethods(aliases)`.

---

**Q: Is there a Gradle example for the latest version?**

```groovy
dependencies {
    implementation 'com.simplj.di:sdf:2.3'
}
```

Check [Maven Central](https://search.maven.org/artifact/com.simplj.di/sdf) for the latest version.
