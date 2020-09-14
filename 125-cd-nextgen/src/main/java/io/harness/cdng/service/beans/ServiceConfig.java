package io.harness.cdng.service.beans;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.harness.cdng.visitor.helpers.serviceconfig.ServiceConfigVisitorHelper;
import io.harness.utils.ParameterField;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.intfc.OverridesApplier;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Wither;

import javax.validation.constraints.NotNull;

@Data
@Builder
@SimpleVisitorHelper(helperClass = ServiceConfigVisitorHelper.class)
public class ServiceConfig implements OverridesApplier<ServiceConfig>, Visitable {
  @Wither private ServiceUseFromStage useFromStage;
  @NotNull private ParameterField<String> identifier;
  @Wither private ParameterField<String> name;
  @Wither private ParameterField<String> description;
  private ServiceDefinition serviceDefinition;
  @Wither private StageOverridesConfig stageOverrides;

  // For Visitor Framework Impl
  String metadata;

  @JsonIgnore
  public ServiceConfig applyUseFromStage(ServiceConfig serviceConfigToUseFrom) {
    return serviceConfigToUseFrom.withStageOverrides(stageOverrides).withUseFromStage(useFromStage);
  }

  @Override
  public ServiceConfig applyOverrides(ServiceConfig overrideConfig) {
    ServiceConfig resultantConfig = this;
    if (overrideConfig.getName() != null) {
      resultantConfig = resultantConfig.withName(overrideConfig.getName());
    }
    if (overrideConfig.getDescription() != null) {
      resultantConfig = resultantConfig.withDescription(overrideConfig.getDescription());
    }
    return resultantConfig;
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    children.add("serviceDefinition", serviceDefinition);
    return children;
  }
}
