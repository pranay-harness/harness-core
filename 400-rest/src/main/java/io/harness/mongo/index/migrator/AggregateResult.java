package io.harness.mongo.index.migrator;

import io.harness.annotation.StoreIn;
import io.harness.ng.DbAliases;
import io.harness.persistence.PersistentEntity;

import lombok.Data;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Entity(noClassnameStored = true)
@StoreIn(DbAliases.CG_MANAGER)
public class AggregateResult implements PersistentEntity {
  @Id private AccountAndName _id;
  private Integer count;
}
