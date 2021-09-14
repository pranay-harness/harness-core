/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.queue;

import com.google.inject.Singleton;

@Singleton
public class TestNoTopicQueuableObjectListener extends QueueListener<TestNoTopicQueuableObject> {
  private boolean throwException;

  public void setThrowException(boolean throwException) {
    this.throwException = throwException;
  }

  TestNoTopicQueuableObjectListener() {
    super(null, true);
  }

  @Override
  public void onMessage(TestNoTopicQueuableObject message) {
    if (throwException) {
      throw new RuntimeException("Expected Exception In Test.");
    }
  }
}
