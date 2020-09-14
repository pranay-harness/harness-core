package io.harness.cdng.service.beans;

import io.harness.cdng.visitor.helpers.serviceconfig.ServiceUseFromOverridesVisitorHelper;
import io.harness.cdng.visitor.helpers.serviceconfig.ServiceUseFromStageVisitorHelper;
import io.harness.utils.ParameterField;
import io.harness.walktree.beans.VisitableChild;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.swagger.annotations.ApiModel;
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
  @NotNull ParameterField<String> stage;
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
    ParameterField<String> name;
    ParameterField<String> description;

    @Override
    public VisitableChildren getChildrenToWalk() {
      return null;
    }
  }
}
