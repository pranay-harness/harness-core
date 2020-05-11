package software.wings.beans.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.InvalidRequestException;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.codehaus.jackson.annotate.JsonIgnore;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.PhaseStep;
import software.wings.yaml.BaseYaml;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.validation.constraints.NotNull;

@OwnedBy(CDC)
@Data
@NoArgsConstructor
public class StepSkipStrategy {
  public enum Scope { ALL_STEPS, SPECIFIC_STEPS }

  @NotNull private Scope scope;
  private List<String> stepIds;
  @NotNull private String assertionExpression;
  @JsonIgnore @Transient private PhaseStep phaseStep;

  public StepSkipStrategy(Scope scope, List<String> stepIds, String assertionExpression) {
    this.scope = scope;
    this.stepIds = stepIds;
    this.assertionExpression = assertionExpression;
  }

  public boolean containsStepId(String stepId) {
    if (scope == Scope.ALL_STEPS) {
      return true;
    }

    return isNotEmpty(stepIds) && stepIds.contains(stepId);
  }

  public static void validateStepSkipStrategies(Collection<StepSkipStrategy> stepSkipStrategies) {
    if (isEmpty(stepSkipStrategies)) {
      return;
    }

    boolean hasAllStepsStrategy = false;
    Set<String> stepIds = new HashSet<>();
    for (StepSkipStrategy stepSkipStrategy : stepSkipStrategies) {
      if (stepSkipStrategy.getScope() == StepSkipStrategy.Scope.ALL_STEPS) {
        if (EmptyPredicate.isNotEmpty(stepIds)) {
          throw new InvalidRequestException("Cannot skip all steps as a skip condition already exists");
        }

        hasAllStepsStrategy = true;
      } else if (hasAllStepsStrategy && EmptyPredicate.isNotEmpty(stepSkipStrategy.getStepIds())) {
        throw new InvalidRequestException("Cannot add a skip condition as a skip all condition already exists");
      } else if (EmptyPredicate.isNotEmpty(stepSkipStrategy.getStepIds())) {
        for (String stepId : stepSkipStrategy.getStepIds()) {
          if (stepIds.contains(stepId)) {
            throw new InvalidRequestException("Multiple skip conditions for the same step");
          }

          stepIds.add(stepId);
        }
      }
    }
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends BaseYaml {
    private String scope;
    private List<String> steps;
    private String assertionExpression;

    @Builder
    public Yaml(String scope, List<String> steps, String assertionExpression) {
      this.scope = scope;
      this.steps = steps;
      this.assertionExpression = assertionExpression;
    }
  }
}
