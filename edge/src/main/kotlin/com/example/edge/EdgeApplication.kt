package com.example.edge

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.graphql.data.method.annotation.GraphQlController
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.graphql.data.method.annotation.SchemaMapping
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.retrieveFlux
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToFlux
import reactor.core.publisher.Mono

@SpringBootApplication
class EdgeApplication {

    @Bean
    fun http(wb: WebClient.Builder) = wb.build()

    @Bean
    fun rsocket(rc: RSocketRequester.Builder) = rc.tcp("localhost", 8282)

}

fun main(args: Array<String>) {
    runApplication<EdgeApplication>(*args)
}


@GraphQlController
class CrmGraphqlController(
    private val crm: CrmClient
) {

    @QueryMapping
    fun customers() = this.crm.customers()

    @SchemaMapping
    fun orders(customer: Customer) = this.crm.ordersFor(customer.id)


}

@Component
class CrmClient(
    private val http: WebClient,
    private val rsocket: RSocketRequester
) {

    fun ordersFor(customerId: Int) = this.rsocket
        .route("orders.{cid}", customerId)
        .retrieveFlux<Order>()

    fun customers() = this.http.get()
        .uri("http://localhost:8080/customers")
        .retrieve()
        .bodyToFlux<Customer>()

    fun customerOrders() =
        this.customers()
            .flatMap { c ->
                Mono.zip(
                    Mono.just(c),
                    ordersFor(c.id).collectList()
                )
            }
            .map { CustomerOrder(it.t1, it.t2) }

}

data class CustomerOrder(
    val customer: Customer,
    val orders: MutableList<Order>
)

data class Order(val id: Int, val customerId: Int)
data class Customer(val id: Int, val name: String)