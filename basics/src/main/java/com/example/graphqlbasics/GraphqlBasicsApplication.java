package com.example.graphqlbasics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.idl.RuntimeWiring;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.util.ReflectionUtils;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@SpringBootApplication
public class GraphqlBasicsApplication {

    public static void main(String[] args) {
        SpringApplication.run(GraphqlBasicsApplication.class, args);
    }

}

@Log4j2
@Component
class EmployeeDataWiringManager
        implements RuntimeWiringConfigurer {

    private final EmployeeService employeeService;

    private final CustomerService customerService;
    private final ObjectMapper om ;

    EmployeeDataWiringManager(
            EmployeeService employeeService,
            CustomerService customerService, ObjectMapper om) {
        this.employeeService = employeeService;
        this.customerService = customerService;
        this.om = om;
    }


    private static Flux<Customer> getGreetingsStream() {
        var id = new AtomicInteger();
        return Flux
                .just("Hi", "Bonjour", "Hola", "Ciao", "Zdravo")
                .map(name -> new Customer(id.incrementAndGet(), name))
                .delayElements(Duration.ofSeconds(1));
    }

    private static String from(Object o, ObjectMapper om) {
        try {
            return om.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            ReflectionUtils.rethrowRuntimeException(e);
        }
        return null ;
    }

    @Override
    public void configure(
            RuntimeWiring.Builder builder) {

        builder
                .type("Subscription", wiring -> wiring.dataFetcher("customers",
                        env -> getGreetingsStream()
                                .map(c -> from(c, om))
                ));

        builder
                .type("Query",
                        wiring -> wiring
                                .dataFetcher("customer", env -> customerService.get())
                                .dataFetcher("employeeById", env -> {
                                    var id = Integer.parseInt(env.getArgument("id"));
                                    return employeeService.getEmployeesById(id).getName();
                                })
                                .dataFetcher("employees", env -> employeeService.getEmployees()));

    }
}

@Service
class CustomerService {


    private final List<Customer> customers = List
            .of(new Customer(2, "Carol Danvers"));

    Collection<Customer> getCustomers() {
        return this.customers;
    }

    Customer get() {
        return this.customers.iterator().next();
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Employee {
    private Integer id;
    private String name;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Customer {
    private Integer id;
    private String name;
}

@Service
class EmployeeService {

    private final AtomicInteger id = new AtomicInteger();

    private final Map<Integer, Employee> db = List
            .of("A", "B", "C", "D", "E")
            .stream()
            .map(name -> new Employee(this.id.incrementAndGet(), name))
            .collect(Collectors.toMap(Employee::getId, employee -> employee));

    Employee getEmployeesById(Integer id) {
        return this.db.get(id);
    }

    Flux<Employee> getEmployees() {
        return Flux.fromIterable(this.db.values());
    }
}
