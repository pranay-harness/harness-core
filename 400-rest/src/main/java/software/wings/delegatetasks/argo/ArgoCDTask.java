package software.wings.delegatetasks.argo;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.logging.CommandExecutionStatus.RUNNING;
import static io.harness.logging.CommandExecutionStatus.SUCCESS;
import static io.harness.logging.LogLevel.INFO;

import io.harness.argo.ArgoCommandUnitConstants;
import io.harness.argo.beans.AppStatus;
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

import software.wings.beans.LogColor;
import software.wings.beans.LogHelper;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.beans.settings.argo.ArgoConfig;
import software.wings.delegatetasks.DelegateLogService;
import software.wings.delegatetasks.argo.beans.request.ArgoRequest;
import software.wings.service.intfc.security.EncryptionService;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.inject.Inject;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

public class ArgoCDTask extends AbstractDelegateRunnableTask {
  @Inject private ArgoCdService argoCdService;
  @Inject private DelegateLogService logService;
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
    boolean enableLogging = isNotEmpty(request.getActivityId());
    logInit(enableLogging, request);
    switch (request.requestType()) {
      case RESOURCE_TREE:
        final ClusterResourceTreeDTO clusterResourceTreeDTO;
        try {
          clusterResourceTreeDTO = argoCdService.fetchResourceTree(argoConfigInternal, request.getAppName());
          AppStatus appStatus = argoCdService.fetchApplicationStatus(argoConfigInternal, request.getAppName());
          clusterResourceTreeDTO.setAppStatus(appStatus);
          return ResourceTreeResponse.builder()
              .executionStatus(CommandExecutionStatus.SUCCESS)
              .clusterResourceTree(clusterResourceTreeDTO)
              .build();
        } catch (Exception e) {
          return ResourceTreeResponse.builder()
              .executionStatus(CommandExecutionStatus.FAILURE)
              .errorMessage(e.getMessage())
              .build();
        }
      case APP_SYNC:
        try {
          final ArgoApp argoApp =
              argoCdService.syncApp(argoConfigInternal, request.getAppName(), AppSyncOptions.DefaultSyncOptions());
          if (enableLogging) {
            logArgoApp(request, argoApp);
          }
          return ArgoSyncResponse.builder().argoApp(argoApp).executionStatus(CommandExecutionStatus.SUCCESS).build();
        } catch (IOException e) {
          return ArgoSyncResponse.builder()
              .executionStatus(CommandExecutionStatus.FAILURE)
              .errorMessage(e.getMessage())
              .build();
        }
      case MANIFEST_DIFF:
        try {
          List<ManifestDiff> manifestDiffs = argoCdService.fetchManifestDiff(argoConfigInternal, request.getAppName());
          if (enableLogging) {
            logManifestDiff(request, manifestDiffs);
          }
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

  private void logInit(boolean enableLogging, ArgoRequest request) {
    if (enableLogging) {
      final ExecutionLogCallback logCallback = getLogCallback(request, ArgoCommandUnitConstants.INIT);
      logCallback.saveExecutionLog("Starting Argo Command Task", INFO, RUNNING);
      logCallback.saveExecutionLog("Application Name:" + request.getAppName(), INFO, RUNNING);
      logCallback.saveExecutionLog("Argo ServerURL:" + request.getArgoConfig().getArgoServerUrl(), INFO, SUCCESS);
    }
  }

  private void logArgoApp(ArgoRequest request, ArgoApp argoApp) {
    final ExecutionLogCallback logCallback = getLogCallback(request, ArgoCommandUnitConstants.ARGO_SYNC_COMMAND);

    logCallback.saveExecutionLog("Synced Argo Application:" + request.getAppName(), INFO, RUNNING);

    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    JsonParser jp = new JsonParser();
    JsonElement je = jp.parse(new Gson().toJson(argoApp));
    String prettyJsonString = gson.toJson(je);

    logCallback.saveExecutionLog("Application Spec:\n" + prettyJsonString, INFO, SUCCESS);
  }

  private void logManifestDiff(ArgoRequest request, List<ManifestDiff> manifestDiffs) {
    final ExecutionLogCallback logCallback = getLogCallback(request, ArgoCommandUnitConstants.ARGO_DRIFT_COMMAND);
    AtomicBoolean diffFound = new AtomicBoolean(false);
    manifestDiffs.forEach(manifestDiff -> {
      if (manifestDiff.getClusterManifest().equalsIgnoreCase(manifestDiff.getGitManifest())) {
        logCallback.saveExecutionLog(
            LogHelper.color("No Diff for Resource:" + manifestDiff.getResourceIdentifier(), LogColor.Yellow), INFO,
            RUNNING);
      } else {
        diffFound.set(true);
        logCallback.saveExecutionLog(
            LogHelper.color("Diff Found for Resource:" + manifestDiff.getResourceIdentifier(), LogColor.Green), INFO,
            RUNNING);
      }
    });
    logCallback.saveExecutionLog(
        "Drift Calculation Done: " + (diffFound.get() ? "Drift Found" : "No Drift Found"), INFO, SUCCESS);
  }

  private ExecutionLogCallback getLogCallback(ArgoRequest request, String commandUnitName) {
    return new ExecutionLogCallback(logService, request.getArgoConfig().getAccountId(), request.getAppId(),
        request.getActivityId(), commandUnitName);
  }

  private ArgoConfigInternal buildArgoConfigInternal(ArgoConfig argoConfig) {
    return ArgoConfigInternal.builder()
        .argoServerUrl(argoConfig.getArgoServerUrl())
        .username(argoConfig.getUsername())
        .password(String.valueOf(argoConfig.getPassword()))
        .build();
  }
}
