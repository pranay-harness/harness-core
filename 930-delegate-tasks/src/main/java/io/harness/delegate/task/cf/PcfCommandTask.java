package io.harness.delegate.task.cf;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.pcf.CfCommandRequest;
import io.harness.delegate.task.pcf.request.CfCommandTaskParameters;
import io.harness.delegate.task.pcf.request.CfInstanceSyncRequest;
import io.harness.delegate.task.pcf.request.CfRunPluginCommandRequest;
import io.harness.delegate.task.pcf.response.CfCommandExecutionResponse;
import io.harness.security.encryption.EncryptedDataDetail;

import com.google.inject.Inject;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;

@Slf4j
@OwnedBy(HarnessTeam.CDP)
public class PcfCommandTask extends AbstractDelegateRunnableTask {
  @Inject PcfDelegateTaskHelper pcfDelegateTaskHelper;

  public PcfCommandTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public CfCommandExecutionResponse run(TaskParameters parameters) {
    CfCommandTaskParameters cfCommandTaskParameters = (CfCommandTaskParameters) parameters;
    final CfCommandRequest cfCommandRequest = cfCommandTaskParameters.getPcfCommandRequest();
    final List<EncryptedDataDetail> encryptedDataDetails;
    if (cfCommandRequest instanceof CfRunPluginCommandRequest) {
      encryptedDataDetails = ((CfRunPluginCommandRequest) cfCommandRequest).getEncryptedDataDetails();
    } else {
      encryptedDataDetails = cfCommandTaskParameters.getEncryptedDataDetails();
    }
    return getPcfCommandExecutionResponse(cfCommandRequest, encryptedDataDetails);
  }

  @Override
  public CfCommandExecutionResponse run(Object[] parameters) {
    throw new NotImplementedException("not implemented");
  }

  private CfCommandExecutionResponse getPcfCommandExecutionResponse(
      CfCommandRequest cfCommandRequest, List<EncryptedDataDetail> encryptedDataDetails) {
    boolean isInstanceSync = cfCommandRequest instanceof CfInstanceSyncRequest;
    return pcfDelegateTaskHelper.getPcfCommandExecutionResponse(
        cfCommandRequest, encryptedDataDetails, isInstanceSync, getLogStreamingTaskClient());
  }
}
