package io.harness.plancreator.steps.http;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.steps.StepSpecTypeConstants;
import io.harness.validation.OneOfSet;
import io.harness.yaml.core.StepSpecType;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import javax.validation.constraints.NotNull;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.TypeAlias;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName(StepSpecTypeConstants.HTTP)
@TypeAlias("HttpStepNode")
@OwnedBy(PIPELINE)
@RecasterAlias("io.harness.plancreator.steps.http.HttpStepNode")
@OneOfSet(fields = {"type, httpStepInfo, timeout, failureStrategies, when, delegateSelectors", "template"})
public class HttpStepNode extends PmsAbstractStepNode {
  @JsonProperty("type") @NotNull StepType type = StepType.Http;
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

  // will re-iterate
  enum StepType {
    Http(StepSpecTypeConstants.HTTP);
    @Getter String name;
    StepType(String name) {
      this.name = name;
    }
  }
}
