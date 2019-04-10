package software.wings.delegatetasks.servicenow;

import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.task.TaskParameters;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.cloudformation.ServiceNowExecutionData;
import software.wings.beans.DelegateTaskResponse;
import software.wings.beans.servicenow.ServiceNowTaskParameters;
import software.wings.delegatetasks.AbstractDelegateRunnableTask;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class ServicenowTask extends AbstractDelegateRunnableTask {
  private static final Logger logger = LoggerFactory.getLogger(ServicenowTask.class);

  public ServicenowTask(String delegateId, DelegateTask delegateTask, Consumer<DelegateTaskResponse> consumer,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, consumer, preExecute);
  }

  @Override
  public ResponseData run(Object[] parameters) {
    throw new NotImplementedException("Not implemented");
  }

  @Override
  public ResponseData run(TaskParameters parameters) {
    return run((ServiceNowTaskParameters) parameters);
  }

  private ResponseData run(ServiceNowTaskParameters parameters) {
    ServiceNowAction action = parameters.getAction();

    ResponseData responseData = null;

    logger.info("Executing ServiceNowTask. Action: {}, IssueNumber: {}", action, parameters.getIssueNumber());

    switch (parameters.getAction()) {
      case CHECK_APPROVAL:
        responseData = checkServiceNowApproval(parameters);
        break;
      default:
        logger.error("Invalid Service Now delegate Task Action {}", parameters.getAction());
    }

    if (responseData != null) {
      logger.info("Done executing ServiceNowTask. Action: {}, IssueId: {}, ExecutionStatus: {}", action,
          parameters.getIssueNumber(), ((ServiceNowExecutionData) responseData).getExecutionStatus());
    } else {
      logger.error("ServiceNowTask Action: {}. IssueId: {}. null response.", action, parameters.getIssueNumber());
    }

    return responseData;
  }

  private ResponseData checkServiceNowApproval(ServiceNowTaskParameters parameters) {
    return ServiceNowExecutionData.builder().executionStatus(ExecutionStatus.SUCCESS).build();
  }
}
