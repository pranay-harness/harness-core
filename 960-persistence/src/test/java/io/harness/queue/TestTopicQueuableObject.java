/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.queue;

import lombok.Value;
import org.mongodb.morphia.annotations.Entity;

@Value
@Entity(value = "!!!testTopicQueue", noClassnameStored = true)
public class TestTopicQueuableObject extends Queuable {
  private int data;

  public TestTopicQueuableObject(int data) {
    this.data = data;
  }
}
