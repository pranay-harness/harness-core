/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.integration;

import io.harness.persistence.HPersistence;

import software.wings.beans.Application;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

@Singleton
public class SeedDataService {
  private String SEED_APP = "SEED_APP";
  private String SEED_SERVICE_WAR = "SEED_SERVICE_WAR";
  private String SEED_WORKFLOW_SSH_BASIC = "SEED_WORKFLOW_SSH_BASIC";

  @Inject private HPersistence persistence;
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
