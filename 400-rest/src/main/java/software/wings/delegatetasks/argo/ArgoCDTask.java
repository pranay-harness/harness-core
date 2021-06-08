package software.wings.delegatetasks.argo;

import io.harness.argo.beans.AppSyncOptions;
import io.harness.argo.beans.ArgoApp;
import io.harness.argo.beans.ArgoConfigInternal;
import io.harness.argo.beans.ClusterResourceTreeDTO;
import io.harness.argo.beans.ManifestDiff;
import io.harness.argo.service.ArgoCdService;
import io.harness.delegate.beans.DelegateResponseData;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.beans.argo.response.ArgoSyncResponse;
import io.harness.delegate.beans.argo.response.ManifestDiffResponse;
import io.harness.delegate.beans.argo.response.ResourceTreeResponse;
import io.harness.delegate.beans.logstreaming.ILogStreamingTaskClient;
import io.harness.delegate.task.AbstractDelegateRunnableTask;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.CommandExecutionStatus;

import software.wings.beans.settings.argo.ArgoConfig;
import software.wings.delegatetasks.argo.beans.request.ArgoAppSyncRequest;
import software.wings.delegatetasks.argo.beans.request.ArgoRequest;
import software.wings.service.intfc.security.EncryptionService;

import com.google.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class ArgoCDTask extends AbstractDelegateRunnableTask {
  @Inject private ArgoCdService argoCdService;
  @Inject EncryptionService encryptionService;

  public ArgoCDTask(DelegateTaskPackage delegateTaskPackage, ILogStreamingTaskClient logStreamingTaskClient,
      Consumer<DelegateTaskResponse> consumer, BooleanSupplier preExecute) {
    super(delegateTaskPackage, logStreamingTaskClient, consumer, preExecute);
  }

  @Override
  public DelegateResponseData run(Object[] parameters) {
    throw new UnsupportedOperationException("not supported");
  }

  @Override
  public DelegateResponseData run(TaskParameters parameters) {
    ArgoRequest request = (ArgoRequest) parameters;
    encryptionService.decrypt(request.getArgoConfig(), request.getEncryptedDataDetails(), false);
    final ArgoConfigInternal argoConfigInternal = buildArgoConfigInternal(request.getArgoConfig());
    switch (request.requestType()) {
      case RESOURCE_TREE:
        final ClusterResourceTreeDTO clusterResourceTreeDTO;
        try {
          clusterResourceTreeDTO = argoCdService.fetchResourceTree(argoConfigInternal, request.getAppName());
          return ResourceTreeResponse.builder()
              .executionStatus(CommandExecutionStatus.SUCCESS)
              .clusterResourceTree(clusterResourceTreeDTO)
              .build();
        } catch (IOException e) {
          return ResourceTreeResponse.builder()
              .executionStatus(CommandExecutionStatus.FAILURE)
              .errorMessage(e.getMessage())
              .build();
        }
      case APP_SYNC:
        try {
          ArgoApp argoApp =
              argoCdService.syncApp(argoConfigInternal, request.getAppName(), AppSyncOptions.DefaultSyncOptions());
          return ArgoSyncResponse.builder().executionStatus(CommandExecutionStatus.SUCCESS).argoApp(argoApp).build();
        } catch (Exception e) {
          return ArgoSyncResponse.builder()
              .executionStatus(CommandExecutionStatus.FAILURE)
              .errorMessage(e.getMessage())
              .build();
        }
      case MANIFEST_DIFF:
        try {
          List<ManifestDiff> manifestDiffs = argoCdService.fetchManifestDiff(argoConfigInternal, request.getAppName());
          return ManifestDiffResponse.builder()
              .manifestDiffList(manifestDiffs)
              .executionStatus(CommandExecutionStatus.SUCCESS)
              .build();
        } catch (IOException e) {
          return ManifestDiffResponse.builder()
              .executionStatus(CommandExecutionStatus.FAILURE)
              .errorMessage(e.getMessage())
              .build();
        }
      default:
        throw new InvalidRequestException("Unhandled Argo TaskType");
    }
  }

  private ArgoConfigInternal buildArgoConfigInternal(ArgoConfig argoConfig) {
    return ArgoConfigInternal.builder()
        .argoServerUrl(argoConfig.getArgoServerUrl())
        .username(argoConfig.getUsername())
        .password(String.valueOf(argoConfig.getPassword()))
        .build();
  }
}
