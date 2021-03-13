package io.debezium.embedded;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.RecordChangeEvent;
import org.apache.kafka.connect.source.SourceRecord;

public class EmbeddedEngineChangeEvent<K, V> implements ChangeEvent<K, V>, RecordChangeEvent<V> {
  private final K key;
  private final V value;
  private final SourceRecord sourceRecord;

  public EmbeddedEngineChangeEvent(K key, V value, SourceRecord sourceRecord) {
    this.key = key;
    this.value = value;
    this.sourceRecord = sourceRecord;
  }

  @Override
  public K key() {
    return key;
  }

  @Override
  public V value() {
    return value;
  }

  @Override
  public V record() {
    return value;
  }

  @Override
  public String destination() {
    return sourceRecord.topic();
  }

  public SourceRecord sourceRecord() {
    return sourceRecord;
  }

  @Override
  public String toString() {
    return "EmbeddedEngineChangeEvent [key=" + key + ", value=" + value + ", sourceRecord=" + sourceRecord + "]";
  }
}
