package com.example.graphqlbasics;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.nativex.hint.NativeHint;
import org.springframework.nativex.hint.ResourceHint;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;


@ResourceHint(patterns = {
//        "^graphql/schema.graphqls",
        "^graphql/.*"
//    ".*/graphql.*graphqls$",
//    "graphql/.*.graphqls$"
})
@SpringBootApplication
public class GraphqlBasicsApplication {

    public static void main(String[] args) {
        SpringApplication.run(GraphqlBasicsApplication.class, args);
    }

}

@GraphQlController
@RequiredArgsConstructor
class CustomerGraphqlController {

    private final CustomerService customerService;


    @SchemaMapping
    Mono<CustomerProfile> profile(Customer customer) {
        return customerService.getCustomerProfileFor(customer.getId());
    }

    @QueryMapping
    Flux<Customer> customers() {
        return customerService.getCustomers();
    }

    /*
      query {
       customerById(id:9) {
        id,name
       }
      }
     */
    @QueryMapping
    Mono<Customer> customerById(@Argument Integer id) {
        return customerService.getCustomerById(id);
    }

    @SubscriptionMapping
    Flux<CustomerUpdate> customerUpdates(@Argument Integer id) {
        return customerService.getCustomerUpdatesStreamFor(id);
    }

    /*
    mutation {
      addCustomer (input : { name :"Bob"}) {
        id , name
      }
    }
     */
    @MutationMapping
    Mono<Customer> addCustomer(@Argument CustomerInput input) {
        return customerService.addCustomer(input.getName());
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class CustomerInput {
    private String name;
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
    private final AtomicInteger id = new AtomicInteger();

    CustomerService() {
        List.of("Dr. Syer", "Yuxin", "Stéphane", "Olga", "Madhura", "Violetta", "Mark")
                .forEach(this::doAddCustomer);
    }

    Mono<Customer> addCustomer(String name) {
        return Mono.just(doAddCustomer(name));
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

    private Customer doAddCustomer(String name) {
        var id = this.id.incrementAndGet();
        this.db.put(id, new Customer(id, name));
        return this.db.get(id);
    }
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class Customer {
    private Integer id;
    private String name;
}
