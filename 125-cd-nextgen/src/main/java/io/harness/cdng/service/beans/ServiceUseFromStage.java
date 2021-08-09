package io.harness.cdng.service.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SwaggerConstants;
import io.harness.cdng.visitor.helpers.serviceconfig.ServiceUseFromOverridesVisitorHelper;
import io.harness.cdng.visitor.helpers.serviceconfig.ServiceUseFromStageVisitorHelper;
import io.harness.pms.yaml.ParameterField;
import io.harness.walktree.beans.VisitableChild;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.io.Serializable;
import java.util.Collections;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDC)
@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@SimpleVisitorHelper(helperClass = ServiceUseFromStageVisitorHelper.class)
@TypeAlias("serviceUseFromStage")
public class ServiceUseFromStage implements Serializable, Visitable {
  // Stage identifier of the stage to select from.
  @NotNull String stage;
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
  @TypeAlias("serviceUseFromStage_overrides")
  public static class Overrides implements Visitable {
    @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> name;
    @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> description;

    @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;
  }
}
