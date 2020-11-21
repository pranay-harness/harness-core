package io.harness.cdng.service.beans;

import io.harness.beans.ParameterField;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.cdng.visitor.helpers.serviceconfig.ServiceUseFromOverridesVisitorHelper;
import io.harness.cdng.visitor.helpers.serviceconfig.ServiceUseFromStageVisitorHelper;
import io.harness.common.SwaggerConstants;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.beans.VisitableChild;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.util.Collections;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@SimpleVisitorHelper(helperClass = ServiceUseFromStageVisitorHelper.class)
public class ServiceUseFromStage implements Serializable, Visitable {
  // Stage identifier of the stage to select from.
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @NotNull ParameterField<String> stage;
  Overrides overrides;

  // For Visitor Framework Impl
  String metadata;

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(YamlTypes.SERVICE_USE_FROM_STAGE).build();
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChild child = VisitableChild.builder().fieldName("overrides").value(overrides).build();
    return VisitableChildren.builder().visitableChildList(Collections.singletonList(child)).build();
  }

  @Data
  @Builder
  @ApiModel(value = "ServiceOverrides")
  @SimpleVisitorHelper(helperClass = ServiceUseFromOverridesVisitorHelper.class)
  public static class Overrides implements Visitable {
    @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> name;
    @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> description;

    @Override
    public LevelNode getLevelNode() {
      return LevelNode.builder().qualifierName(YamlTypes.SERVICE_USE_FROM_STAGE_OVERRIDES).build();
    }
  }
}
