package io.harness.yaml.extended.ci.codebase.impl;

import static io.harness.annotations.dev.HarnessTeam.CI;
import static io.harness.beans.SwaggerConstants.STRING_CLASSPATH;
import static io.harness.yaml.extended.ci.codebase.BuildTypeConstants.BRANCH_TYPE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.extended.ci.codebase.BuildSpec;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.codehaus.jackson.annotate.JsonTypeName;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("io.harness.yaml.extended.ci.impl.BranchBuildSpec")
@JsonTypeName(BRANCH_TYPE)
@OwnedBy(CI)
public class BranchBuildSpec implements BuildSpec {
  @NotNull @ApiModelProperty(dataType = STRING_CLASSPATH) ParameterField<String> branch;
}
