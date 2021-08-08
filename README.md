# GraphQL 

## GraphQL Basics 

* graphql is a query language for your API and a server-side runtime for executing queries using a tuype sysstem that you define dofr your data. 
* graphql isnt tied to any specific database or storager engine and is insyead backed by your existing code and data 
* a graphql setvice is createrd by defining types and fields on those types, then providing functions for each field on each type. 
* at its simplest, graphql is about asking for specified fields on objects. given the following query: 

```
{
 hero {
 	name 
 }
}

```

we could expect the following result:

```
{
	"data" : {
		"hero" : {
			"name" : "r2-d2"
		}
	}
}

```

* the result has the same shape as the query
* you can ask for multiple fields, too. you can make sub-selections of fields. Graphql queries can traverse related objects and their fields, letting clients fetch lots of related data in one request, instead of making several roundtrips as one would need in a classic REST architecture. 

```
{
	hero  {
		name 
		friends {
			name
		}
	}
}

```

* you can send arguments into queries. In GraphQL you can send arguments for each field and nested object. you can even send arguments into scalar fields, to implement data transformations once on the server, instead of of on every cleint separately. 

```
{
	human (id :  "1000" ) {
		name 
		heigh(unit: FOOT)
	}
}

```

* would return: 

```
{
	"data" : {
		"human" : {
			"name" : "Luke Skywalker",
			"height" : 5.643243
		}
	}
}

```

* You can also assign aliases to the returned objuects os that youc an differentiate them.

```
{
	empireHero: hero (episode: EMPIRE) {
		name 
	}
	jediHero : hero (episode : JEDI) {
		name
	}
}

```

* this would return fields with alias names:

```
{
	"data": {
		"empireHero" : {
			"name" : "Luke Skywalker"
		}
		"jediHero" : {
			"name" : "R2-D2"
		}
	}
}

```

* Lets say you wanted to get two records from the server and each record was to return the identical fields. You don't want to re-specify the same fields in both queries, so you can use a `fragment`.

```
{
  leftComparison: hero(episode: EMPIRE) {
    ...comparisonFields
  }
  rightComparison: hero(episode: JEDI) {
    ...comparisonFields
  }
}

fragment comparisonFields on Character {
  name
  appearsIn
  friends {
    name
  }
}

```

 * the above says to return the comparisons with the fields inline for both. It reuses the fragment specification for both the right and left comparison. 

 * Normally, you include the token 	`query` and a name for the query. Thus far all the example queries have just started with `{ ... `. 

 * you can define variables as a way to support client parameters to be passed as arguments. they'd get sent as a dictionary of some sort in the client Java code. Variables are sort of like named parameters in a JDBC statement. You can even provide default variables, too: 

 ``` 
 query HeroNameAndDFriends ($episode: Episode = JEDI)  {
 	hero (episode : $episode) {
 		name
 		friends {
 			name
 		}
 	}
 }
 
 ```

 * There are some directives that can be used in the queries that allow you to make the query (and therefore the response) more dynamic.  

```

query Hero(
	$episode: Episode, 
	$withFriends: Boolean!) {

  hero(episode: $episode) {
    name
    friends @include(if: $withFriends) {
      name
    }
  }
}

```

* in that example, the `@include(if: $withFriends)` directive includes the friends data if and only if the "withFriends" argument is "true." 

* there are two directives: `@include(if: Boolean)` and `@skip(if: Boolean)`. 

* Most interactions with GraphQL discuss fetching data, but there's also good support for mutating data, through somethign called _mutuations_. 

* In REST, any request might end up causing some side-effects on the server, but by convention it's suggested that one doesn't use GET requests to modify data. GraphQL is similar - technically any query could be implemented to cause a data write. However, it's useful to establish a convention that any operations that cause writes should be sent explicitly via a mutation.

* Just like in queries, if the mutation field returns an object type, you can ask for nested fields. This can be useful for fetching the new state of an object after an update. Let's look at a simple example mutation:

```
mutation CreateReviewForEpisode($ep: Episode!, $review: ReviewInput!) {
  createReview(episode: $ep, review: $review) {
    stars
    commentary
  }
} 

```

* a mutation can contain multiple fields, just like a query. While query fields are executed in _parallel_, mutation fields run in _series_, one after the other. This means that if we send two incrementCredits mutations in one request, the first is guaranteed to finish before the second begins, ensuring that we don't end up with a race condition with ourselves.


* There are some meta fields, like `__typename`, that give you information about the response. 



## Spring GraphQL 

* this project is a logical successro to the GraphQL Java Spring project 
* this new support introduces: 
	* HTTP handlers 
	* WebSocket handlers - following the protocol from graphql-ws with support fro GraphQL subscription streams 
	* Web Interception - abilityt o intercept every graphql rewquest, inspect HTTp headers, and modifyt eh graphql `ExecutionInput` or `ExecutionResult`
	* a Spring Boot starter that pulls everything together 

* This is also the foundational piece on which security, testing, and metrics wil be integrated

* there are a few means for security: you can secure teh graphql URL itself. You can secure the http url from which responses originate. You can secure the methods involved in the production of the response. but in order to do that youll need to alao handle propagating the context across threads. Here's an example [of webflux, graphql and security](https://github.com/spring-projects/spring-graphql/tree/main/samples/webflux-security)

* Exception handling: spring greaphql enables apps to create mutliple, independent `GralQlExceptionResolver` componetns to resolve exceptions to GraphQL errors for inclusion in the GralQL response. It also prvides an `ErrorType` to use to classify errors with common categories such as `BAD_REQUEST`, `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, and `INTERNAL_ERROR` by default. 

* you can test grapql requests using `WebTestClient`: just send and receive JSON. However, GraphQL specific details make this approach more cumbersome than it should be. This is why, in addition, you can use the `WebGrapQlTester`. 

* Metrics are supported when the `spring-boot-starter-actuator` project is on the classpath. 

* You can also use Spring GraphQL with the [Spring Data QueryDSL integration](https://github.com/spring-projects/spring-graphql/tree/main/samples/webmvc-http) to make it easy to create a Querydsl backed DataFetcher. It prepares a Querydsl Predicate from GraphQL request parameters, and uses it to fetch data and that works for JPA, MongoDB, and LDAP.

### Schema vs Object-First Development 

GraphQL provides a schema language that helps clients to create valid requests, enables the GraphiQL UI editor, promotes a common vocabulary across teams, and so on. It also brings up the age old schema vs object-first development dilemma.

Our take is that schema-first development should be preferred. It facilitates a conversation among people of technical and non-technical background, it helps with tooling, it makes it easier to track changes, and so on. There is also no one-for-one mapping between GraphQL schema and Java types.

That said there is room for code generation too, to get started, for clients to create queries, and so on. Frameworks like Netflix DGS have excellent support for this that can be used with Spring GraphQL.

## Resources: 

* Why [Github uses GraphQL](https://docs.github.com/en/graphql/overview/about-the-graphql-api)
* [Introducing Spring GraphQL](https://spring.io/blog/2021/07/06/introducing-spring-graphql)
* the [Spring GraphQL reference documentation](https://docs.spring.io/spring-graphql/docs/1.0.0-SNAPSHOT/reference/html/)
