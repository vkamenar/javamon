# javamon

Javamon is a lightweight JVM monitoring agent. It can be used as a Java library or a wrapper (launcher).
In the latter case there is no need to modify or recompile the target Java application source code. Javamon exposes
an HTTP endpoint compatible with [Prometheus](https://github.com/prometheus), an open source monitoring system.
The reference implementation has the following metrics:  

| Metric name     | Type    | Units   | Description                                                                        |
| --------------- | ------- | ------- | -----------------------------------------------------------------------------------|
| heap_size_bytes | gauge   | bytes   | Current JVM heap size                                                              |
| heap_free_bytes	| gauge   | bytes   | Bytes currently available in the Java heap                                         |
| uptime_sec      | counter | seconds | Seconds since last javamon restart<br>(Normally this value matches the JVM uptime) |

More metrics can be added. For example, the [source code](/src/com/agent/javamon.java#L126-L132)
shows how to include the active user threads count.  

Adding the javamon endpoint to Prometheus (`prometheus.yml`):

```
scrape-configs:
  - job_name: 'javamon'

    # Change the IP and port according to the javamon configuration
    static_configs:
      - targets: ['127.0.0.1:9091']
```

A sample Grafana dashboard is [available](/dashboard_javamon.json):
![javamon dashboard for Grafana](https://vkamenar.github.io/javamon/dashboard_javamon.png)


## Lightweight monitor

Every monitoring solution introduces some overhead in terms of memory, CPU and other system resources.
Javamon pretends to be as lightweight as possible while relying on standard Java features only. Javamon
is implemented as a single class file, without any external dependencies. The jar file size is less than
4Kb. Javamon creates only one thread. It doesn't impact Garbage Collection (GC) activity because it
doesn't create any additional objects during normal operation. If the HTTP listener can't start because
the port is busy or for any other reason, javamon will retry listening after 10s. These retries do
produce minor GC activity.  

## Wrapper

If javamon is used as a wrapper (or launcher), there is no need to modify or recompile the source code
of the target Java application. For example, let's suppose the Java application is launched as follows:  

>java -cp app.jar com.package.myapp var1 var2

Let's include javamon as a wrapper:

>java -Djm.main=com.package.myapp -cp app.jar:javamon.jar com.agent.javamon var1 var2

**Note:** When using Windows, the classpath separator is `;` instead of `:`.

The following configuration parameters are supported by the javamon wrapper:

| Parameter | Description                              | Default   |
| --------- | ---------------------------------------- | --------- |
| jm.host   | The HTTP endpoint hostname or IP address | 127.0.0.1 |
| jm.port   | The HTTP endpoint port                   | 9091      |
| jm.main   | The Java application main class          |           |

For security reasons, the default javamon host is not `0.0.0.0`, but `127.0.0.1`. Therefore, if the
`jm.host` configuration parameter is missing or empty, the HTTP endpoint will be bound to localhost.
This means that by default it will be accessible only if Prometheus is running on the same host.  

## Java library

Please, check the [javamon javadoc](https://vkamenar.github.io/javamon/javadoc.htm).  

The following sample shows how to use javamon as a library (API):

```java
import com.agent.javamon;

public class MyTestClass{
   public static void main(String[] args){
      javamon jm = new javamon("127.0.0.1", 9091);
      jm.start(); // launch the endpoint: http://127.0.0.1:9091/metrics
      // The main loop...
      jm.shut(); // signal the endpoint to stop gracefully before exiting
   }
}
```

A more complete example using javamon as a wrapper is available [here](/test/TestAPI.java).  
