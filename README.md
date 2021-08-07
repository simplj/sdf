# [S]imple [D]ependency Injection [F]ramework

* Simple
  > As simple as adding one single annotation in a class
* Lightweight
  > A single independent jar of ~70 kb of size (does not require any other dependency jars)
* Powerful
  > Lets' get introduced to the framework to know it's capabilities

Table of contents
=================
<!--ts-->
   * [Maven Dependency](#maven-dependency)
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
      * [`@Bind` with variable id](#bind-with-variable-id)
      * [Bootstrap (or Application entrypoint) Action](#bootstrap-or-application-entrypoint-action)
      * [Resolving Dependencies](#resolving-dependencies)
      * [Generic Type Dependencies](#generic-type-dependencies)
      * [Dependencies with TypeVariables](#dependencies-with-typevariables)
      * [Subtypes Dependencies](#subtypes-dependencies)
      * [Dynamic/Runtime Dependencies](#dynamicruntime-dependencies)
   * [Constraints/Restrictions](#constraintsrestrictions)
   * [Suggestions/Feedback](https://github.com/simplj/sdf/discussions)
   * [Report an Issue](https://github.com/simplj/sdf/issues)
   * [License](#License)
   * [Previous Versions](#previous-versions)
<!--te-->

## Maven Dependency

```
<dependency>
    <groupId>com.simplj.di</groupId>
    <artifactId>sdf</artifactId>
    <version>1.1</version>
</dependency>
```
[Maven Repository](https://mvnrepository.com/artifact/com.simplj.di/sdf/latest)

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
> By adding the `@Dependency` to the class enables it to get loaded by the framework as singleton instance. 💡If singleton behavior is not expected just modify the annotation to `@Dependency(singleton=false)`. When `singleton` is set as `false` for a class, then, a new and different instance will be provided for each time an instance of the class is needed (a class depends on this class or requesting for an instance manually using the [resolve](#resolving-dependencies) method).
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
Dependencies can also be provided in a separate class through a `static` method using `@DependencyProvider` annotation. **Classes from `java.lang` package cannot be used as a dependency**. 💡Check [Providing Constants](#providing-constants) section to see how to use `java.lang` classes as constants instead. Like `@Dependency`, options `id`, `singleton` and `isDefault` can also be used for their respective purposes.
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
> 💡 _Please note that if a class is annotated with `@Dependency` and is also configured using `@DependencyProvider` OR if two or more methods annotated with `@DependencyProvider` return the same type, then sdf will throw an `ambiguity error`._

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
  public FileHandler(@Bind(id = "dbAdapter") IAdapter a1, @Bind(id = "fileAdapter") IAdapter a2) {
    this.adapter1 = a1;
    this.adapter2 = a2;
  }
}
```
> In the above example, `DbAdapter` is default and assigned an id 'dbAdapter' and `FileAdapter` is assigned an id 'fileAdapter' (and is not default).
>  * In `FileHandler`, `FileAdapter` will be injected since the `adapter` parameter is bound to the id 'fileAdapter' using `@Bind` annotation.
>  * In `DbHandler`, `DbAdapter` will be injected since it is marked as default implementation for `IAdapter` using `isDefault` field in `@Dependency` annotation. This can also be bound with it's id like it's done in `MultiHandler` example.
>  * It is also possible to inject multiple implementations of `IAdapter` using specific id in `@Bind` annotation like how it's done in `MultiHandler` example.

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
  public static DbBackupService dbBackupService(@Bind(id = "db.backup.path") String backupPath) {
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

### Generic Type Dependencies
SDF resolves Dependencies with generic types. Consider the below example:
```java
interface FoodCategory {}
class Carnivorous implements FoodCategory {}
class Herbivorous implements FoodCategory {}
class Omnivorous implements FoodCategory {}

abstract class Animal<T> {}

@Dependency
class Deer extends Animal<Herbivorous> {}
@Dependency
class Boar extends Animal<Omnivorous> {}
@Dependency
class Tiger extends Animal<Carnivorous> {
  private Animal<Herbivorous> food;
  public Tiger(Animal<Herbivorous> food) {
    this.food = food;
  }
@Dependency
class Lion extends Animal<Carnivorous> {
  private List<Animal<? extends FoodCategory>> food;
  public Lion(@Subtypes List<Animal<? extends FoodCategory>> food) {
    this.food = food;
  }
}
```
> In the above example,
>  * while initializing Tiger, Deer instance will be passed to the constructor. If there is another dependency which extends type `Animal<Herbivorous>`, then the framework will complain about ambiguity.
>  * while initializing Lion, instances of Deer, Boar and Tiger will be passed to the constructor, since Lion is dependent on all [`@Subtypes`](#subtypes-dependencies) of type `Animal<? extends FoodCategory>`. SDF is intelligent enough to deduce type `? extends FoodCategory` to types `Carnivorous`, `Herbivorous` and `Omnivorous`, and similarly type `Animal<? extends FoodCategory>` is deduced to `Animal<Carnivorous>`, `Animal<Herbivorous>` and `Animal<Omnivorous>`. Hence all the instances of Dear, Boar and Tiger will be provided to Lion constructor. Also, to avoid `Circular Dependency`, SDF will not try to provide instance of Lion to the constructor of Lion iteself even though Lion is a `Animal<Carnivorous>`.
>  
> SDF supports generics with bounded types as well. For example:
>  * `Upper Bound Wildcard` - Used in `Lion` class constructor in the above example
>  * `Lower Bound Wildcard` - An instance of `Number` can be passed to `? super Integer`
>  * `Upper Bound TypeVariable` - An instance of `Integer` can be passed to `T extends Number`.
>  
> To resolve instance of type `Animal<Herbivorous>` manually (using the `resolve` method), the type needs to be wrapped in `TypeClass`. Since, `Animal<Herbivoruos>.class` is not possible in java, the framework's `TypeClass<T>` helps wrapping the generic type. Below example shows how to resolve type `Animal<Herbivorous>` manually.
```java
TypeClass<Animal<Herbivorous>> herbivorousType = new TypeClass<Animal<Herbivorous>> {};
Animal<Herbivorous> herbivorous = resolver.resolve(herbivorousType);
```

### Dependencies with TypeVariables
SDF supports using TypeVariables in dependencies as well but in a restrictive manner i.e. TypeVariables can only be used either with [`@Bind`](#bind-with-variable-id) or with [`@RtProvided`](#dynamicruntime-dependencies). (_The reason for this very obvious i.e. to void ambiguity for naked TypeVariables. What I mean is this - if a class is dependent on a type `T` and if there are N number of dependencies available in SDF, then all N instances can be provided to the `T` - resulting in ambiguity error._)
Lets look at the following example
```java
public static class Student {}
public static class Child extends Student {}
public static class School<T extends Student> {
    private final Set<T> students;

    public School(Set<T> students) {
        this.students = students;
    }

    public Set<T> getStudents() {
        return students;
    }
}

public class DependencyProviders {
  @DependencyProvider(id = "child")
  public static Child child() {
    return new Child();
  }
  @DependencyProvider
  public static <T> Set<T> sampleStudentSet(@Bind(id = "child") T value) {
    return Collections.singleton(value);
  }
  @DependencyProvider
  public static <T extends Student> School<T> someSchool(Set<T> students) {
    return new School<>(students);
  }
  ...
}
```
> I wonder if there is any school with one student only, but let's imagine there is for the sake of this documentation 😄
> 
> In the above example, `Child` instance will be provided to `sampleStudentSet` as it is bound with id. Because of this the type variable `T` in `sampleStudentSet` will be substituted to `Child` making the return type of `sampleStudentSet` to `Set<Child>`. And naturally, the return type `Set<Child>` will be provided to `someSchool` resulting our unique school with only one student 😄
> 
> 💡 _Naked type variables (in return type or in argument) is not allowed. TypeVariables must only be used either with `@Bind` or with `@RtProvided`._

### Subtypes Dependencies
There can be a situation where a class needs all the classes which are subtypes of class A (collection of instances that are subtype of A). SDF has the ability to inject all the available implementations of a class/interface as a `List<A>` or a `Map<String, A>` type. All is needed is to just mark the `List` or `Map` type in parameter with the annotation `@SubTypes`. Consider the below example:
```java
interface Notifier {}
@Dependency
class NotifierSerivceA implements Notifier
@Dependency
class NotifierSerivceB implements Notifier
@Dependency
class NotifierSerivceC implements Notifier

@Dependency
class EventPublisher {
  private List<Notifier> notifiers;
  public EventPublisher(@SubTypes List<Notifier> notifiers) {
    this.notifiers = notifiers;
  }
}
```
> In the above example, `EventPublisher` expects all the implementations of interface `Notifier`. SDF will provide `NotifierServiceA`, `NotiferServiceB` and `NotifierServiceC` to the parameter `notifiers` in `EventPublisher` constructor as it is of type `List<Notifier>` and is marked with `@SubTypes`. Generics can also be applied on this. Consider the below extended example:
```java
interface Notifier<A> {}
@Dependency
class NotifierSerivceA implements Notifier<String>
@Dependency
class NotifierSerivceB implements Notifier<String>
@Dependency
class NotifierSerivceC implements Notifier<Integer>

@Dependency
class EventPublisher {
  private List<Notifier<String>> notifiers;
  public EventPublisher(@SubTypes List<Notifier<String>> notifiers) {
    this.notifiers = notifiers;
  }
}
```
> In the above example, SDF will only provide `NotifierServiceA` and `NotiferServiceB` to the parameter `notifiers` in `EventPublisher` constructor as it expects subtypes of `Notifier<String>`.
> 
> 💡 _Currently, only `List` and `Map` types are supported for Subtypes._

### Dynamic/Runtime Dependencies
SDF supports resolving dependencies with parameters provided at the runtime. This helps initializing a class instance with different runtime values. The parameter which will be supplied at the runtime needs to be annotated with `@RtProvided` (i.e. short of _RuntimeProvided_) with an id. At the time of resolving the class, `dynamicResolve()` needs to be used with a map having the parameter value against the id as key.
```java
@Dependency
public class UserAnalysisService {
  private final IAdapter adapter;
  private final User user;
  
  public UserService(IAdapter adapter, @RtProvided(id = "user") User user) {
    this.adapter = adapter;
    this.user = user;
  }
  
  public void performAnalysis() {
    ...
  }
}
```
In the above example, `UserAnalysisService` takes an `adapter` and an `user` instance which is provided at the runtime. Now to initialize, use `dynamicResolve()` with a map having the parameter value against the key "user".
```java
  User user = getUser(); //Logic to get the user from a runtime flow.
  UserAnalysisService service = resolver.dynamicResolve(UserAnalysisService.class, Collections.singletonMap("user", user);
```
> 💡 _Class with `@RtProvided` parameters cannot be used as a dependency for another class_\
> 💡 _Class with `@RtProvided` parameters cannot be used as a singleton (for obvious reason)_

## Constraints/Restrictions
  * If a class is initiated thourgh constructor using `@Dependency`, then there must be one public constructor present in the class. If multiple constructor is needed to present, please initiate through [Factory Method](#through-factory-method).
  * If a class is initiated through a factory method using `@Dependency` or `@DependencyProvider` then the method must be declared `static`.
  * `@Dependency` annotation cannot be used on Abstract class or interface. Only concrete classes can be marked as `@Dependency`.
  * `java.lang` classes cannot be declared as dependency (using `@Dependency` or `@DependencyProvider`). Use them as [Constants](#providing-constants) if needed.
  * If a constant is provided through JVM argument, then it must be prefixed with `sdf_consts.`. example: `-Dsdf_consts.env.name=DEV` - using this in JVM argument will add a constant value 'DEV' against id 'env.name'.
  * When using variable id in `@Bind`, it can only refer to constants of type `java.lang.String`.
  * `DependencyResolver.setup()`(as described [here](#bootstrap-or-application-entrypoint-action)) must be called in bootstrap or application entry point to load the dependencies properly by the framework.
  * If dependencies are provided using `@DependencyProvider`, then the class(es) containing the provider methods must be set to `DependencyResolver` using `setDependencyProviders()` method before invoking `setup()`.💡If no such provider methods exist, then no need to use `setDependencyProviders()` at all.
  * If dependencies are set at class level using `@Dependency`, then base package(s) of the classes must be set to `DependencyResolver` using `setBasePackages()` method before invoking `setup()`.💡If dependencies are not set at class level, then no need to use `setBasePackages()` at all.
  * Naked type variables (in return type or in argument) is not allowed. TypeVariables must only be used either with `@Bind` or with `@RtProvided`..
  * Only `List` and `Map` types are supported for Subtypes.
  * Class with `@RtProvided` parameters cannot be used as a dependency for another class.
  * Class with `@RtProvided` parameters cannot be used as a singleton.

## License
[BSD 3-Clause "Revised" license](https://opensource.org/licenses/BSD-3-Clause)

## Previous Versions
  * [Version 1.0](https://github.com/simplj/sdf/tree/1.0)
