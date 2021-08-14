package com.example.graphqlbasics;

import graphql.schema.DataFetchingEnvironment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SpringBootApplication
public class GraphqlBasicsApplication {

    public static void main(String[] args) {
        SpringApplication.run(GraphqlBasicsApplication.class, args);
    }

    @Bean
    RuntimeWiringConfigurer runtimeWiringConfigurer(CustomerService customerService) {
        return builder -> {

            builder
                    .type("Customer", wiring -> wiring
                            .dataFetcher("profile", env -> customerService.getCustomerProfileFor(getId(env))));
            builder
                    .type("Subscription", wiring -> wiring
                            .dataFetcher("customerUpdates", env -> customerService.getCustomerUpdatesStreamFor(getId(env))));
            builder
                    .type("Query", wiring -> wiring
                            .dataFetcher("customers", env -> customerService.getCustomers())
                            .dataFetcher("customerById", env -> customerService.getCustomerById(getId(env))));
        };
    }

    private int getId(DataFetchingEnvironment e) {
        return Integer.parseInt(e.getArgument("id"));
    }

}

@Data
@AllArgsConstructor
@NoArgsConstructor
class CustomerUpdate {
    private Customer customer;
    private String event;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class CustomerProfile {
    private Integer id;
}

@Service
class CustomerService {

    private final Map<Integer, Customer> db = new ConcurrentHashMap<>();

    CustomerService() {
        var id = new AtomicInteger();
        var customers = Stream.of("Dr. Syer", "Stéphane", "Yuxin", "Olga", "Madhura", "Violetta", "Mark")
                .map(name -> new Customer(id.incrementAndGet(), name))
                .collect(Collectors.toList());
        customers.forEach(c -> this.db.put(c.getId(), c));
    }

    Flux<CustomerUpdate> getCustomerUpdatesStreamFor(Integer id) {
        var customer = this.db.get(id);
        return Flux
                .fromStream(Stream.generate(() -> new CustomerUpdate(customer, Math.random() >= .5 ? "CREATED" : "DELETED")))
                .delayElements(Duration.ofSeconds(1))
                .take(10);
    }

    Mono<Customer> getCustomerById(Integer id) {
        return Mono.just(this.db.get(id));
    }

    Flux<Customer> getCustomers() {
        return Flux.fromIterable(this.db.values());
    }

    Mono<CustomerProfile> getCustomerProfileFor(Integer id) {
        return Mono.just(new CustomerProfile(this.db.get(id).getId()));
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Customer {
    private Integer id;
    private String name;
}
