package software.wings.beans.workflow;

import io.harness.yaml.BaseYaml;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public final class StepSkipStrategyYaml extends BaseYaml {
  private String scope;
  private List<String> steps;
  private String assertionExpression;

  @Builder
  public StepSkipStrategyYaml(String scope, List<String> steps, String assertionExpression) {
    this.scope = scope;
    this.steps = steps;
    this.assertionExpression = assertionExpression;
  }
}
