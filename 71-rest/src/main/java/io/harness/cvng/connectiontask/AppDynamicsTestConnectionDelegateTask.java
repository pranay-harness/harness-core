package io.harness.cvng.connectiontask;

import com.google.inject.Inject;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectionTaskParams;
import io.harness.delegate.beans.connector.appdynamicsconnector.AppDynamicsConnectionTaskResponse;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.service.intfc.appdynamics.AppdynamicsDelegateService;

import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

@Slf4j
public class AppDynamicsTestConnectionDelegateTask extends AbstractDelegateRunnableTask {
  @Inject private AppdynamicsDelegateService appdynamicsDelegateService;

  public AppDynamicsTestConnectionDelegateTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    AppDynamicsConnectionTaskParams taskParameters = (AppDynamicsConnectionTaskParams) parameters;
    boolean validCredentails = false;
    Exception execptionInProcessing = null;
    try {
      validCredentails = appdynamicsDelegateService.validateConfig(
          taskParameters.getAppDynamicsConnectorDTO(), taskParameters.getEncryptionDetails());
    } catch (Exception ex) {
      logger.info("Exception while validating appdynamics credentials", ex);
      execptionInProcessing = ex;
    }

    return AppDynamicsConnectionTaskResponse.builder()
        .valid(validCredentails)
        .errorMessage(execptionInProcessing != null ? execptionInProcessing.getMessage() : "")
        .build();
  }
}
