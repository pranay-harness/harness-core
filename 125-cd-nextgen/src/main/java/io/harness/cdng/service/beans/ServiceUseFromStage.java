package io.harness.cdng.service.beans;

import io.harness.beans.ParameterField;
import io.harness.cdng.visitor.helpers.serviceconfig.ServiceUseFromOverridesVisitorHelper;
import io.harness.cdng.visitor.helpers.serviceconfig.ServiceUseFromStageVisitorHelper;
import io.harness.common.SwaggerConstants;
import io.harness.walktree.beans.VisitableChild;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.Collections;
import javax.validation.constraints.NotNull;

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
  }
}
