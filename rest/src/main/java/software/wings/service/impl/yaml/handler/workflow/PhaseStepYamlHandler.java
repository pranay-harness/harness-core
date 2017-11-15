package software.wings.service.impl.yaml.handler.workflow;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import software.wings.beans.ErrorCode;
import software.wings.beans.FailureStrategy;
import software.wings.beans.Graph.Node;
import software.wings.beans.ObjectType;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStep.PhaseStepBuilder;
import software.wings.beans.PhaseStep.Yaml;
import software.wings.beans.PhaseStepType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.sm.ExecutionStatus;
import software.wings.utils.Util;
import software.wings.yaml.workflow.StepYaml;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author rktummala on 10/28/17
 */
public class PhaseStepYamlHandler extends BaseYamlHandler<PhaseStep.Yaml, PhaseStep> {
  @Inject YamlHandlerFactory yamlHandlerFactory;

  @Override
  public PhaseStep createFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return setWithYamlValues(changeContext, changeSetContext, true);
  }

  private PhaseStep setWithYamlValues(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext,
      boolean isCreate) throws HarnessException {
    Yaml yaml = changeContext.getYaml();
    PhaseStepType phaseStepType = Util.getEnumFromString(PhaseStepType.class, yaml.getType());
    ExecutionStatus statusForRollback = Util.getEnumFromString(ExecutionStatus.class, yaml.getStatusForRollback());

    List<Node> stepList = Lists.newArrayList();
    if (yaml.getSteps() != null) {
      BaseYamlHandler stepYamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.STEP, ObjectType.STEP);

      // Steps
      stepList = yaml.getSteps()
                     .stream()
                     .map(stepYaml -> {
                       try {
                         ChangeContext.Builder clonedContext = cloneFileChangeContext(changeContext, stepYaml);
                         return (Node) createOrUpdateFromYaml(
                             isCreate, stepYamlHandler, clonedContext.build(), changeSetContext);
                       } catch (HarnessException e) {
                         throw new WingsException(e);
                       }
                     })
                     .collect(Collectors.toList());
    }

    // Failure strategies
    List<FailureStrategy> failureStrategies = Lists.newArrayList();
    if (yaml.getFailureStrategies() != null) {
      BaseYamlHandler failureStrategyYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.FAILURE_STRATEGY, ObjectType.FAILURE_STRATEGY);
      failureStrategies = yaml.getFailureStrategies()
                              .stream()
                              .map(failureStrategy -> {
                                try {
                                  ChangeContext.Builder clonedContext =
                                      cloneFileChangeContext(changeContext, failureStrategy);
                                  return (FailureStrategy) createOrUpdateFromYaml(
                                      isCreate, failureStrategyYamlHandler, clonedContext.build(), changeSetContext);
                                } catch (HarnessException e) {
                                  throw new WingsException(e);
                                }
                              })
                              .collect(Collectors.toList());
    }

    return PhaseStepBuilder.aPhaseStep(phaseStepType, yaml.getName())
        .addAllSteps(stepList)
        .withFailureStrategies(failureStrategies)
        .withPhaseStepNameForRollback(yaml.getPhaseStepNameForRollback())
        .withRollback(yaml.isRollback())
        .withStatusForRollback(statusForRollback)
        .withStepsInParallel(yaml.isStepsInParallel())
        .withWaitInterval(yaml.getWaitInterval())
        .build();
  }

  @Override
  public Yaml toYaml(PhaseStep bean, String appId) {
    // Failure strategies
    BaseYamlHandler failureStrategyYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.FAILURE_STRATEGY, ObjectType.FAILURE_STRATEGY);
    List<FailureStrategy> failureStrategies = bean.getFailureStrategies();
    List<FailureStrategy.Yaml> failureStrategyYamlList =
        failureStrategies.stream()
            .map(failureStrategy -> (FailureStrategy.Yaml) failureStrategyYamlHandler.toYaml(failureStrategy, appId))
            .collect(Collectors.toList());

    // Phase steps
    BaseYamlHandler stepYamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.STEP, ObjectType.STEP);
    List<StepYaml> stepsYamlList = bean.getSteps()
                                       .stream()
                                       .map(step -> (StepYaml) stepYamlHandler.toYaml(step, appId))
                                       .collect(Collectors.toList());

    return Yaml.Builder.anYaml()
        .withFailureStrategies(failureStrategyYamlList)
        .withName(bean.getName())
        .withPhaseStepNameForRollback(bean.getPhaseStepNameForRollback())
        .withRollback(bean.isRollback())
        .withStatusForRollback(bean.getStatusForRollback() != null ? bean.getStatusForRollback().name() : null)
        .withStepsInParallel(bean.isStepsInParallel())
        .withSteps(stepsYamlList)
        .withType(bean.getPhaseStepType().name())
        .withWaitInterval(bean.getWaitInterval())
        .build();
  }

  @Override
  public PhaseStep updateFromYaml(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    return setWithYamlValues(changeContext, changeSetContext, false);
  }

  @Override
  public boolean validate(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    return true;
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
  public PhaseStep update(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext)
      throws HarnessException {
    throw new WingsException(ErrorCode.UNSUPPORTED_OPERATION_EXCEPTION);
  }
}
