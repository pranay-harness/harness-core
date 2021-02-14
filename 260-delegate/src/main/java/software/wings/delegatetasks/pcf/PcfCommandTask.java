package software.wings.delegatetasks.pcf;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.InvalidArgumentsException;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.helpers.ext.pcf.request.PcfCommandRequest;
import software.wings.helpers.ext.pcf.request.PcfCommandTaskParameters;
import software.wings.helpers.ext.pcf.request.PcfInstanceSyncRequest;
import software.wings.helpers.ext.pcf.request.PcfRunPluginCommandRequest;
import software.wings.helpers.ext.pcf.response.PcfCommandExecutionResponse;

import com.google.inject.Inject;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
@Slf4j
@TargetModule(Module._930_DELEGATE_TASKS)
public class PcfCommandTask extends AbstractDelegateRunnableTask {
  @Inject PcfDelegateTaskHelper pcfDelegateTaskHelper;

  public PcfCommandTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public PcfCommandExecutionResponse run(TaskParameters parameters) {
    if (!(parameters instanceof PcfRunPluginCommandRequest)) {
      throw new InvalidArgumentsException(Pair.of("pcfCommandRequest", "Must be instance of PcfPluginCommandRequest"));
    }
    final PcfRunPluginCommandRequest pluginCommandRequest = (PcfRunPluginCommandRequest) parameters;
    return getPcfCommandExecutionResponse(pluginCommandRequest, pluginCommandRequest.getEncryptedDataDetails());
  }

  @Override
  public PcfCommandExecutionResponse run(Object[] parameters) {
    final PcfCommandRequest pcfCommandRequest;
    final List<EncryptedDataDetail> encryptedDataDetails;
    if (parameters[0] instanceof PcfCommandTaskParameters) {
      PcfCommandTaskParameters pcfCommandTaskParameters = (PcfCommandTaskParameters) parameters[0];
      pcfCommandRequest = pcfCommandTaskParameters.getPcfCommandRequest();
      encryptedDataDetails = pcfCommandTaskParameters.getEncryptedDataDetails();
    } else {
      pcfCommandRequest = (PcfCommandRequest) parameters[0];
      encryptedDataDetails = (List<EncryptedDataDetail>) parameters[1];
    }
    return getPcfCommandExecutionResponse(pcfCommandRequest, encryptedDataDetails);
  }

  private PcfCommandExecutionResponse getPcfCommandExecutionResponse(
      PcfCommandRequest pcfCommandRequest, List<EncryptedDataDetail> encryptedDataDetails) {
    boolean isInstanceSync = pcfCommandRequest instanceof PcfInstanceSyncRequest;
    return pcfDelegateTaskHelper.getPcfCommandExecutionResponse(
        pcfCommandRequest, encryptedDataDetails, isInstanceSync);
  }
}
