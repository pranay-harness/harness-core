package io.harness;

import io.harness.impl.WebhookParserSCMServiceImpl;
import io.harness.service.WebhookParserSCMService;

import com.google.inject.AbstractModule;
import java.util.concurrent.atomic.AtomicReference;

public class SCMJavaClientModule extends AbstractModule {
  private static final AtomicReference<SCMJavaClientModule> instanceRef = new AtomicReference();

  public SCMJavaClientModule() {}

  @Override
  protected void configure() {
    bind(WebhookParserSCMService.class).to(WebhookParserSCMServiceImpl.class);
  }

  public static SCMJavaClientModule getInstance() {
    if (instanceRef.get() == null) {
      instanceRef.compareAndSet(null, new SCMJavaClientModule());
    }

    return instanceRef.get();
  }
}
