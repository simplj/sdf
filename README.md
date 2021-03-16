# [S]imple [D]ependency Injection [F]ramework

* Simple
  > As simple as adding one single annotation in a class
* Lightweight
  > less then 50 kb of size
* Powerful
  > We hope you will agree on this after getting introduced with the framework

Table of contents
=================
<!--ts-->
   * [Dependency](#dependency)
   * [Usage](#usage)
      * [Instantiating a class as Dependency using `@Dependency`](#instantiating-a-class-as-dependency-using-dependency)
         * [Through constructor](through-constructor)
         * [Through factory method](#through-factory-method)
      * [Instantiating a class as Dependency using `@DependencyProvider`](#instantiating-a-class-as-dependency-using-dependencyprovider)
      * [Injecting a subclass for it's parent (Single Implementation)](#injecting-a-subclass-for-its-parent-single-implementation)
      * [Injecting a subclass for it's parent (Multiple Implementations)](#injecting-a-subclass-for-its-parent-multiple-implementations)
      * [Providing Constants](#providing-constants)
         * [Using `@Constant` annotation](#using-constant-annotation)
         * [Through `java.lang.Properties`](#through-javalangproperties)
         * [Through JVM Argument](#through-jvm-argument)
      * [`@Bind` with variable id](bind-with-variable-id)
      * [Bootstrap (or Application entrypoint) Action](#bootstrap-or-application-entrypoint-action)
      * [Resolving Dependencies](#resolving-dependencies)
   * [Constraints/Restrictions](#constraintsrestrictions)
   * [Suggestions/Feedback](https://github.com/simplj/sdf/discussions)
   * [Report an Issue](https://github.com/simplj/sdf/issues)
   * [License](#License)
<!--te-->

## Dependency

```
<dependency>
    <groupId>com.simplj.di</groupId>
    <artifactId>sdf</artifactId>
    <version>0.4</version>
</dependency>
```
[Maven Repository](https://mvnrepository.com/artifact/com.simplj.di/sdf)

## Usage

### Instantiating a class as Dependency using `@Dependency`
##### Through constructor
```java
package "simple.example.sdf.service"

@Dependency
public class SimpleClass {
  ...
}
```
> By adding the `@Dependency` to the class enables it to get loaded by the framework as singleton instance. 💡If singleton behavior is not expected just modify the annotation to `@Dependency(singleton=false)`.
```java
package "complex.example.sdf.service"

@Dependency
public class DependantClass {
  private final SimpleClass simpleClass;
  public DependentClass(SimpleClass arg) {
    this.simpleClass = arg;
  }
  ...
}
```
> Dependency `SimpleClass` will be injected to `DependantClass` by the framework since `SimpleClass` is marked as `@Dependency`.

##### Through factory method
```java
@Dependency(initMethod = "getInstance")
public class FactoryClass {
  private FactoryClass() {
  }
  public static FactoryClass getInstance() {
    return new FactoryClass();
  }
  ...
}
```
> In the above example, the `FactoryClass` is instantiated using the `getInstance` method. **The factory method must be** `static`.
```java
@Dependency(initMethod = "getInstance")
public class DependantFactoryClass {
  private final FactoryClass factoryClass;
  private DependantFactoryClass(FactoryClass arg) {
    this.factoryClass = arg;
  }
  public static DependantFactoryClass getInstance(FactoryClass arg) {
    return new DependantFactoryClass(arg);
  }
  ...
}
```
> Dependency `FactoryClass` will be injected to `DependantFactoryClass` through the `static` factory method `getInstance` by the framework since `FactoryClass` is marked as `@Dependency`.

### Instantiating a class as Dependency using `@DependencyProvider`
Dependencies can also be provided in a separate class through a `static` method using `@DependencyProvider` annotation. **Classes from `java.lang` package cannot be used as a dependency**. 💡Check [Providing Constants](#providing-constants) section to see how to use `java.lang` classes as constants instead. Like `@Dependency`, `id`, `singleton` and `isDefault` fields can also be used in their respective purpose.
```java
public class HttpConnection {
  ...
}
public class HttpClient {
  private final HttpConnection httpConnection;
  public HttpClient(HttpConnection arg) {
    this.httpConnection = arg;
  }
  ...
}
~~~~~~~
public class HttpDependencyProviders {
  @DependencyProvider
  public static getHttpConnection() {
    return new HttpConnection();
  }
  @DependencyProvider
  public static httpClient(HttpConnection conn) {
    return new HttpClient(conn);
  }
  ...
}
```
> In the above example `HttpConnection` will be instantiated first and the instance will be passed to `httpClient` method to instantiate `HttpClient`. Dependency provider method names can be anything (there is no rule for naming pattern to follow). This way of initialization can be helpful in following two ways:
>  * Providing dependency for a class from external library which is not annotated with `@Dependency`.
>  * Putting instantiation logic of all the classes in a single place.
>
> 💡_Please note that if a class is annotated with `@Dependency` and is also configured using `@DependencyProvider` OR if two or more methods annotated with `@DependencyProvider` return the same type, then sdf will throw an `ambiguity error`._

### Injecting a subclass for it's parent (Single Implementation)
```java
public interface IAdapter {
  ...
}
~~~~~~~
@Dependency
public class DbAdapter implements IAdapter {
   ...
}
~~~~~~~
@Dependency
public class SomeService {
  private final IAdapter adapter;
  public SomeService(IAdapter adapter) {
    this.adapter = adapter;
  }
}
```
> In the above example `DbAdapter` will be passed for the dependency `IAdapter` in `SomeService` as the interface has only one implementation and the same is marked as `@Dependency`. In case there are multiple dependencies then sdf will throw an `ambiguity error` at runtime. There are two ways to resolve this:
>  * setting `isDefault=true` in the `@Dependency` annotation for the implementation which can be considered default.
>  * assigning an `id` for the implementations and `@Bind`-ing with specific id in parameter as desired.

### Injecting a subclass for it's parent (Multiple Implementations)
```java
public interface IAdapter {
  ...
}
~~~~~~~
@Dependency(id = "dbAdapter", isDefault = true)
public class DbAdapter implements IAdapter {
   ...
}
@Dependency(id = "fileAdapter")
public class FileAdapter implements IAdapter {
   ...
}
~~~~~~~
@Dependency
public class FileHandler {
  private final IAdapter adapter;
  public FileHandler(@Bind(id = "fileHandler") IAdapter adapter) {
    this.adapter = adapter;
  }
}
@Dependency
public class DbHandler {
  private final IAdapter adapter;
  public FileHandler(IAdapter adapter) {
    this.adapter = adapter;
  }
}
@Dependency
public class MultiHandler {
  private final IAdapter adapter1;
  private final IAdapter adapter2;
  public FileHandler(@Bind(id = "dbAdapter") IAdapter a1, (@Bind(id = "fileAdapter") IAdapter a2) {
    this.adapter1 = a1;
    this.adapter2 = a2;
  }
}
```
> In the above examples, `DbAdapter` is default and assigned an id 'dbAdapter' and `FileAdapter` is assigned an id 'fileAdapter' (and is not default).
>  * In `FileHandler`, `FileAdapter` will be injected since the `adapter` parameter is bound to the id 'fileAdapter' using `@Bind` annotation.
>  * In `DbHandler`, `DbAdapter` will be injected since the `adapter` since it is marked as default implementation for `IAdapter` using `isDefault` field in `@Dependency` annotation. This can also be bound with it's id like it's done in `MultiHandler` example.
>  * It is also possible to inject multiple implementations of `IAdapter` using specific id in `@Bind` annotation like how it's done in `MultiHandler` example.
>
> 💡_A future version of release will have the ability to inject all the available implementations of a class/interface using a `Collection<T>` type in parameter_.

### Providing Constants
Constant values can be provided against an id in following three ways:
* ##### Using `@Constant` annotation
```java
public class DependencyProviders {
  @Constant(id = "db.userName")
  public static String env() {
    return "admin";
  }
  @Constant(id = "db.backup.path")
  public static String dbBackupPath(@Bind(id = "db.userName") String userName) {
    return String.format("/apps/db/bkp/%s/bkp.sql", userName);
  }
  @DependencyProvider
  public static DbBackupService(@Bind(id = "db.backup.path") String backupPath) {
    return new DbBackService(backupPath);
  }
  ...
}
```
> As you can see, a constant can be dependant on another constant or dependency as well. In the above example, constant with id 'db.userName' will be loaded first followed by constant 'db.backup.path' with value of 'db.userName' and at last DbBackupService will instantiated with value of 'db.backup.path'.
* ##### Through `java.lang.Properties`
  Constants can be provided through `java.lang.Properties` to sdf. The property key will be the id. Using this id the value can be bound to a parameter using the `@Bind` annotation.
* ##### Through JVM Argument
   Constant values can also be provided through jvm argument with a prefix 'sdf_consts.'. For example, if a constant or a dependency expects a constant with id `env.name` then in jvm argument it must be provided as `-Dsdf_consts.env.name=<env_value>`

### `@Bind` with variable id
Sdf supports variable id in `@Bind` annotation. If provided an id wrapped with `${` and `}` then the same will be substituted from constants.
```java
public class SomeService {
  private final IAdapter adapter;
  public SomeService(@Bind("${target.adapter}") IAdapter arg) {
    this.adapter = arg;
  }
  ...
}
```
> In the above example if value of `target.example` is set as 'fileAdapter' (through [constants](#providing-constants)) then the [FileAdapter](#injecting-a-subclass-for-its-parent-multiple-implementations) class will be injected and if the value is set as 'dbAdapter' then the [DbAdapter](#injecting-a-subclass-for-its-parent-multiple-implementations) class will be injected.

### Bootstrap (or Application entrypoint) Action
In the bootstrap or application entrypoint (our very known `main` method), base package(s) of the dependency classes (if set up at class level using `@Dependency` annotation) and the dependency provider class(es) containing all the `@DependencyProvider` or `@Constant` annotated methods (if exists) must be provided to a class (the main class for the framework) called `DependencyResolver` and `setup` called in order to enable framework initialize and resolve dependencies properly.
```java
Properties props = new Properties(); //Do the actual coding to load properties from file or elsewhere.
DependencyResolver.getInstance()
  .setProperties(props) //Not required if constants through properties is not needed
  .setDependencyProviders(HttpDependencyProviders.class, DependencyProviders.class) //Not required if not such classes exists
  .setBasePackages("simple.example.sdf.service", "complex.example.sdf.service") //Not required if dependency not set at class level
  .setup(); //This is mandatory for the framework to initialize
```

### Resolving Dependencies
`resolve` methods in `DependencyResolver` are used to resolve a dependency that is loaded through the framework (marked with `@Dependency` or `@DependencyProvider`). `DependencyResolver` must be initialized via `setup()` method before resolving dependencies.
```java
DependencyResolver resolver = DependencyResolver.getInstance();
SimpleClass simpleClass = resolver.resolve(SimpleClass.class);
IAdapter adapter = resolver.resolve("dbAdapter", IAdapter.class);
//or
FileAdapter fileAdapter = resolver.resolve("fileAdapter", FileAdapter.class);
//constants can also be resolved by it's id
String env = resolver.resolve("env.name", String.class);
```

## Constraints/Restrictions
  * If a class is initiated thourgh constructor using `@Dependency`, then there must be one public constructor present in the class. If multiple constructor is needed to present, please initiate through [Factory Method](#through-factory-method).
  * If a class is initiated through a factory method using `@Dependency` or `@DependencyProvider` then the method must be declared `static`.
  * `java.lang` classes cannot be declared as dependency (using `@Dependency` or `@DependencyProvider`). Use them as [Constants](#providing-constants) if needed.
  * If a constant is provided through JVM argument, then it must be prefixed with `sdf_consts.`. example: `-Dsdf_consts.env.name=DEV` - using this in JVM argument will add a constant value 'DEV' against id 'env.name'.
  * `DependencyResolver.setup()`(as described [here](#bootstrap-or-application-entrypoint-action)) must be called in bootstrap or application entry point to load the dependencies properly by the framework.
  * If dependencies are provided using `@DependencyProvider`, then the class(es) containing the provider methods must be set to `DependencyResolver` using `setDependencyProviders()` method before invoking `setup()`.💡If no such provider methods exist, the then no need to use `setDependencyProviders()` at all.
  * If dependencies are set at class level using `@Dependency`, then base package(s) of the classes must be set to `DependencyResolver` using `setBasePackages()` method before invoking `setup()`.💡If dependencies are not set at class level, the then no need to use `setBasePackages()` at all.

## License
[BSD 3-Clause "Revised" license](https://opensource.org/licenses/BSD-3-Clause)
