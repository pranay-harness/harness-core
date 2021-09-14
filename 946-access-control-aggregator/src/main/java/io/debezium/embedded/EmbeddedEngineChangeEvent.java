/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.debezium.embedded;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.RecordChangeEvent;
import org.apache.kafka.connect.source.SourceRecord;

/**
 * This class is a copy of EmbeddedEngineChangeEvent from debezium-embedded library.
 * Weirdly, they have not made the class public, and we have to get sourceRecord from the event to get '__op' field
 * Hence, we have made a copy of the class and made it public.
 * Please change this class if you change the version of Debezium in the future, right now it is 1.4.1.Final
 */
@OwnedBy(PL)
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
