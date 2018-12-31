package io.harness.queue;

import com.google.inject.Singleton;

@Singleton
public class TestQueuableObjectListener extends QueueListener<TestQueuableObject> {
  private boolean throwException;

  /**
   * Sets throw exception.
   *
   * @param throwException the throw exception
   */
  public void setThrowException(boolean throwException) {
    this.throwException = throwException;
  }

  TestQueuableObjectListener() {
    super(true);
  }

  @Override
  protected void onMessage(TestQueuableObject message) {
    if (throwException) {
      throw new RuntimeException("Expected Exception In Test.");
    }
  }
}
