/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.delegatetasks;

import static java.lang.String.format;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.RemoteMethodReturnValueData;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.ExecutionCredential;
import software.wings.beans.HostValidationTaskParameters;
import software.wings.beans.SettingAttribute;
import software.wings.utils.HostValidationService;

import com.google.inject.Inject;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public class HostValidationTask extends AbstractDelegateRunnableTask {
  @Inject private HostValidationService hostValidationService;

  public HostValidationTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    HostValidationTaskParameters hostValidationTaskParameters = null;
    if (!(parameters[0] instanceof HostValidationTaskParameters)) {
      hostValidationTaskParameters = HostValidationTaskParameters.builder()
                                         .hostNames((List<String>) parameters[2])
                                         .connectionSetting((SettingAttribute) parameters[3])
                                         .encryptionDetails((List<EncryptedDataDetail>) parameters[4])
                                         .executionCredential((ExecutionCredential) parameters[5])
                                         .build();
    } else {
      hostValidationTaskParameters = (HostValidationTaskParameters) getParameters()[0];
    }
    return getTaskExecutionResponseData(hostValidationTaskParameters);
  }

  @Override
  public RemoteMethodReturnValueData run(TaskParameters parameters) {
    HostValidationTaskParameters hostValidationTaskParameters = null;
    if (!(parameters instanceof HostValidationTaskParameters)) {
      String message = format(
          "Unrecognized task params while running HostValidationTask: [%s]", parameters.getClass().getSimpleName());
      log.error(message);
      return RemoteMethodReturnValueData.builder().returnValue(message).build();
    }
    hostValidationTaskParameters = (HostValidationTaskParameters) parameters;
    return getTaskExecutionResponseData(hostValidationTaskParameters);
  }

  private RemoteMethodReturnValueData getTaskExecutionResponseData(
      HostValidationTaskParameters hostValidationTaskParameters) {
    Object methodReturnValue = null;
    Throwable exception = null;

    try {
      log.info("Running HostValidationTask for hosts: ", hostValidationTaskParameters.getHostNames());
      methodReturnValue = hostValidationService.validateHost(hostValidationTaskParameters.getHostNames(),
          hostValidationTaskParameters.getConnectionSetting(), hostValidationTaskParameters.getEncryptionDetails(),
          hostValidationTaskParameters.getExecutionCredential(), null);
    } catch (Exception ex) {
      exception = ex.getCause();
      String message =
          "Exception while running HostValidationTask for hosts " + hostValidationTaskParameters.getHostNames() + ex;
      log.error(message);
    }
    return RemoteMethodReturnValueData.builder().returnValue(methodReturnValue).exception(exception).build();
  }
}
