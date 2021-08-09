package com.example.graphqlbasics;

import graphql.schema.idl.RuntimeWiring;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.graphql.boot.RuntimeWiringBuilderCustomizer;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

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

@Component
class EmployeeDataWiringManager implements RuntimeWiringBuilderCustomizer {

    private final EmployeeService employeeService;
    private final CustomerService customerService;

    EmployeeDataWiringManager(
            EmployeeService employeeService,
            CustomerService customerService) {
        this.employeeService = employeeService;
        this.customerService = customerService;
    }

    @Override
    public void customize(RuntimeWiring.Builder builder) {


        // todo figure out how to handle a mutation
        // todo figure out how to handle a query
        // todo figure out security


        builder
                .type("Query",
                        wiring -> wiring
                                .dataFetcher("customers", environment -> {
                                    Map<String, Object> arguments = environment.getArguments();
                                    arguments.forEach((k, v) -> System.out.println(k + '=' + v));
                                    return customerService.getCustomers();
                                })
                                .dataFetcher("employeeById", e -> {
                                    var id = (Integer) e.getArgument("id");
                                    return employeeService.getEmployeesById( (id));
                                })
                                .dataFetcher("employees",
                                        env -> employeeService.getEmployees()));


    }
}

@Service
class CustomerService {


    private final List<Customer> customers = List
            .of(new Customer(2, "Carol Danvers"));

    Collection<Customer> getCustomers() {
        return this.customers;
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
