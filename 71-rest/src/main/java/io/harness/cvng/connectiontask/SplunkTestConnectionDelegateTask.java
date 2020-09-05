package io.harness.cvng.connectiontask;

import com.google.inject.Inject;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectionTaskParams;
import io.harness.delegate.beans.connector.splunkconnector.SplunkConnectionTaskResponse;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.service.intfc.splunk.SplunkDelegateService;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Slf4j
public class SplunkTestConnectionDelegateTask extends AbstractDelegateRunnableTask {
  @Inject private SplunkDelegateService splunkDelegateService;
  public SplunkTestConnectionDelegateTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    SplunkConnectionTaskParams taskParameters = (SplunkConnectionTaskParams) parameters;
    boolean validCredentails = false;
    Exception execptionInProcessing = null;
    try {
      validCredentails = splunkDelegateService.validateConfig(
          taskParameters.getSplunkConnectorDTO(), taskParameters.getEncryptionDetails());
    } catch (Exception ex) {
      logger.info("Exception while validating appdynamics credentials", ex);
      execptionInProcessing = ex;
    }

    return SplunkConnectionTaskResponse.builder()
        .valid(validCredentails)
        .errorMessage(execptionInProcessing != null ? execptionInProcessing.getMessage() : "")
        .build();
  }
}
