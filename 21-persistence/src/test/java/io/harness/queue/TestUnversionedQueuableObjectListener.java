package io.harness.queue;

import com.google.inject.Singleton;

@Singleton
public class TestUnversionedQueuableObjectListener extends QueueListener<TestUnversionedQueuableObject> {
  private boolean throwException;

  public void setThrowException(boolean throwException) {
    this.throwException = throwException;
  }

  TestUnversionedQueuableObjectListener() {
    super(true);
  }

  @Override
  public void onMessage(TestUnversionedQueuableObject message) {
    if (throwException) {
      throw new RuntimeException("Expected Exception In Test.");
    }
  }
}
