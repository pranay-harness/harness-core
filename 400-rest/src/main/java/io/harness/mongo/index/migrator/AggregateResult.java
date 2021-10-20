package io.harness.mongo.index.migrator;

import io.harness.persistence.PersistentEntity;

import lombok.Data;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Entity(noClassnameStored = true)
public final class AggregateResult implements PersistentEntity {
  @Id private AccountAndName _id;
  private Integer count;
}
