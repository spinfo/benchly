# Benchly Workbench Coordinator

A web app that connects to multiple instances of the [Strings & Structures Workbench](https://github.com/spinfo/stringsnstructures)

A [Frontend is available via github](https://github.com/spinfo/benchly-frontend) as well.

[An example for a complete setup is available in the wiki.](https://github.com/spinfo/benchly/wiki/Simple-Setup)

## Build

For a standard build:

```
mvn clean package
```

To build a standalone jar:

```
mvn compile assembly:single
```

## Run

The application needs a database connection which you can give via command line. (Tested on MySQL but should work with other databases.)

For example:

```
java -jar target/benchly.jar --jdbc-url="jdbc:mysql://user:pass@localhost:3306/
```

To allow the encryption of storage locations, the environment variable `BENCHLY_SHARED_SECRET` needs to be set to the same value that the instances use. 

The application can directly serve the frontend using the `--frontend-path="/a/path"` parameter.
