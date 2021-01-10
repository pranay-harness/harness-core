package io.harness.cdng.service.beans;

import io.harness.cdng.visitor.YamlTypes;
import io.harness.cdng.visitor.helpers.serviceconfig.ServiceConfigVisitorHelper;
import io.harness.pms.yaml.ParameterField;
import io.harness.validation.OneOfField;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.intfc.OverridesApplier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Wither;

@Data
@Builder
@OneOfField(fields = {"service", "serviceRef"})
@SimpleVisitorHelper(helperClass = ServiceConfigVisitorHelper.class)
public class ServiceConfig implements OverridesApplier<ServiceConfig>, Visitable {
  @Wither private ServiceUseFromStage useFromStage;

  @Wither private ServiceYaml service;
  private ParameterField<String> serviceRef;
  private ServiceDefinition serviceDefinition;
  @Wither private StageOverridesConfig stageOverrides;
  @Wither Map<String, String> tags;

  // For Visitor Framework Impl
  String metadata;

  @JsonIgnore
  public ServiceConfig applyUseFromStage(ServiceConfig serviceConfigToUseFrom) {
    return serviceConfigToUseFrom.withStageOverrides(stageOverrides).withUseFromStage(useFromStage);
  }

  @Override
  public ServiceConfig applyOverrides(ServiceConfig overrideConfig) {
    ServiceYaml resultantConfigService = service;
    ServiceYaml overrideConfigService = overrideConfig.getService();
    if (overrideConfigService.getName() != null) {
      resultantConfigService = resultantConfigService.withName(overrideConfigService.getName());
    }
    if (overrideConfigService.getDescription() != null) {
      resultantConfigService = resultantConfigService.withDescription(overrideConfigService.getDescription());
    }

    ServiceConfig resultantConfig = this;
    resultantConfig = resultantConfig.withService(resultantConfigService);
    return resultantConfig;
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    children.add("service", service);
    children.add("serviceDefinition", serviceDefinition);
    children.add("useFromStage", useFromStage);
    children.add("stageOverrides", stageOverrides);
    return children;
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(YamlTypes.SERVICE_CONFIG).build();
  }
}
