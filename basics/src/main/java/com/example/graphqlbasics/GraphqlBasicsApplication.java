package com.example.graphqlbasics;

import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.schema.idl.RuntimeWiring;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * @author Josh Long
 */
@SpringBootApplication
public class GraphqlBasicsApplication {

    public static void main(String[] args) {
        SpringApplication.run(GraphqlBasicsApplication.class, args);
    }

}

@Log4j2
@Component
class EmployeeDataWiringManager implements RuntimeWiringConfigurer {

    private final CustomerService customerService;
    private final ObjectMapper om;

    EmployeeDataWiringManager(CustomerService customerService, ObjectMapper om) {
        this.customerService = customerService;
        this.om = om;
    }

    @Override
    public void configure(RuntimeWiring.Builder builder) {

        builder
                .type("Subscription", wiring -> wiring
                        .dataFetcher("customers", env -> customerService.getCustomers().map(this::json)));

        builder
                .type("Query", wiring -> wiring
                        .dataFetcher("customerById", env -> {
                            var id = Integer.parseInt(env.getArgument("id"));
                            return customerService.getCustomerById(id);
                        })
                );
    }

    @SneakyThrows
    private String json(Object o) {
        return om.writeValueAsString(o);
    }


}

@Service
class CustomerService {

    private final List<Customer> customers;

    CustomerService() {
        var id = new AtomicInteger();
        this.customers = List
                .of("Dr. Syer", "Stéphane", "Yuxin", "Olga", "Madhura", "Violetta", "Mark")
                .stream()
                .map(name -> new Customer(id.incrementAndGet(), name))
                .collect(Collectors.toList());
    }

    Mono<Customer> getCustomerById(Integer id) {
        Optional<Customer> first = this.customers.stream().filter(c -> c.getId().equals(id)).findFirst();
        return first.map(Mono::just).orElseGet(Mono::empty);
    }

    Flux<Customer> getCustomers() {
        return Flux.fromIterable(this.customers);
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Customer {
    private Integer id;
    private String name;
}
