/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.morphia;

import org.mongodb.morphia.mapping.MappedClass;
import org.mongodb.morphia.mapping.Mapper;

public class HMappedClass extends MappedClass {
  private final String collectionName;

  public HMappedClass(String collectionName, Class<?> clazz, Mapper mapper) {
    super(clazz, mapper);
    this.collectionName = collectionName;
  }

  @Override
  public String getCollectionName() {
    return collectionName;
  }
}
