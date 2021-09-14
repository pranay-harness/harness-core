/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.mongo.index.migrator;

import io.harness.persistence.PersistentEntity;

import lombok.Data;
import org.mongodb.morphia.annotations.Entity;
import org.mongodb.morphia.annotations.Id;

@Data
@Entity(noClassnameStored = true)
public class AggregateResult implements PersistentEntity {
  @Id private AccountAndName _id;
  private Integer count;
}
