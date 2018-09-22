package software.wings.integration;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.Application;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;

import java.util.HashMap;
import java.util.Map;

@Singleton
public class SeedDataService {
  private String SEED_APP = "SEED_APP";
  private String SEED_SERVICE_WAR = "SEED_SERVICE_WAR";
  private String SEED_WORKFLOW_SSH_BASIC = "SEED_WORKFLOW_SSH_BASIC";

  @Inject private WingsPersistence wingsPersistence;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ServiceTemplateService serviceTemplateService;
  @Inject private ServiceInstanceService serviceInstanceService;
  @Inject private EnvironmentService environmentService;
  @Inject private InfrastructureMappingService infrastructureMappingService;

  private Map<String, Object> seedEntitiesCache = new HashMap<>();

  public Application getSeedApp() {
    if (seedEntitiesCache.get(SEED_APP) == null) {
      return null;
    }
    return (Application) seedEntitiesCache.get(SEED_APP);
  }
}
