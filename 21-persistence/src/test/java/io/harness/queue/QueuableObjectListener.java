package io.harness.queue;

import com.google.inject.Singleton;

@Singleton
public class QueuableObjectListener extends QueueListener<QueuableObject> {
  public QueuableObjectListener() {
    super(true);
  }

  @Override
  protected void onMessage(QueuableObject message) {}
}
