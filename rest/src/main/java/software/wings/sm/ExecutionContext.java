/**
 *
 */

package software.wings.sm;

import software.wings.beans.ErrorStrategy;
import software.wings.beans.OrchestrationWorkflowType;
import software.wings.beans.WorkflowType;
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

  String getStateExecutionInstanceName();

  Map<String, String> getServiceVariables();

  Map<String, String> getSafeDisplayServiceVariables();

  SettingValue getGlobalSettingValue(String accountId, String settingId);

  Object evaluateExpression(String expression, Map<String, Object> context);

  String renderExpression(String expression, Map<String, Object> context);

  Map<String, Object> prepareContext(Object stateExecutionData);
}
