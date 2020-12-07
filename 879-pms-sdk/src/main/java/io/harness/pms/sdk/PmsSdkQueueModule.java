package io.harness.pms.sdk;

import io.harness.pms.execution.NodeExecutionEvent;
import io.harness.pms.sdk.execution.NodeExecutionEventListener;
import io.harness.queue.QueueController;
import io.harness.queue.QueueListener;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;

public class PmsSdkQueueModule extends AbstractModule {
  private static PmsSdkQueueModule instance;

  public static PmsSdkQueueModule getInstance() {
    if (instance == null) {
      instance = new PmsSdkQueueModule();
    }
    return instance;
  }

  private PmsSdkQueueModule() {}

  @Override
  protected void configure() {
    bind(QueueController.class).toInstance(new PmsSdkQueueController());
    bind(new TypeLiteral<QueueListener<NodeExecutionEvent>>() {}).to(NodeExecutionEventListener.class);
  }

  private static class PmsSdkQueueController implements QueueController {
    @Override
    public boolean isPrimary() {
      return true;
    }

    @Override
    public boolean isNotPrimary() {
      return false;
    }
  }
}
