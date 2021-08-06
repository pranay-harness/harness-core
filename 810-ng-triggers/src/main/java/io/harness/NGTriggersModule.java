package io.harness;

import static io.harness.AuthorizationServiceHeader.PIPELINE_SERVICE;
import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ngtriggers.beans.source.webhook.WebhookSourceRepo;
import io.harness.ngtriggers.service.NGTriggerService;
import io.harness.ngtriggers.service.NGTriggerWebhookRegistrationService;
import io.harness.ngtriggers.service.NGTriggerYamlSchemaService;
import io.harness.ngtriggers.service.impl.NGTriggerServiceImpl;
import io.harness.ngtriggers.service.impl.NGTriggerWebhookRegistrationServiceImpl;
import io.harness.ngtriggers.service.impl.NGTriggerYamlSchemaServiceImpl;
import io.harness.ngtriggers.utils.AwsCodeCommitDataObtainer;
import io.harness.ngtriggers.utils.GitProviderBaseDataObtainer;
import io.harness.ngtriggers.utils.SCMDataObtainer;
import io.harness.pipeline.PipelineRemoteClientModule;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.webhook.WebhookConfigProvider;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import java.util.concurrent.atomic.AtomicReference;

@OwnedBy(PIPELINE)
public class NGTriggersModule extends AbstractModule {
  private static final AtomicReference<NGTriggersModule> instanceRef = new AtomicReference<>();
  private String pmsApiBaseUrl;
  private ServiceHttpClientConfig pmsHttpClientConfig;
  private String pipelineServiceSecret;

  public static NGTriggersModule getInstance(
      String pmsApiBaseUrl, ServiceHttpClientConfig pmsHttpClientConfig, String pipelineServiceSecret) {
    if (instanceRef.get() == null) {
      instanceRef.compareAndSet(null, new NGTriggersModule(pmsApiBaseUrl, pmsHttpClientConfig, pipelineServiceSecret));
    }
    return instanceRef.get();
  }

  private NGTriggersModule(
      String pmsApiBaseUrl, ServiceHttpClientConfig pmsHttpClientConfig, String pipelineServiceSecret) {
    this.pmsApiBaseUrl = pmsApiBaseUrl;
    this.pmsHttpClientConfig = pmsHttpClientConfig;
    this.pipelineServiceSecret = pipelineServiceSecret;
  }

  @Override
  protected void configure() {
    install(SCMJavaClientModule.getInstance());
    bind(NGTriggerService.class).to(NGTriggerServiceImpl.class);
    bind(NGTriggerWebhookRegistrationService.class).to(NGTriggerWebhookRegistrationServiceImpl.class);
    bind(NGTriggerYamlSchemaService.class).to(NGTriggerYamlSchemaServiceImpl.class);
    bind(WebhookConfigProvider.class).toInstance(new WebhookConfigProvider() {
      @Override
      public String getPmsApiBaseUrl() {
        return pmsApiBaseUrl;
      }
    });
    install(new PipelineRemoteClientModule(
        ServiceHttpClientConfig.builder().baseUrl(pmsHttpClientConfig.getBaseUrl()).build(), pipelineServiceSecret,
        PIPELINE_SERVICE.toString()));
    MapBinder<String, GitProviderBaseDataObtainer> gitProviderBaseDataObtainerMap =
        MapBinder.newMapBinder(binder(), String.class, GitProviderBaseDataObtainer.class);
    gitProviderBaseDataObtainerMap.addBinding(WebhookSourceRepo.AWS_CODECOMMIT.name())
        .to(AwsCodeCommitDataObtainer.class);
    gitProviderBaseDataObtainerMap.addBinding(WebhookSourceRepo.GITHUB.name()).to(SCMDataObtainer.class);
    gitProviderBaseDataObtainerMap.addBinding(WebhookSourceRepo.BITBUCKET.name()).to(SCMDataObtainer.class);
    gitProviderBaseDataObtainerMap.addBinding(WebhookSourceRepo.GITLAB.name()).to(SCMDataObtainer.class);
  }
}