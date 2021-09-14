/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.queue;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class TestTopicQueuableObjectListener extends QueueListener<TestTopicQueuableObject> {
  private boolean throwException;

  public void setThrowException(boolean throwException) {
    this.throwException = throwException;
  }

  @Inject
  TestTopicQueuableObjectListener(QueueConsumer<TestTopicQueuableObject> consumer) {
    super(consumer, true);
  }

  @Override
  public void onMessage(TestTopicQueuableObject message) {
    if (throwException) {
      throw new RuntimeException("Expected Exception In Test.");
    }
  }
}
