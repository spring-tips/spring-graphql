# GraphQL 

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

* 

