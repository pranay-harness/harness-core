package io.harness.app;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

import io.harness.app.impl.CIPipelineServiceImpl;
import io.harness.app.impl.YAMLToObjectImpl;
import io.harness.app.intfc.CIPipelineService;
import io.harness.app.intfc.YAMLToObject;
import io.harness.impl.CIPipelineExecutionService;
import io.harness.impl.CIPipelineExecutionServiceImpl;
import io.harness.managerclient.ManagerCIResource;
import io.harness.managerclient.ManagerClientFactory;
import io.harness.persistence.HPersistence;
import io.harness.security.ServiceTokenGenerator;
import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.ci.CIServiceAuthSecretKey;
import software.wings.service.impl.ci.CIServiceAuthSecretKeyImpl;
import software.wings.service.impl.security.NoOpSecretManagerImpl;
import software.wings.service.intfc.security.SecretManager;

public class CIManagerServiceModule extends AbstractModule {
  private String managerBaseUrl;
  private final CIManagerConfiguration ciManagerConfiguration;

  public CIManagerServiceModule(CIManagerConfiguration ciManagerConfiguration, String managerBaseUrl) {
    this.ciManagerConfiguration = ciManagerConfiguration;
    this.managerBaseUrl = managerBaseUrl;
  }

  @Override
  protected void configure() {
    ServiceTokenGenerator tokenGenerator = new ServiceTokenGenerator();
    bind(CIManagerConfiguration.class).toInstance(ciManagerConfiguration);
    bind(YAMLToObject.class).toInstance(new YAMLToObjectImpl());
    bind(HPersistence.class).to(WingsMongoPersistence.class).in(Singleton.class);
    bind(WingsPersistence.class).to(WingsMongoPersistence.class).in(Singleton.class);
    bind(SecretManager.class).to(NoOpSecretManagerImpl.class);
    bind(CIPipelineService.class).to(CIPipelineServiceImpl.class);
    bind(ManagerCIResource.class).toProvider(new ManagerClientFactory(managerBaseUrl, tokenGenerator));
    bind(CIPipelineExecutionService.class).to(CIPipelineExecutionServiceImpl.class);
    bind(CIServiceAuthSecretKey.class).to(CIServiceAuthSecretKeyImpl.class);
  }
}
