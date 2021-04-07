package io.harness.yaml.extended.ci.codebase.impl;

import static io.harness.common.SwaggerConstants.STRING_CLASSPATH;
import static io.harness.yaml.extended.ci.codebase.BuildTypeConstants.BRANCH_TYPE;

import io.harness.pms.yaml.ParameterField;
import io.harness.yaml.extended.ci.codebase.BuildSpec;

import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;
import wiremock.com.fasterxml.jackson.annotation.JsonTypeName;

@Value
@Builder
@TypeAlias("io.harness.yaml.extended.ci.impl.BranchBuildSpec")
@JsonTypeName(BRANCH_TYPE)
public class BranchBuildSpec implements BuildSpec {
  @NotNull @ApiModelProperty(dataType = STRING_CLASSPATH) ParameterField<String> branch;
}
