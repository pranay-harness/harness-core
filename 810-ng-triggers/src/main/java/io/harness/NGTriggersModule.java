package io.harness;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.webhook.WebhookSourceRepo;
import io.harness.ngtriggers.service.NGTriggerService;
import io.harness.ngtriggers.service.impl.NGTriggerServiceImpl;
import io.harness.ngtriggers.utils.AwsCodeCommitDataObtainer;
import io.harness.ngtriggers.utils.GitProviderBaseDataObtainer;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import java.util.concurrent.atomic.AtomicReference;

@OwnedBy(PIPELINE)
public class NGTriggersModule extends AbstractModule {
  private static final AtomicReference<NGTriggersModule> instanceRef = new AtomicReference<>();

  public static NGTriggersModule getInstance() {
    if (instanceRef.get() == null) {
      instanceRef.compareAndSet(null, new NGTriggersModule());
    }
    return instanceRef.get();
  }

  @Override
  protected void configure() {
    install(SCMJavaClientModule.getInstance());
    bind(NGTriggerService.class).to(NGTriggerServiceImpl.class);
    MapBinder.newMapBinder(binder(), String.class, GitProviderBaseDataObtainer.class)
        .addBinding(WebhookSourceRepo.AWS_CODECOMMIT.name())
        .to(AwsCodeCommitDataObtainer.class);
  }
}