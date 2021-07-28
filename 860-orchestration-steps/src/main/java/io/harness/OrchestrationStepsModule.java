package io.harness;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cf.CFApi;
import io.harness.cf.openapi.ApiClient;
import io.harness.steps.approval.step.ApprovalInstanceService;
import io.harness.steps.approval.step.ApprovalInstanceServiceImpl;
import io.harness.steps.barriers.service.BarrierService;
import io.harness.steps.barriers.service.BarrierServiceImpl;
import io.harness.steps.resourcerestraint.service.ResourceRestraintInstanceService;
import io.harness.steps.resourcerestraint.service.ResourceRestraintInstanceServiceImpl;
import io.harness.steps.resourcerestraint.service.ResourceRestraintRegistry;
import io.harness.steps.resourcerestraint.service.ResourceRestraintRegistryImpl;
import io.harness.steps.resourcerestraint.service.ResourceRestraintService;
import io.harness.steps.resourcerestraint.service.ResourceRestraintServiceImpl;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@OwnedBy(HarnessTeam.PIPELINE)
public class OrchestrationStepsModule extends AbstractModule {
  private final OrchestrationStepConfig configuration;
  private static OrchestrationStepsModule instance;

  public OrchestrationStepsModule(OrchestrationStepConfig configuration) {
    this.configuration = configuration;
  }

  public static OrchestrationStepsModule getInstance(OrchestrationStepConfig orchestrationStepConfig) {
    if (instance == null) {
      instance = new OrchestrationStepsModule(orchestrationStepConfig);
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(BarrierService.class).to(BarrierServiceImpl.class);
    bind(ResourceRestraintService.class).to(ResourceRestraintServiceImpl.class);
    bind(ResourceRestraintInstanceService.class).to(ResourceRestraintInstanceServiceImpl.class);
    bind(ResourceRestraintRegistry.class).to(ResourceRestraintRegistryImpl.class);
    bind(ApprovalInstanceService.class).to(ApprovalInstanceServiceImpl.class);
  }

  @Provides
  @Singleton
  @Named("cfPipelineAPI")
  CFApi providesCfAPI() {
    ApiClient apiClient = new ApiClient();
    if (configuration != null) {
      apiClient.setBasePath(configuration.getFfServerBaseUrl());
    }
    return new CFApi(apiClient);
  }

  @Provides
  @Singleton
  public OrchestrationStepConfig orchestrationStepsConfig() {
    return configuration;
  }
}
