package io.harness.queue;

import com.google.inject.Singleton;

@Singleton
public class TestVersionedQueuableObjectListener extends QueueListener<TestVersionedQueuableObject> {
  private boolean throwException;

  public void setThrowException(boolean throwException) {
    this.throwException = throwException;
  }

  TestVersionedQueuableObjectListener() {
    super(true);
  }

  @Override
  public void onMessage(TestVersionedQueuableObject message) {
    if (throwException) {
      throw new RuntimeException("Expected Exception In Test.");
    }
  }
}
