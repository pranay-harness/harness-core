package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.validation.Validator.notEmptyCheck;
import static io.harness.validation.Validator.notNullCheck;

import com.google.inject.Singleton;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.UnsupportedOperationException;
import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.beans.workflow.StepSkipStrategy;
import software.wings.beans.workflow.StepSkipStrategy.Scope;
import software.wings.beans.workflow.StepSkipStrategy.Yaml;
import software.wings.beans.yaml.ChangeContext;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.utils.Utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@OwnedBy(CDC)
@Singleton
public class StepSkipStrategyYamlHandler extends BaseYamlHandler<Yaml, StepSkipStrategy> {
  private StepSkipStrategy toBean(ChangeContext<Yaml> changeContext) {
    Yaml yaml = changeContext.getYaml();
    String yamlFilePath = changeContext.getChange().getFilePath();
    Scope scope = Utils.getEnumFromString(Scope.class, yaml.getScope());
    notNullCheck("Invalid scope in yaml file path: " + yamlFilePath, scope);
    notEmptyCheck("Invalid assertion expression in yaml file path: " + yamlFilePath, yaml.getAssertionExpression());

    List<String> stepIds = null;
    if (Scope.SPECIFIC_STEPS.name().equals(yaml.getScope()) && isNotEmpty(yaml.getSteps())) {
      PhaseStep phaseStep =
          (PhaseStep) changeContext.getProperties().get(PhaseStepYamlHandler.PHASE_STEP_PROPERTY_NAME);
      notNullCheck("Invalid phase step in yaml file path: " + yamlFilePath, phaseStep);
      if (isNotEmpty(phaseStep.getSteps())) {
        stepIds = new ArrayList<>();
        Map<String, String> nameToIdMap =
            phaseStep.getSteps().stream().collect(Collectors.toMap(GraphNode::getName, GraphNode::getId));
        for (String stepName : yaml.getSteps()) {
          String stepId = nameToIdMap.get(stepName);
          notNullCheck("Invalid step name in yaml file path: " + yamlFilePath, stepId);
          stepIds.add(stepId);
        }
      }
    }

    return new StepSkipStrategy(scope, stepIds, yaml.getAssertionExpression());
  }

  @Override
  public Yaml toYaml(StepSkipStrategy bean, String appId) {
    List<String> steps = null;
    if (bean.getScope() == Scope.SPECIFIC_STEPS && isNotEmpty(bean.getStepIds())) {
      PhaseStep phaseStep = bean.getPhaseStep();
      notNullCheck("Invalid phase step", phaseStep);
      if (isNotEmpty(phaseStep.getSteps())) {
        steps = new ArrayList<>();
        Map<String, String> idToNameMap =
            phaseStep.getSteps().stream().collect(Collectors.toMap(GraphNode::getId, GraphNode::getName));
        for (String stepId : bean.getStepIds()) {
          String stepName = idToNameMap.get(stepId);
          if (stepName != null) {
            steps.add(stepName);
          }
        }
      }
    }

    return Yaml.builder()
        .scope(bean.getScope().name())
        .steps(steps)
        .assertionExpression(bean.getAssertionExpression())
        .build();
  }

  @Override
  public StepSkipStrategy upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    return toBean(changeContext);
  }

  @Override
  public Class getYamlClass() {
    return Yaml.class;
  }

  @Override
  public StepSkipStrategy get(String accountId, String yamlFilePath) {
    throw new UnsupportedOperationException("get method not supported for StepSkipStrategyYamlHandler");
  }

  @Override
  public void delete(ChangeContext<Yaml> changeContext) {
    // Do nothing.
  }
}
