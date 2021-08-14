package com.example.customers

import org.springframework.boot.ApplicationArguments
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.annotation.Id
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Component
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@SpringBootApplication
class CustomersApplication

fun main(args: Array<String>) {
    runApplication<CustomersApplication>(*args)
}

@Component
class Initializer(private val customerRepository: CustomerRepository) :
    ApplicationRunner {

    override fun run(args: ApplicationArguments?) {
        val names = Flux
            .just("Luke", "Yuxin", "Dave", "Olga", "Stéphane", "Madhura")
            .map { Customer(name = it) }
            .flatMap { customerRepository.save(it) }
        names
            .thenMany(customerRepository.findAll())
            .subscribe { println(it) }
    }
}

@RestController
class CustomerRestController(val cr: CustomerRepository) {

    @GetMapping("/customers")
    fun get() = this.cr.findAll()
}

data class Customer(@Id val id: Int? = null, val name: String)

interface CustomerRepository : ReactiveCrudRepository<Customer, Int>