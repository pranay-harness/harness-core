package io.harness.influx;

import org.influxdb.BatchOptions;
import org.influxdb.InfluxDB;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.influxdb.dto.Pong;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;

import java.util.List;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@SuppressWarnings("deprecation")
public class DummyInfluxDB implements InfluxDB {
  @Override
  public InfluxDB setLogLevel(LogLevel logLevel) {
    return this;
  }

  @Override
  public InfluxDB enableGzip() {
    return this;
  }

  @Override
  public InfluxDB disableGzip() {
    return this;
  }

  @Override
  public boolean isGzipEnabled() {
    return false;
  }

  @Override
  public InfluxDB enableBatch() {
    return this;
  }

  @Override
  public InfluxDB enableBatch(BatchOptions batchOptions) {
    return this;
  }

  @Override
  public InfluxDB enableBatch(int i, int i1, TimeUnit timeUnit) {
    return this;
  }

  @Override
  public InfluxDB enableBatch(int i, int i1, TimeUnit timeUnit, ThreadFactory threadFactory) {
    return this;
  }

  @Override
  public InfluxDB enableBatch(int i, int i1, TimeUnit timeUnit, ThreadFactory threadFactory,
      BiConsumer<Iterable<Point>, Throwable> biConsumer, ConsistencyLevel consistencyLevel) {
    return this;
  }

  @Override
  public InfluxDB enableBatch(int i, int i1, TimeUnit timeUnit, ThreadFactory threadFactory,
      BiConsumer<Iterable<Point>, Throwable> biConsumer) {
    return this;
  }

  @Override
  public void disableBatch() {}

  @Override
  public boolean isBatchEnabled() {
    return false;
  }

  @Override
  public Pong ping() {
    return null;
  }

  @Override
  public String version() {
    return null;
  }

  @Override
  public void write(Point point) {}

  @Override
  public void write(String s) {}

  @Override
  public void write(List<String> list) {}

  @Override
  public void write(String s, String s1, Point point) {}

  @Override
  public void write(int i, Point point) {}

  @Override
  public void write(BatchPoints batchPoints) {}

  @Override
  public void writeWithRetry(BatchPoints batchPoints) {}

  @Override
  public void write(String s, String s1, ConsistencyLevel consistencyLevel, String s2) {}

  @Override
  public void write(String s, String s1, ConsistencyLevel consistencyLevel, TimeUnit timeUnit, String s2) {}

  @Override
  public void write(String s, String s1, ConsistencyLevel consistencyLevel, List<String> list) {}

  @Override
  public void write(String s, String s1, ConsistencyLevel consistencyLevel, TimeUnit timeUnit, List<String> list) {}

  @Override
  public void write(int i, String s) {}

  @Override
  public void write(int i, List<String> list) {}

  @Override
  public QueryResult query(Query query) {
    return null;
  }

  @Override
  public void query(Query query, Consumer<QueryResult> consumer, Consumer<Throwable> consumer1) {}

  @Override
  public void query(Query query, int i, Consumer<QueryResult> consumer) {}

  @Override
  public void query(Query query, int i, BiConsumer<Cancellable, QueryResult> biConsumer) {}

  @Override
  public void query(Query query, int i, Consumer<QueryResult> consumer, Runnable runnable) {}

  @Override
  public void query(Query query, int i, BiConsumer<Cancellable, QueryResult> biConsumer, Runnable runnable) {}

  @Override
  public void query(Query query, int i, BiConsumer<Cancellable, QueryResult> biConsumer, Runnable runnable,
      Consumer<Throwable> consumer) {}

  @Override
  public QueryResult query(Query query, TimeUnit timeUnit) {
    return null;
  }

  @Override
  public void createDatabase(String s) {}

  @Override
  public void deleteDatabase(String s) {}

  @Override
  public List<String> describeDatabases() {
    return null;
  }

  @Override
  public boolean databaseExists(String s) {
    return false;
  }

  @Override
  public void flush() {}

  @Override
  public void close() {}

  @Override
  public InfluxDB setConsistency(ConsistencyLevel consistencyLevel) {
    return this;
  }

  @Override
  public InfluxDB setDatabase(String s) {
    return this;
  }

  @Override
  public InfluxDB setRetentionPolicy(String s) {
    return this;
  }

  @Override
  public void createRetentionPolicy(String s, String s1, String s2, String s3, int i, boolean b) {}

  @Override
  public void createRetentionPolicy(String s, String s1, String s2, int i, boolean b) {}

  @Override
  public void createRetentionPolicy(String s, String s1, String s2, String s3, int i) {}

  @Override
  public void dropRetentionPolicy(String s, String s1) {}
}
