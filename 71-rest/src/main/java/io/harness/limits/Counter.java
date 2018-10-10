package io.harness.limits;

import lombok.EqualsAndHashCode;
import lombok.Value;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;

@EqualsAndHashCode(callSuper = false)
@Entity(value = "limitCounters", noClassnameStored = true)
@Indexes(@Index(fields = @Field("key"), options = @IndexOptions(name = "key_idx", unique = true)))
@Value
public class Counter extends Base {
  private final String key;
  private final Long value;

  public Counter(String key, long value) {
    this.key = key;
    this.value = value;
  }

  // morphia expects an no-args constructor
  private Counter() {
    this.key = null;
    this.value = null;
  }
}
