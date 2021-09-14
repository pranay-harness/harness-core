/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.integration.common;

import software.wings.beans.Base;

import lombok.Getter;
import lombok.Setter;
import org.mongodb.morphia.annotations.Entity;

@Entity(value = "!!!testMongo", noClassnameStored = true)
public class MongoEntity extends Base {
  @Getter @Setter private String data;
}
