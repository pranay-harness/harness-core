/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.UUIDGenerator.generateUuid;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.ExecutionStatus;
import io.harness.eraro.ErrorCode;
import io.harness.exception.HarnessException;
import io.harness.exception.WingsException;

import software.wings.beans.FailureStrategy;
import software.wings.beans.GraphNode;
import software.wings.beans.PhaseStep;
import software.wings.beans.PhaseStep.PhaseStepBuilder;
import software.wings.beans.PhaseStep.Yaml;
import software.wings.beans.PhaseStepType;
import software.wings.beans.workflow.StepSkipStrategy;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlConstants;
import software.wings.beans.yaml.YamlType;
import software.wings.service.impl.workflow.WorkflowServiceHelper;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.utils.Utils;
import software.wings.yaml.workflow.StepYaml;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

/**
 * @author rktummala on 10/28/17
 */
@OwnedBy(CDC)
@Singleton
public class PhaseStepYamlHandler extends BaseYamlHandler<PhaseStep.Yaml, PhaseStep> {
  public static final String PHASE_STEP_PROPERTY_NAME = "PHASE_STEP";

  @Inject YamlHandlerFactory yamlHandlerFactory;
  @Inject WorkflowServiceHelper workflowServiceHelper;

  private PhaseStep toBean(ChangeContext<Yaml> changeContext, List<ChangeContext> changeSetContext) {
    Yaml yaml = changeContext.getYaml();
    PhaseStepType phaseStepType = Utils.getEnumFromString(PhaseStepType.class, yaml.getType());
    String phaseStepUuid = generateUuid();
    PhaseStepBuilder phaseStepBuilder = PhaseStepBuilder.aPhaseStep(phaseStepType, yaml.getName(), phaseStepUuid);

    ExecutionStatus statusForRollback = Utils.getEnumFromString(ExecutionStatus.class, yaml.getStatusForRollback());

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
    PhaseStep phaseStep = phaseStepBuilder.addAllSteps(stepList)
                              .withFailureStrategies(failureStrategies)
                              .withPhaseStepNameForRollback(yaml.getPhaseStepNameForRollback())
                              .withRollback(isRollback)
                              .withStatusForRollback(statusForRollback)
                              .withStepsInParallel(yaml.isStepsInParallel())
                              .withWaitInterval(yaml.getWaitInterval())
                              .build();

    workflowServiceHelper.validateWaitInterval(phaseStep);

    List<StepSkipStrategy> stepSkipStrategies = new ArrayList<>();
    if (yaml.getStepSkipStrategies() != null) {
      StepSkipStrategyYamlHandler stepSkipStrategyYamlHandler =
          yamlHandlerFactory.getYamlHandler(YamlType.STEP_SKIP_STRATEGY);
      stepSkipStrategies = yaml.getStepSkipStrategies()
                               .stream()
                               .map(stepSkipStrategy -> {
                                 ChangeContext<StepSkipStrategy.Yaml> clonedContext =
                                     cloneFileChangeContext(changeContext, stepSkipStrategy).build();
                                 clonedContext.getProperties().put(PHASE_STEP_PROPERTY_NAME, phaseStep);
                                 return stepSkipStrategyYamlHandler.upsertFromYaml(clonedContext, changeSetContext);
                               })
                               .collect(toList());
    }

    StepSkipStrategy.validateStepSkipStrategies(stepSkipStrategies);
    phaseStep.setStepSkipStrategies(stepSkipStrategies);
    return phaseStep;
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

    // Step skip strategies
    StepSkipStrategyYamlHandler stepSkipStrategyYamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.STEP_SKIP_STRATEGY);
    List<StepSkipStrategy> stepSkipStrategies = bean.getStepSkipStrategies();
    List<StepSkipStrategy.Yaml> stepSkipStrategyYamlList =
        stepSkipStrategies.stream()
            .map(stepSkipStrategy -> {
              stepSkipStrategy.setPhaseStep(bean);
              return stepSkipStrategyYamlHandler.toYaml(stepSkipStrategy, appId);
            })
            .collect(toList());

    // Phase steps
    StepYamlHandler stepYamlHandler = yamlHandlerFactory.getYamlHandler(YamlType.STEP);
    List<StepYaml> stepsYamlList =
        bean.getSteps().stream().map(step -> stepYamlHandler.toYaml(step, appId)).collect(toList());

    return Yaml.builder()
        .failureStrategies(failureStrategyYamlList)
        .stepSkipStrategies(stepSkipStrategyYamlList)
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
  public void delete(ChangeContext<Yaml> changeContext) {
    // Do nothing
  }
}
