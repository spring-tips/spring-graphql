package com.example.graphql;

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
public class GraphqlApplication {

	public static void main(String[] args) {
		SpringApplication.run(GraphqlApplication.class, args);
	}
}


@GraphQlController
class CustomerController {


	private final CustomerRepository repository;

	CustomerController(CustomerRepository repository) {
		this.repository = repository;
	}

	@SchemaMapping
	Mono<CustomerProfile> profile(Customer customer) {
		return Mono
			.just(new CustomerProfile(customer.id(), (int) (Math.random() * 1000)))
			.delayElement(Duration.ofSeconds(1));
	}

	@SubscriptionMapping
	Flux<CustomerEvent> customerEvents(@Argument int id) {
		return this.repository
			.findById(id)
			.flatMapMany(customer -> {
				var stream = Stream
					.generate(() -> new CustomerEvent(customer, Math.random() >= .5 ? CustomerEventType.DELETED : CustomerEventType.UPDATED));
				return Flux.fromStream(stream);
			})
			.delayElements(Duration.ofSeconds(1))
			.take(10);
	}

	@MutationMapping
	Mono<Customer> addCustomer(@Argument String name) {
		return this.repository.save(new Customer(null, name));
	}

	@QueryMapping
	Flux<Customer> customers() {
		return this.repository.findAll();
	}

}

record CustomerEvent(Customer customer, CustomerEventType event) {
}

enum CustomerEventType {UPDATED, DELETED}


record CustomerProfile(Integer customerId, Integer id) {
}

interface CustomerRepository extends ReactiveCrudRepository<Customer, Integer> {
}

record Customer(@Id Integer id, String name) {
}