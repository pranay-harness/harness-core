package io.harness.cdng.environment.yaml;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.common.SwaggerConstants;
import io.harness.cdng.visitor.YamlTypes;
import io.harness.cdng.visitor.helpers.pipelineinfrastructure.EnvironmentYamlVisitorHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.ng.core.environment.beans.EnvironmentType;
import io.harness.pms.yaml.ParameterField;
import io.harness.walktree.beans.LevelNode;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.intfc.OverridesApplier;

import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Wither;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@SimpleVisitorHelper(helperClass = EnvironmentYamlVisitorHelper.class)
@TypeAlias("environmentYaml")
@OwnedBy(CDC)
public class EnvironmentYaml implements OverridesApplier<EnvironmentYaml>, Visitable {
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither String name;
  @NotNull @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither String identifier;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) @Wither private ParameterField<String> description;
  @NotNull @Wither EnvironmentType type;
  @Wither Map<String, String> tags;

  // For Visitor Framework Impl
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) }) @ApiModelProperty(hidden = true) String metadata;

  @Override
  public EnvironmentYaml applyOverrides(EnvironmentYaml overrideConfig) {
    EnvironmentYaml resultant = this;
    if (EmptyPredicate.isNotEmpty(overrideConfig.getName())) {
      resultant = resultant.withName(overrideConfig.getName());
    }
    if (EmptyPredicate.isNotEmpty(overrideConfig.getIdentifier())) {
      resultant = resultant.withIdentifier(overrideConfig.getIdentifier());
    }
    if (!ParameterField.isNull(overrideConfig.getDescription())) {
      resultant = resultant.withDescription(overrideConfig.getDescription());
    }
    if (overrideConfig.getType() != null) {
      resultant = resultant.withType(overrideConfig.getType());
    }
    if (EmptyPredicate.isNotEmpty(overrideConfig.getTags())) {
      resultant = resultant.withTags(overrideConfig.getTags());
    }
    return resultant;
  }

  @Override
  public LevelNode getLevelNode() {
    return LevelNode.builder().qualifierName(YamlTypes.ENVIRONMENT_YAML).build();
  }
}
