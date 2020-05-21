/**
 *
 */

package software.wings.sm;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.OrchestrationWorkflowType;
import io.harness.beans.SweepingOutputInstance;
import io.harness.beans.SweepingOutputInstance.SweepingOutputInstanceBuilder;
import io.harness.beans.WorkflowType;
import io.harness.context.ContextElementType;
import io.harness.logging.AutoLogContext;
import software.wings.api.InfraMappingElement;
import software.wings.api.ServiceElement;
import software.wings.api.instancedetails.InstanceApiResponse;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.ErrorStrategy;
import software.wings.service.intfc.sweepingoutput.SweepingOutputInquiry.SweepingOutputInquiryBuilder;
import software.wings.settings.SettingValue;

import java.util.List;
import java.util.Map;

/**
 * The Interface ExecutionContext.
 *
 * @author Rishi
 */
@OwnedBy(CDC)
public interface ExecutionContext {
  Object evaluateExpression(String expression);

  Object evaluateExpression(String expression, StateExecutionContext stateExecutionContext);

  StateExecutionData getStateExecutionData();

  boolean isRetry();

  int retryCount();

  Map<String, Object> asMap();

  void resetPreparedCache();

  String renderExpression(String expression);

  String renderExpression(String expression, StateExecutionContext stateExecutionContext);

  List<String> renderExpressionList(List<String> expressions);

  List<String> renderExpressionList(List<String> expressions, String separator);

  <T extends ContextElement> T getContextElement();

  <T extends ContextElement> T getContextElement(ContextElementType contextElementType);

  <T extends ContextElement> T getContextElement(ContextElementType contextElementType, String name);

  <T extends ContextElement> List<T> getContextElementList(ContextElementType contextElementType);

  ErrorStrategy getErrorStrategy();

  Application getApp();

  Application fetchRequiredApp();

  Environment fetchRequiredEnvironment();

  String getWorkflowExecutionId();

  String getWorkflowId();

  String getWorkflowExecutionName();

  WorkflowType getWorkflowType();

  OrchestrationWorkflowType getOrchestrationWorkflowType();

  String getStateExecutionInstanceId();

  String getPipelineStageElementId();

  int getPipelineStageParallelIndex();

  String getPipelineStageName();

  String getAppId();

  String getAccountId();

  String getStateExecutionInstanceName();

  Map<String, Object> getServiceVariables();

  Map<String, String> getSafeDisplayServiceVariables();

  SettingValue getGlobalSettingValue(String accountId, String settingId);

  SweepingOutputInstanceBuilder prepareSweepingOutputBuilder(SweepingOutputInstance.Scope sweepingOutputScope);

  SweepingOutputInquiryBuilder prepareSweepingOutputInquiryBuilder();

  InfraMappingElement fetchInfraMappingElement();

  String fetchInfraMappingId();

  ServiceElement fetchServiceElement();

  AutoLogContext autoLogContext();

  InstanceApiResponse renderExpressionsForInstanceDetails(String expression, boolean newInstancesOnly);

  InstanceApiResponse renderExpressionsForInstanceDetailsForWorkflow(String expression, boolean newInstancesOnly);

  String appendStateExecutionId(String str);
}
