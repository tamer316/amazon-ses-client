Amazon SES Client
=================
Application built with the following (main) technologies:

* Scala
* SBT
* Specs2
* Amazon SES Java SDK

Description
-----------
A Scala Client for Amazon SES which wraps the Java SDK

Prerequisites
-------------
The following applications are installed and running:

* [Scala 2.11.8](http://www.scala-lang.org/)
* [SBT](http://www.scala-sbt.org/)
    - For Mac:
      ```
      brew install sbt
      ```
Usage
-----
```
val config = ConfigFactory.load
val sesClient = SESClient(config.getString("aws.accessKeyId"), config.getString("aws.secretKeyId"))(Region.getRegion(Regions.AP_SOUTH_1))
val email = Email(subject = Content("Test Subject"),
                 source = Address("test1@email.com"),
                 bodyHtml = Some(Content("<h1>Welcome</h1>")),
                 to = Seq(Address("test2@email.com")))
sesClient.send(email)
```

Publishing
-------
- Publish to your local repository
  ```
  sbt publish-local
  ```
  
Testing
---------
- Run Unit tests
  ```
  sbt test
  ```
  
- Run one test
  ```
  sbt test-only *SESClientSpec
  ```

Code Coverage
-------------
SBT-scoverage a SBT auto plugin: https://github.com/scoverage/sbt-scoverage
- Run tests with coverage enabled by entering:
  ```
  sbt clean coverage test
  ```

After the tests have finished, find the coverage reports inside target/scala-2.11/scoverage-report