package io.harness.plancreator.steps.http;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.yaml.core.StepSpecType;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

import javax.validation.constraints.NotNull;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;
import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(StepSpecTypeConstants.HTTP)
@TypeAlias("HttpStepNode")
@OwnedBy(PIPELINE)
@RecasterAlias("io.harness.plancreator.steps.http.HttpStepNode")
public class HttpStepNode extends PmsAbstractStepNode {
  @NotNull String type;
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  HttpStepInfo httpStepInfo;

  @Override
  public String getType() {
    return StepSpecTypeConstants.HTTP;
  }

  @Override
  public StepSpecType getStepSpecType() {
    return httpStepInfo;
  }
}
