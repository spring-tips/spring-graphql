package graphql.gateway;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.graphql.data.method.annotation.*;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }

}


@Data
@AllArgsConstructor
@NoArgsConstructor
class Customer {
    private Integer id;
    private String name;
}

class CustomerAddEvent
        extends ApplicationEvent {

    @Override
    public Customer getSource() {
        return (Customer) super.getSource();
    }

    public CustomerAddEvent(Customer source) {
        super(source);
    }
}

@Log4j2
class EventConsumer<X extends ApplicationEvent>
        implements Consumer<FluxSink<X>>, ApplicationListener<X> {

    private final AtomicReference<FluxSink<X>> sink = new AtomicReference<>();

    @Override
    public void onApplicationEvent(X x) {
        if (null != this.sink.get()) {
            this.sink.get().next(x);
            log.info("new event:" + x.getSource());
        } else {
            log.info("ignoring event:" + x.getSource());
        }
    }

    @Override
    public void accept(FluxSink<X> cas) {
        log.info("setting FluxSink<X>");
        this.sink.set(cas);
    }
}

@Configuration
class CrmEventConfiguration {

    @Bean
    EventConsumer<CustomerAddEvent> customerAddEventEventConsumer() {
        return new EventConsumer<>();
    }
}


@GraphQlController
class CrmGraphqlController {

    private final CrmClient crm;

    CrmGraphqlController(CrmClient crm) {
        this.crm = crm;
    }

    @QueryMapping
    Flux<Customer> customers() {
        return this.crm.getCustomers();
    }

    @QueryMapping
    Mono<Customer> customerById(Integer id) {
        return this.crm.getCustomerById(id);
    }

    @MutationMapping
    Mono<Customer> addCustomer(@Argument String name) {
        return this.crm.addCustomer(name);
    }

    @SubscriptionMapping
    Flux<CustomerAddEvent> customerNotifications() {
        return this.crm.getCustomerSubscription();
    }
}


@Log4j2
@Component
class CrmClient {

    private final Flux<CustomerAddEvent> flux;
    private final ApplicationEventPublisher aep;
    private final AtomicInteger id = new AtomicInteger();
    private final Map<Integer, Customer> db = new ConcurrentHashMap<>();

    CrmClient(ApplicationEventPublisher aep, EventConsumer<CustomerAddEvent> flux) {
        this.flux = Flux.create(flux);
        this.aep = aep;
    }

    Flux<CustomerAddEvent> getCustomerSubscription() {
        return this.flux  ;
    }

    Mono<Customer> addCustomer(String name) {
        var nextId = this.id.incrementAndGet();
        var c = new Customer(nextId, name);
        this.db.put(nextId, c);
        this.aep.publishEvent(new CustomerAddEvent(c));
        log.info ( "publishing a new event " );
        return Mono.just(this.db.get(nextId));
    }

    Mono<Customer> getCustomerById(Integer id) {
        return Mono.just(this.db.get(id));
    }

    Flux<Customer> getCustomers() {
        return Flux.fromIterable(this.db.values());
    }
}

