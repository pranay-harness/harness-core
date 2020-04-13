package io.harness.limits;

import io.harness.annotation.HarnessEntity;
import io.harness.persistence.AccountAccess;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Field;
import org.mongodb.morphia.annotations.Index;
import org.mongodb.morphia.annotations.IndexOptions;
import org.mongodb.morphia.annotations.Indexed;
import org.mongodb.morphia.annotations.Indexes;
import software.wings.beans.Base;

@Getter
@ToString
@EqualsAndHashCode(callSuper = false)
@Entity(value = "limitCounters", noClassnameStored = true)
@Indexes(@Index(fields = @Field("key"), options = @IndexOptions(name = "key_idx", unique = true)))
@FieldNameConstants(innerTypeName = "CounterKeys")
@HarnessEntity(exportable = true)
public class Counter extends Base implements AccountAccess {
  private final String key;
  private final Long value;
  @Indexed private String accountId;

  public Counter(String key, long value) {
    this.key = key;
    this.value = value;

    populateAccountIdFromKey();
  }

  public void populateAccountIdFromKey() {
    this.accountId = getAccountIdFromKey();
  }

  private String getAccountIdFromKey() {
    Action action = Action.fromKey(key);
    return action.getAccountId();
  }

  // morphia expects an no-args constructor
  private Counter() {
    this.key = null;
    this.value = null;
    this.accountId = null;
  }
}
