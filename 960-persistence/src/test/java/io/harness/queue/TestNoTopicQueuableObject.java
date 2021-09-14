/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.queue;

import lombok.Getter;
import lombok.Setter;
import org.mongodb.morphia.annotations.Entity;

@Entity(value = "!!!testNoTopicQueue", noClassnameStored = true)
public class TestNoTopicQueuableObject extends Queuable {
  @Getter @Setter private int data;

  public TestNoTopicQueuableObject(int data) {
    this.data = data;
  }
}
