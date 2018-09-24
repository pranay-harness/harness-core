package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.util.stream.Collectors.toList;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import software.wings.beans.FailureStrategy;
import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStep.PhaseStepBuilder;
import software.wings.beans.PhaseStep.Yaml;
import software.wings.beans.PhaseStepType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlConstants;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.HarnessException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.sm.ExecutionStatus;
import software.wings.utils.Util;
import software.wings.yaml.workflow.StepYaml;

import java.util.List;

/**
 * @author rktummala on 10/28/17
 */
@Singleton
public class PhaseStepYamlHandler extends BaseYamlHandler<PhaseStep.Yaml, PhaseStep> {
  @Inject YamlHandlerFactory yamlHandlerFactory;

  private PhaseStep toBean(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    Yaml yaml = changeContext.getYaml();
    PhaseStepType phaseStepType = Util.getEnumFromString(PhaseStepType.class, yaml.getType());
    String phaseStepUuid = generateUuid();
    PhaseStepBuilder phaseStepBuilder = PhaseStepBuilder.aPhaseStep(phaseStepType, yaml.getName(), phaseStepUuid);

    ExecutionStatus statusForRollback = Util.getEnumFromString(ExecutionStatus.class, yaml.getStatusForRollback());

    List<GraphNode> stepList = Lists.newArrayList();
    if (yaml.getSteps() != null) {
      StepYamlHandler stepYamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.STEP);

      // Steps
      stepList = yaml.getSteps()
                     .stream()
                     .map(stepYaml -> {
                       try {
                         ChangeContext.Builder clonedContextBuilder = cloneFileChangeContext(changeContext, stepYaml);
                         ChangeContext clonedContext = clonedContextBuilder.build();
                         clonedContext.getEntityIdMap().put("PHASE_STEP", phaseStepUuid);
                         return stepYamlHandler.upsertFromYaml(clonedContext, changeSetContext);
                       } catch (HarnessException e) {
                         throw new WingsException(e);
                       }
                     })
                     .collect(toList());
    }

    // Failure strategies
    List<FailureStrategy> failureStrategies = Lists.newArrayList();
    if (yaml.getFailureStrategies() != null) {
      FailureStrategyYamlHandler failureStrategyYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.FAILURE_STRATEGY);
      failureStrategies =
          yaml.getFailureStrategies()
              .stream()
              .map(failureStrategy -> {
                try {
                  ChangeContext.Builder clonedContext = cloneFileChangeContext(changeContext, failureStrategy);
                  return failureStrategyYamlHandler.upsertFromYaml(clonedContext.build(), changeSetContext);
                } catch (HarnessException e) {
                  throw new WingsException(e);
                }
              })
              .collect(toList());
    }

    Boolean isRollback = (Boolean) changeContext.getProperties().get(YamlConstants.IS_ROLLBACK);
    return phaseStepBuilder.addAllSteps(stepList)
        .withFailureStrategies(failureStrategies)
        .withPhaseStepNameForRollback(yaml.getPhaseStepNameForRollback())
        .withRollback(isRollback)
        .withStatusForRollback(statusForRollback)
        .withStepsInParallel(yaml.isStepsInParallel())
        .withWaitInterval(yaml.getWaitInterval())
        .build();
  }

  @Override
  public Yaml toYaml(PhaseStep bean, String appId) {
    // Failure strategies
    FailureStrategyYamlHandler failureStrategyYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.FAILURE_STRATEGY);
    List<FailureStrategy> failureStrategies = bean.getFailureStrategies();
    List<FailureStrategy.Yaml> failureStrategyYamlList =
        failureStrategies.stream()
            .map(failureStrategy -> failureStrategyYamlHandler.toYaml(failureStrategy, appId))
            .collect(toList());

    // Phase steps
    StepYamlHandler stepYamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.STEP);
    List<StepYaml> stepsYamlList =
        bean.getSteps().stream().map(step -> stepYamlHandler.toYaml(step, appId)).collect(toList());

    return Yaml.builder()
        .failureStrategies(failureStrategyYamlList)
        .name(bean.getName())
        .phaseStepNameForRollback(bean.getPhaseStepNameForRollback())
        .statusForRollback(bean.getStatusForRollback() != null ? bean.getStatusForRollback().name() : null)
        .stepsInParallel(bean.isStepsInParallel())
        .steps(stepsYamlList)
        .type(bean.getPhaseStepType().name())
        .waitInterval(bean.getWaitInterval())
        .build();
  }

  @Override
  public PhaseStep upsertFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return toBean(changeContext, changeSetContext);
  }

  @Override
  public Class getYamlClass() {
    return PhaseStep.Yaml.class;
  }

  @Override
  public PhaseStep get(String accountId, String yamlFilePath) {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }

  @Override
  public void delete(ChangeContext<Yaml> changeContext) throws HarnessException {
    // Do nothing
  }
}
