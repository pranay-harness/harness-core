/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.sm.states;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.ExecutionStatus.SKIPPED;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.RepairActionCode;

import software.wings.api.SkipStateExecutionData;
import software.wings.beans.Workflow;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.StateExecutionContext;
import software.wings.sm.WorkflowStandardParams;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.apache.commons.jexl3.JexlException;
import org.slf4j.Logger;

@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public interface WorkflowState {
  List<String> getRuntimeInputVariables();
  long getTimeout();
  List<String> getUserGroupIds();
  RepairActionCode getTimeoutAction();
  Map<String, String> getWorkflowVariables();
  boolean isContinued();
  void setContinued(boolean continued);

  String getPipelineStageElementId();
  int getPipelineStageParallelIndex();
  String getStageName();

  String getName();

  String getDisableAssertion();

  String getWorkflowId();

  default ExecutionResponse checkDisableAssertion(
      ExecutionContextImpl context, WorkflowService workflowService, Logger log) {
    String disableAssertion = getDisableAssertion();
    String workflowId = getWorkflowId();
    SkipStateExecutionData skipStateExecutionData = SkipStateExecutionData.builder().workflowId(workflowId).build();
    Workflow workflow = workflowService.readWorkflowWithoutServices(context.getAppId(), workflowId);

    if (workflow == null || workflow.getOrchestrationWorkflow() == null) {
      return ExecutionResponse.builder()
          .executionStatus(FAILED)
          .errorMessage("Workflow does not exist")
          .stateExecutionData(skipStateExecutionData)
          .build();
    }
    if (disableAssertion != null && disableAssertion.equals("true")) {
      return ExecutionResponse.builder()
          .executionStatus(SKIPPED)
          .errorMessage(getName() + " step in " + context.getPipelineStageName() + " has been skipped")
          .stateExecutionData(skipStateExecutionData)
          .build();
    }

    if (isNotEmpty(disableAssertion)) {
      try {
        if (context.getStateExecutionInstance() != null
            && isNotEmpty(context.getStateExecutionInstance().getContextElements())) {
          WorkflowStandardParams stdParams =
              (WorkflowStandardParams) context.getStateExecutionInstance().getContextElements().get(0);
          if (stdParams.getWorkflowElement() != null) {
            stdParams.getWorkflowElement().setName(workflow.getName());
            stdParams.getWorkflowElement().setDescription(workflow.getDescription());
          }
        }

        Object resultObj = context.evaluateExpression(
            disableAssertion, StateExecutionContext.builder().stateExecutionData(skipStateExecutionData).build());
        // rendering expression in order to have it tracked
        context.renderExpression(disableAssertion);
        if (!(resultObj instanceof Boolean)) {
          return ExecutionResponse.builder()
              .executionStatus(FAILED)
              .errorMessage("Skip Assertion Evaluation Failed : Expression '" + disableAssertion
                  + "' did not return a boolean value")
              .stateExecutionData(skipStateExecutionData)
              .build();
        }

        boolean assertionResult = (boolean) resultObj;
        if (assertionResult) {
          return ExecutionResponse.builder()
              .executionStatus(SKIPPED)
              .errorMessage(getName() + " step in " + context.getPipelineStageName()
                  + " has been skipped based on assertion expression [" + disableAssertion + "]")
              .stateExecutionData(skipStateExecutionData)
              .build();
        }
      } catch (JexlException je) {
        log.error("Skip Assertion Evaluation Failed", je);
        String jexlError = Optional.ofNullable(je.getMessage()).orElse("");
        if (jexlError.contains(":")) {
          jexlError = jexlError.split(":")[1];
        }
        if (je instanceof JexlException.Variable
            && ((JexlException.Variable) je).getVariable().equals("sweepingOutputSecrets")) {
          jexlError = "Secret Variables defined in Script output of shell scripts cannot be used in assertions";
        }
        return ExecutionResponse.builder()
            .executionStatus(FAILED)
            .errorMessage("Skip Assertion Evaluation Failed : " + jexlError)
            .stateExecutionData(skipStateExecutionData)
            .build();
      } catch (Exception e) {
        log.error("Skip Assertion Evaluation Failed", e);
        return ExecutionResponse.builder()
            .executionStatus(FAILED)
            .errorMessage("Skip Assertion Evaluation Failed : " + (e.getMessage() != null ? e.getMessage() : ""))
            .stateExecutionData(skipStateExecutionData)
            .build();
      }
    }

    return null;
  }
}
