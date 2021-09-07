package io.harness;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.impl.WebhookParserSCMServiceImpl;
import io.harness.impl.scm.SCMServiceGitClientImpl;
import io.harness.impl.scm.ScmServiceClientImpl;
import io.harness.service.ScmClient;
import io.harness.service.ScmServiceClient;
import io.harness.service.WebhookParserSCMService;

import com.google.inject.AbstractModule;
import java.util.concurrent.atomic.AtomicReference;

@OwnedBy(DX)
public class SCMJavaClientModule extends AbstractModule {
  private static final AtomicReference<SCMJavaClientModule> instanceRef = new AtomicReference();

  public SCMJavaClientModule() {}

  @Override
  protected void configure() {
    bind(WebhookParserSCMService.class).to(WebhookParserSCMServiceImpl.class);
    bind(ScmClient.class).to(SCMServiceGitClientImpl.class);
    bind(ScmServiceClient.class).to(ScmServiceClientImpl.class);
  }

  public static SCMJavaClientModule getInstance() {
    if (instanceRef.get() == null) {
      instanceRef.compareAndSet(null, new SCMJavaClientModule());
    }
    return instanceRef.get();
  }
}
