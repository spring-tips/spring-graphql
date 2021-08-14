package com.example.orders

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.messaging.handler.annotation.DestinationVariable
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux
import java.util.concurrent.ConcurrentHashMap

@SpringBootApplication
class OrdersApplication

fun main(args: Array<String>) {
    runApplication<OrdersApplication>(*args)
}

@Controller
class OrderRsocketController {

    private val db = ConcurrentHashMap<Int, Collection<Order>>()

    init {
        for (i in 0..6) {
            val list = mutableListOf<Order>()
            for (c in 0..(Math.random() * 100).toInt()) {
                list.add(Order(c, i))
            }
            this.db[i] = list
        }
    }

    @MessageMapping("orders.{customerId}")
    fun orderFor(@DestinationVariable customerId: Int) =
        Flux.fromStream(this.db[customerId]!!.stream())
}

data class Order(val id: Int, val customerId: Int)