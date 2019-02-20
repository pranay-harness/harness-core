/**
 *
 */

package software.wings.sm;

import io.harness.beans.OrchestrationWorkflowType;
import io.harness.beans.WorkflowType;
import io.harness.context.ContextElementType;
import software.wings.beans.ErrorStrategy;
import software.wings.beans.SweepingOutput;
import software.wings.beans.SweepingOutput.SweepingOutputBuilder;
import software.wings.settings.SettingValue;

import java.util.List;
import java.util.Map;

/**
 * The Interface ExecutionContext.
 *
 * @author Rishi
 */
public interface ExecutionContext {
  Object evaluateExpression(String expression);

  Object evaluateExpression(String expression, Object stateExecutionData);

  StateExecutionData getStateExecutionData();

  Map<String, Object> asMap();

  String renderExpression(String expression);

  String renderExpression(String expression, Object addition);

  String renderExpression(String expression, Object stateExecutionData, Object addition);

  List<String> renderExpressionList(List<String> expressions);

  List<String> renderExpressionList(List<String> expressions, String separator);

  <T extends ContextElement> T getContextElement();

  <T extends ContextElement> T getContextElement(ContextElementType contextElementType);

  <T extends ContextElement> T getContextElement(ContextElementType contextElementType, String name);

  <T extends ContextElement> List<T> getContextElementList(ContextElementType contextElementType);

  ErrorStrategy getErrorStrategy();

  String getWorkflowExecutionId();

  String getWorkflowId();

  String getWorkflowExecutionName();

  WorkflowType getWorkflowType();

  OrchestrationWorkflowType getOrchestrationWorkflowType();

  String getStateExecutionInstanceId();

  String getPipelineStateElementId();

  String getAppId();

  String getAccountId();

  String getStateExecutionInstanceName();

  Map<String, Object> getServiceVariables();

  Map<String, String> getSafeDisplayServiceVariables();

  SettingValue getGlobalSettingValue(String accountId, String settingId);

  Object evaluateExpression(String expression, Map<String, Object> context);

  String renderExpression(String expression, Map<String, Object> context);

  Map<String, Object> prepareContext(Object stateExecutionData);

  SweepingOutputBuilder prepareSweepingOutputBuilder(SweepingOutput.Scope sweepingOutputScope);
}
