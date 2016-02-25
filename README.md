# reactive-kafka-workbench

Kicking the tires of [reactive-kafka](https://github.com/softwaremill/reactive-kafka).

## Aim

Get nice Reactive Streams interface to dealing with Kafka 0.9.0.

- with string keys
- with custom payload objects

Learn to write tests for Akka Streams.

## Preconditions

You will need Zookeeper and Kafka 0.9.0 running, locally.

To get them on OS X, with Homebrew (note: Kafka 0.9.0 is not yet officially available in Homebrew):

```
$ brew install zookeeper
...
$ zkServer start
```

```
$ brew install https://raw.githubusercontent.com/iostat/homebrew2/master/Library/Formula/kafka.rb
...
$ kafka-server-start.sh /usr/local/etc/kafka/server.properties
```

This will launch Kafka 0.9.0 in the terminal, allowing you to see its logs.

Note: If you have an earlier, Kafka 0.8.x installed, uninstall it and also remove the `/usr/local/etc/kafka` folder, before
installing Kafka 0.9.0.

## Getting started

Reactive-kafka doesn't have a 0.9.0 release, yet (230216) so let's build and publish one locally.

```
$ git clone https://github.com/softwaremill/reactive-kafka.git
...
$ cd reactive-kafka
$ sbt publishLocal
```

Then, in the `reactive-kafka-workbench` folder:

```
$ sbt test
```

## References

- [reactive-kafka](https://github.com/softwaremill/reactive-kafka) - using Kafka as Akka Streams in Scala
- [Using Kafka Command-line Tools](http://www.cloudera.com/documentation/kafka/latest/topics/kafka_command_line.html)

---

## Debugging helps

To use Kafka from the command line:

```
$ kafka-topics --zookeeper localhost:2181 --list
```

```
$ kafka-topics --zookeeper localhost:2181 --list
```

```
$ kafka-console-consumer.sh  --zookeeper localhost:2181 --topic XXX --from-beginning
```

