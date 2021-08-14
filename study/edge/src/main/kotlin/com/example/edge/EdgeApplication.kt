package com.example.edge

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.ApplicationEvent
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.ApplicationListener
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.graphql.data.method.annotation.*
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.SynchronousSink
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

@SpringBootApplication
class EdgeApplication


fun main(args: Array<String>) {
    runApplication<EdgeApplication>(*args)
}


@Configuration
class CrmEventConfiguration {

    @Bean
    fun eventFlux() = EventConsumer<CustomerAdditionEvent>()
}

data class CustomerAddition(val name: String)

@GraphQlController
class CrmGraphqlController(private val crm: CrmClient) {

    @QueryMapping
    fun customers() = this.crm.getCustomers()

    @MutationMapping
    fun addCustomer(@Argument customerAddition: CustomerAddition) =
        this.crm.addCustomer(customerAddition.name)

    @SubscriptionMapping
    fun customerAdditionEvents() = this.crm.getCustomerNotifications()

}

@Component
class CrmClient(
    private val eventConsumer: EventConsumer<CustomerAdditionEvent>,
    private val aep: ApplicationEventPublisher
) {

    private val db = mutableMapOf<Int, Customer>()
    private val id = AtomicInteger()
    private val flux: Flux<CustomerAdditionEvent> = Flux.generate(this.eventConsumer)

    fun getCustomerNotifications(): Flux<CustomerAdditionEvent> = this.flux

    fun addCustomer(name: String): Customer {
        val nextId = this.id.incrementAndGet()
        val c = Customer(nextId, name)
        this.db[nextId] = c
        this.aep.publishEvent(CustomerAdditionEvent(customer = c))
        return this.db[nextId]!!
    }

    fun getCustomerById(id: Int): Customer = this.db[id]!!

    fun getCustomers() = this.db.values


}


class EventConsumer<T : ApplicationEvent> : Consumer<SynchronousSink<T>>,
    ApplicationListener<T> {

    private val sinkRef = AtomicReference<SynchronousSink<T>>()

    override fun accept(t: SynchronousSink<T>) {
        this.sinkRef.set(t)
    }

    override fun onApplicationEvent(event: T) {
        this.sinkRef.get().next(event)
    }

}

class CustomerAdditionEvent(customer: Customer) : ApplicationEvent(customer)

data class Customer(val id: Int, val name: String)


