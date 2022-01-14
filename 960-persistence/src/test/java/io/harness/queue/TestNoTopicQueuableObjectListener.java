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
