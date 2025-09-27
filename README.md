# Mail SRS for Java

A library to work with Sender Rewriting Scheme (SRS).

### Dependencies

Requires Java 8 runtime.

### Example

```
String email = "user@example.com";

SRS srs = new SRS("aKey");

ReversePath forward = srs.forward(email, "srs.example.com");
System.out.println(forward);

ReversePath reverse = srs.reverse("SRS0=amVT=4G=example.com=user@srs.forward.com");
System.out.println(reverse);
```

### License

Mail SRS for Java is licensed under Apache License 2.0.