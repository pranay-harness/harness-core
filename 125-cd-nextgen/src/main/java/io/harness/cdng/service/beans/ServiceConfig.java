package io.harness.cdng.service.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.common.SwaggerConstants;
import io.harness.cdng.visitor.helpers.serviceconfig.ServiceConfigVisitorHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.pms.yaml.ParameterField;
import io.harness.validation.OneOfField;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.intfc.OverridesApplier;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Wither;

@Data
@Builder
@OneOfField(fields = {"service", "serviceRef"})
@SimpleVisitorHelper(helperClass = ServiceConfigVisitorHelper.class)
@OwnedBy(CDC)
public class ServiceConfig implements OverridesApplier<ServiceConfig>, Visitable {
  @Wither private ServiceUseFromStage useFromStage;

  @Wither private ServiceYaml service;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) private ParameterField<String> serviceRef;
  private ServiceDefinition serviceDefinition;
  @Wither private StageOverridesConfig stageOverrides;

  // For Visitor Framework Impl
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  @JsonIgnore
  public ServiceConfig applyUseFromStage(ServiceConfig serviceConfigToUseFrom) {
    return serviceConfigToUseFrom.withStageOverrides(stageOverrides).withUseFromStage(useFromStage);
  }

  @Override
  public ServiceConfig applyOverrides(ServiceConfig overrideConfig) {
    ServiceYaml resultantConfigService = service;
    ServiceYaml overrideConfigService = overrideConfig.getService();
    if (EmptyPredicate.isNotEmpty(overrideConfigService.getName())) {
      resultantConfigService = resultantConfigService.withName(overrideConfigService.getName());
    }
    if (!ParameterField.isNull(overrideConfigService.getDescription())) {
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
}
