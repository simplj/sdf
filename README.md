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
      * [Instantiating a class as Dependency using `@Dependency` through constructor](#instantiating-a-class-as-dependency-using-dependency-through-constructor)
      * [Instantiating a class as Dependency using `@Dependency` through factory method](#instantiating-a-class-as-dependency-using-dependency-through-factory-method)
      * [Injecting a subclass for it's parent (Singple Implementation)](#injecting-a-subclass-for-its-parent-single-implementation)
      * [Injecting a subclass for it's parent (Multiple Implementations)](#injecting-a-subclass-for-its-parent-multiple-implementations)
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

### Instantiating a class as Dependency using `@Dependency` through constructor
```java
@Dependency
public class SimpleClass {
  ...
}
```
> By adding the `@Dependency` to the class enables it to get loaded by the framework as singleton instance. 💡If singleton behavior is not expected just modify the annotation to `@Dependency(singleton=false)`.
```java
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

### Instantiating a class as Dependency using `@Dependency` through factory method
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
> In the above example, the `FactoryClass` is instantiated using the `getInstance` method. Please note that the factory method must be static.
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
>  * assigning an `id` for the implementations and `@Bind`ing with specific id in parameter as desired.

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
> 💡 _A future version of release will have the ability to inject all the available implementations of a class/interface using a `Collection<T>` type in parameter_.

## License
[BSD 3-Clause "Revised" license](https://opensource.org/licenses/BSD-3-Clause)
