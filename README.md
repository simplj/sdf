# [S]imple [D]ependency Injection [F]ramework

* Simple
  > As simple as adding one single annotation in a class
* Lightweight
  > less then 50 kb of size
* Powerful
  > We hope you will agree on this after getting introduced with the framework

## Dependency

```
<dependency>
    <groupId>com.simplj.di</groupId>
    <artifactId>sdf</artifactId>
    <version>0.4</version>
</dependency>
```

## Usage
- Adding a class as Dependency using `@Dependency`
```java
@Dependency
public class SimpleClass {
  ...
}
```
> By adding the `@Dependency` to the class enables it to get loaded by the framework as singleton instance. If singleton behavior is not expected just modify the annotation to `@Dependency(singleton=false)`.
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
> Dependency `SimpleClass` will be injected to `DependantClass` will be injected by the framework since `SimpleClass` is marked as `@Dependency`.

## License
[BSD 2-Clause "Simplified" License](https://opensource.org/licenses/bsd-license.html)
