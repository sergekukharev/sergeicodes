---
title: "REST API design best practices"
date: 2022-05-05T16:33:35+01:00
draft: false
---

Over my career, I’ve designed and built many REST APIs used at scale. I learned from my teammates and shared my learnings with them. Now it’s time to share what I learned publicly.

API design is a controversial topic. People have opinions. I believe in principles, not silver bullets. Here are three that I follow when designing APIs:

### Be consistent
Consistency is important in everything we do. Applied to APIs, it means consistent [naming](#naming), [status codes](#status-codes), [response structure](#response-body), etc. Well-designed API should minimize surprises and the necessity of documentation.


### Be self-aware
I’m giving practical tips below, but they are in no way a silver bullet. Public APIs used by client apps are very different from internal ones used by microservices talking to each other. Generic APIs that fit any case are built differently from APIs built for a limited set of cases. Find the right balance between the flexibility of your design and its simplicity.


### Ensure extensibility
Versioning is a massive topic on its own. If you ensure the extensibility of your APIs, you might not need to worry about versioning. On the other hand, a public API used by 3rd party clients might need one.


## Not included
I might touch on the subjects below in follow-up articles. This article is **not** about:
- REST vs. RPC vs. GraphQL, etc. This article assumes you considered the options and picked REST.
- Authorization and authentication
- Pagination
- Sorting
- Resource relationships
- Documentation and OpenAPI
- Versioning
- Monitoring
- GraphQL


Instead, I will focus on basic building blocks.


## Naming
Naming is hard. Picking the right name for the REST endpoint is even harder, as you are limited in ways you can express yourself. I think the “Domain-Driven Design” community got pretty good at naming things. I often pick endpoint names in a similar way I choose names of my domain objects.

I usually adhere to the following rules when it comes to REST specifics.


### Use plurals everywhere
APIs are simply less confusing this way.

Consider following endpoint:
> GET /user
What does it return? Current user? If yes, how do you produce a list of users? How do you get one user? How do you change the name of that user?

Here’s a way better way, with only plurals:
> GET /users - returns list of users
> GET /users/{id} - returns a single user
> POST /users/{id} - updates user info
> GET /users/{id}/address - get an address of the user

This way, endpoints are consistent, clear, extensible.


### Use nouns everywhere
There’s no reason to use verbs in your endpoint names. You have [http methods](#http-methods) to cover that.

Consider the following endpoint:
> POST /users/{id}/set-address

If you want to read the address, would you create another, say `get-address` endpoint? POST method already conveys that the endpoint will overwrite something.

In the example above, verbs and nouns are mixed up. It reads weird. If you only use verbs, it reads even worse - `POST /get-user/{id}/set-address`.

Using verbs makes endpoints inconsistent. That’s a good enough reason to avoid using them.


## HTTP methods
In REST, HTTP methods can be confusing. After many years designing APIs, I sometimes still google “post vs. put.”

Unfortunately, the fundamental concept that helps understand them has a very fancy and confusing name. The endpoint is **idempotent** if calling it any number of times won’t change the result.

`POST` is the only method NOT idempotent, meaning multiple calls to the same endpoint can have different results. That is why designers use `POST` for the creation of resources.

With this in mind, the usage of methods is very straightforward.
* `GET` - query one resource or collection of resources
* `POST` - create new resources or make changes that cause different side effects that might affect clients.
* `PUT` - overwrite a resource
* `DELETE` - remove resource


### Usage of PATCH
`PATCH` is usually used to update a single resource field. While I think it’s very expressive, I rarely use it. I find it impractical from the implementation and maintenance points of view.

If I need a way to change one field, I usually create a nested endpoint, for example, `PUT /users/{id}/address`.

## Request parameters
Identifiers should be part of the path, while filters should go to query. For example:
> GET /users/{id}
> GET /users?country=Brazil

Of course, there could be exceptions. Sometimes you have more than one identifier:
> GET /locations?lat=60.3231&lon=42.1234
> GET /users?last_name=doe

In practice, this type of endpoint should instead return a collection. There could be more than one location at specified coordinates or people with specified last names.

### Dynamic parameters
Sometimes, it’s convenient to use dynamic parameters like `GET /messages/last` to get the last received message. Be careful with these parameters, as some idempotent endpoints will stop being such. `DELETE /messages/last` will delete a different message each time you call it.

## Status Codes
Another way of making REST endpoints more expressive is by using different status codes for different outcomes.

There are four main families of status codes:
* 2xx - for successful requests
* 3xx - redirects
* 4xx - failed request, but not necessarily a bad outcome
* 5xx - failed request, usually something that requires human attention

There are numerous status codes available. I tend to use only a few of them. It’s hard to remember the subtle differences. It is also hard to achieve usage consistency between engineers.

I often use  [https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/200](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/200)  to look up a code quickly. If you use Alfred, you can hack a custom search to lookup a status code for you.

### Success codes
Most APIs limit themselves to `200 - OK`, which is enough. Consider also using `201 - Created` for the creation of new resources.

### Client Error codes
4xx errors are not always failures. They can also send signals to clients about the availability of resources or capacity of the system.

I use the following status codes all the time:
* [400 Bad Request](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/400) when request failed validation
* [401 Unauthorized](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/401) when an access token is missing
* [403 Forbidden](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/403) when a client is authenticated but has no access to the resource
* [404 Not Found](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/404) when resource never existed
* [410 Gone](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/410) when the resource does not exist anymore

#### Generic error codes
There are a couple of codes valid on the application level to capture generic problems, like [405 Method Not Allowed](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/405) or [429 Too Many Requests](https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/429). You might or might not expose these to your clients, depending on your case. In any case, make sure that your monitoring captures such error codes, as it usually requires some human action.

#### Special note on 404
Designers tend to abuse 404. What is “Not found”? Was it deleted? Did it ever exist? Maybe the client doesn’t have access to it?

I think `404 - Not Found` should only be used when the resource never existed. There are `410 - Gone` and `403 - Unauthorized` for other cases.

### Server Error codes
Your server application should have a default error/exception handler. It would always return an error with the `500 - Internal Server Error` status code.

As a developer, you might find some other server error codes useful. Your clients should treat `5xx` codes the same way in practice.

Make sure that you monitor all requests that return server errors. Take action to mitigate those issues as soon as possible, as server error codes often break clients.


## Response body
### Envelopes
The usage of envelopes is the most critical design rule, as it makes APIs extensible and helps avoid dealing with versioning.

Consider response of `GET /users` endpoint. The most obvious response would look like this:
```json
[
  {
    "id": "abcd",
    "first_name": "John",
    // …
  },
  {
    "id": "ef12",
    "first_name": "Sam",
    // …
  }
]
```

This design has a significant flaw. You can’t add any metadata (such as pagination) without breaking existing clients or introducing a new endpoint version.

Enveloped responses allow such extensibility:
```json
{
  "users": [
    {
      "id": "abcd",
      "first_name": "John",
      // ...
    },
    {
      "id": "ef12",
      "first_name": "Sam",
      // ...
    }
  ],
  "pagination": {
    // ...
  }
}
```

In the case of single resources, envelopes are not as critical, but I still add them. Let’s consider `GET /users/{id}`. I can add any metadata to the endpoint without extending the `user` resource if I have an envelope. This way, the `user` resource is consistent across different endpoints.

To sum it up, you must use envelopes for collections. It would help if you used envelopes for individual resources.


### Standard error response
Every API should have a consistent way of reporting errors. It will lead to fewer surprises, simpler client code, and faster debugging.

I usually use the so-called “problem detail” coming from [RFC 7807 - Problem Details for HTTP APIs](https://datatracker.ietf.org/doc/html/rfc7807). You can have your format. Just be consistent.


### Multiple errors
Most of the time, you return only one error. Sometimes you will have the temptation to return multiple errors. The leading case is validation issues.

I suggest you still return one problem but include all validation issues as additional context:
```json
{
  "type": "https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/400",
  "title": "Some fields failed validation",
  "detail": "`first_name`, `last_name` failed validation",
  "instance": "/users/abcd",
  "validation_problems": [
    {
      "field": "first_name",
      "problem": "Must not be empty"
    },
    {
      "field": "last_name",
      "problem": "Has invalid characters"
    }
  ]
}
```


### Verbosity of errors
In case of client errors, you should add as much context to the error response as needed to recover from the error.

In case of server errors, you might want to hide all the context, as it might expose the internal structure of your systems to bad actors. Usually, I hide all error messages, stack traces, etc., in production. Instead, I rely on error aggregation software and logs to see what is happening:


```json
{
  "type": "https://developer.mozilla.org/en-US/docs/Web/HTTP/Status/500",
  "title": "Internal server error",
  "detail": "Oops! Something went wrong",
  "instance": "/users/abcd",
}
```


## Final words
API design is complicated - you have to make often irreversible decisions. Usually, I’m not a fan of any upfront design. API design is an exception. 2 hours spent thinking about your API now will save you weeks, if not months down the road.

## Resources
* [Build APIs You Won’t Hate](https://www.goodreads.com/en/book/show/18981361) by Phil Sturgeon. A slightly dated book on the topic.
* [A similar article](https://github.com/peterboyer/restful-api-design-tips) by Peter Boyer
