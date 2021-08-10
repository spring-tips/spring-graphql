package com.example.graphqlbasics;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.graphql.execution.RuntimeWiringConfigurer;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
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

    @Bean
    RuntimeWiringConfigurer runtimeWiringConfigurer(CustomerService customerService, ObjectMapper objectMapper) {
        return builder -> {

            builder
                    .type("Subscription", wiring -> wiring
                            .dataFetcher("customers", env ->
                                    customerService.getCustomers()
                                            .delayElements(Duration.ofSeconds(1))
                                            .map(x -> json(objectMapper, x))));

            builder
                    .type("Query", wiring -> wiring
                            .dataFetcher("customerById", env -> {
                                var id = Integer.parseInt(env.getArgument("id"));
                                return customerService.getCustomerById(id);
                            })
                    );

        };
    }


    @SneakyThrows
    private String json(ObjectMapper objectMapper, Object o) {
        return objectMapper.writeValueAsString(o);
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
