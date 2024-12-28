package demo.sdf;

import com.simplj.di.core.DependencyResolver;
import com.simplj.di.core.DependencyResolverConfig;
import com.simplj.di.core.DependencyResolverFactory;
import com.simplj.di.core.TypeClass;
import demo.sdf.model.Book;
import demo.sdf.model.Stock;
import demo.sdf.service.DemoService;
import greet.sdf.Greeter;
import greet.sdf.service.AuditService;
import greet.sdf.service.GreeterService;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        System.setProperty("sdf_consts.id.generator", "uuid");
        init();
        initGreet();
        DependencyResolver resolver = DependencyResolverFactory.defaultResolver();
        DependencyResolver greet = DependencyResolverFactory.resolver("greet");

        greet.invoke(GreeterService.class, "greet", "Chester", "Bennington");
        greet.invoke(GreeterService.class, "lastNameGreet", "Mike", "Shinoda");

        AuditService audit = greet.resolve(AuditService.class);
        audit.printAuditLogs();
    }

    private static void createBook(DependencyResolver resolver) throws IOException, ClassNotFoundException {
        TypeClass<DemoService<Book>> type = new TypeClass<DemoService<Book>>() {
        };
        DemoService<Book> service = resolver.resolve(type);

        System.out.println("\nCreating a book record using service '" + service.getClass().getName() + "'...");
        Book book = new Book("Head First Java");
        book.addAttribute("Author", "Kathy Sierra");
        book.addAttribute("Year", "2003");

        String id = service.create(book);
        System.out.println("Book record created with id: " + id);
        System.out.println("Fetching book for id: " + id);
        Book b = service.fetch(id);
        System.out.println("Fetched Book: " + b);
    }

    private static void createStock(DependencyResolver resolver) throws IOException, ClassNotFoundException {
        TypeClass<DemoService<Stock>> type = new TypeClass<DemoService<Stock>>() {
        };
        DemoService<Stock> service = resolver.resolve(type);

        System.out.println("\nCreating a stock record using service '" + service.getClass().getName() + "'...");
        Stock stock = new Stock("xyz.demo");
        stock.addAttribute("High", "80.10");
        stock.addAttribute("Low", "78.12");

        String id = service.create(stock);
        System.out.println("Stock record created with id: " + id);
        System.out.println("Fetching stock for id: " + id);
        Stock s = service.fetch(id);
        System.out.println("Fetched Stock: " + s);
    }

    private static void init() {
        System.out.println("Configuring default resolver...");
        DependencyResolverConfig config = DependencyResolverConfig.builder()
                .setBasePackages("demo.sdf")
                .setDependencyProviders(new DependencyProviders(), new ConstantProviders())
                .build();
        DependencyResolverFactory.configureDefaultResolver(config);
        System.out.println("=================================================================");
    }

    private static void initGreet() {
        System.out.println("Configuring greet resolver...");
        DependencyResolverConfig config = DependencyResolverConfig.builder().setBasePackages("greet.sdf").withDynamicMethods().build();
        DependencyResolverFactory.configureResolver("greet", config);
        System.out.println("=================================================================");
    }
}
