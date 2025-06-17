# javamon

Javamon is a lightweight monitoring agent for JVM processes. It can be used as a library or a wrapper.
In the latter case there is no need to modify the target Java application source code. Javamon exposes
an HTTP endpoint compatible with [Prometheus](https://github.com/prometheus). The reference implementation
has the following metrics:  

* Uptime in seconds
* Heap memory usage in bytes (total size and free size)  

## Lightweight monitor

Every monitoring solution introduces some overhead in terms of memory, CPU and other system resources.
Javamon pretends to be as lightweight as possible while relying on standard Java features only. Javamon
is implemented as a single class file, without any external dependencies. The jar file size is less than
4Kb. Javamon creates only one thread. It doesn't impact Garbage Collection (GC) activity because it
doesn't create any additional objects during normal operation.  

## Wrapper

If javamon is used as a wrapper (or launcher), there is no need to modify or recompile the source code
of the target Java application. For example, let's suppose the Java application is launched as follows:  

```java -classpath app.jar com.package.myapp var1 var2```

Let's include javamon as a wrapper:

```java -Djm.main=com.package.myapp -classpath app.jar:javamon.jar com.agent.javamon var1 var2```

**Note:** When using Windows, the jars are separated by ```;``` instead of ```:```.

The following configuration parameters are supported by the javamon wrapper:

| Parameter | Description                              | Default   |
| --------- | ---------------------------------------- | --------- |
| jm.host   | The HTTP endpoint hostname or IP address | 127.0.0.1 |
| jm.port   | The HTTP endpoint port                   | 9091      |
| jm.main   | The Java application main class          |           |

For security reasons, the default javamon host is not `0.0.0.0`, but `127.0.0.1`. Therefore, if the
`jm.host` configuration parameter is missing or empty, the HTTP endpoint will be bound to localhost.
This means that by default it will be accessible only if Prometheus is running on the same host.
