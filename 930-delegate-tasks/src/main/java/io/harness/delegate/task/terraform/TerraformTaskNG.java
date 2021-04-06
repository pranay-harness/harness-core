package io.harness.delegate.task.terraform;

import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.delegate.git.NGGitService;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.delegate.task.shell.SshSessionConfigMapper;
import io.harness.delegate.task.terraform.handlers.TerraformAbstractTaskHandler;
import io.harness.exception.UnexpectedTypeException;
import io.harness.git.model.AuthRequest;
import io.harness.shell.SshSessionConfig;

import com.google.inject.Inject;
import java.util.Map;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class TerraformTaskNG extends AbstractDelegateRunnableTask {
  @Inject private Map<TFTaskType, TerraformAbstractTaskHandler> tfTaskTypeToHandlerMap;
  @Inject private SshSessionConfigMapper sshSessionConfigMapper;
  @Inject private NGGitService ngGitService;

  public TerraformTaskNG(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    return null;
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    TerraformTaskNGParameters taskParameters = (TerraformTaskNGParameters) parameters;
    CommandUnitsProgress commandUnitsProgress = CommandUnitsProgress.builder().build();

    if (!tfTaskTypeToHandlerMap.containsKey(taskParameters.getTaskType())) {
      throw new UnexpectedTypeException(
          String.format("Unexpected Terraform Task Type: [%s]", taskParameters.getTaskType()));
    }

    GitStoreDelegateConfig gitStoreDelegateConfig = taskParameters.getConfigFile().getGitStoreDelegateConfig();
    SshSessionConfig sshSessionConfig = sshSessionConfigMapper.getSSHSessionConfig(
        gitStoreDelegateConfig.getSshKeySpecDTO(), gitStoreDelegateConfig.getEncryptedDataDetails());

    AuthRequest authRequest =
        ngGitService.getAuthRequest((GitConfigDTO) gitStoreDelegateConfig.getGitConfigDTO(), sshSessionConfig);

    TerraformAbstractTaskHandler taskHandler = tfTaskTypeToHandlerMap.get(taskParameters.getTaskType());
    return taskHandler.executeTask(
        taskParameters, getLogStreamingTaskClient(), commandUnitsProgress, authRequest, getDelegateId(), getTaskId());
  }
}
