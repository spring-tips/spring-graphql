package graphql.gateway;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.annotation.Id;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.graphql.data.method.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.stream.Stream;

@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

}

@GraphQlController
class CustomerGraphqlController {

    private final CustomerRepository repository;

    CustomerGraphqlController(CustomerRepository repository) {
        this.repository = repository;
    }

    @QueryMapping
    Flux<Customer> customers() {
        return this.repository.findAll();
    }

    @MutationMapping
    Mono<Customer> addCustomer(@Argument String name) {
        return this.repository.save(new Customer(null, name));
    }

    @SubscriptionMapping
    Flux<CustomerEvent> customerEvents(@Argument("id") Integer customerId) {
        return this.repository
                .findById(customerId)
                .flatMapMany(customer -> {
                    var stream = Stream
                            .generate(() -> new CustomerEvent(customer, Math.random() > .5 ? CustomerEventType.DELETED : CustomerEventType.MODIFIED));
                    return Flux.fromStream(stream);
                })
                .delayElements(Duration.ofSeconds(1))
                .take(10);

    }

}

interface CustomerRepository extends ReactiveCrudRepository<Customer, Integer> {
}


@Data
@AllArgsConstructor
@NoArgsConstructor
class Customer {

    @Id
    private Integer id;

    private String name;
}

enum CustomerEventType {DELETED, MODIFIED}

@Data
class CustomerEvent {

    private final Customer customer;
    private final String event;

    CustomerEvent(Customer customer, CustomerEventType customerEventType) {
        this.customer = customer;
        this.event = customerEventType.name();
    }
}