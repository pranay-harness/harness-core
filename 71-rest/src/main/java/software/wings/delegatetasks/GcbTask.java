package software.wings.delegatetasks;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.threading.Morpheus.sleep;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Log.LogLevel.INFO;
import static software.wings.sm.states.GcbState.GcbDelegateResponse.gcbDelegateResponseOf;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.UnsupportedOperationException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.Log;
import software.wings.beans.command.GcbTaskParams;
import software.wings.helpers.ext.gcb.GcbService;
import software.wings.helpers.ext.gcb.models.BuildOperationDetails;
import software.wings.helpers.ext.gcb.models.GcbBuildDetails;
import software.wings.helpers.ext.gcb.models.RepoSource;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.sm.states.GcbState;
import software.wings.sm.states.GcbState.GcbDelegateResponse;
import software.wings.sm.states.gcbconfigs.GcbTriggerBuildSpec;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;

/**
 * Created by rishi on 12/14/16.
 */
@OwnedBy(CDC)
@Slf4j
public class GcbTask extends AbstractDelegateRunnableTask {
  @Inject private EncryptionService encryptionService;
  @Inject private DelegateLogService logService;
  @Inject private GcbService gcbService;
  private final AtomicInteger alreadyLogged = new AtomicInteger(0); // move to taskParams

  public GcbTask(
      DelegateTaskPackage delegateTaskPackage, Consumer<DelegateTaskResponse> postExecute, BooleanSupplier preExecute) {
    super(delegateTaskPackage, postExecute, preExecute);
  }

  @Override
  public GcbDelegateResponse run(TaskParameters parameters) {
    throw new NotImplementedException("not implemented");
  }

  @Override
  public GcbState.GcbDelegateResponse run(Object[] parameters) {
    return run((GcbTaskParams) parameters[0]);
  }

  public GcbDelegateResponse run(GcbTaskParams params) {
    switch (params.getType()) {
      case START:
        return startGcbBuild(params);
      case POLL:
        return pollGcbBuild(params);
      default:
        throw new UnsupportedOperationException(format("Unsupported TaskType: %s", params.getType()));
    }
  }

  protected GcbDelegateResponse startGcbBuild(final @NotNull GcbTaskParams params) {
    BuildOperationDetails buildOperationDetails = triggerGcb(params);
    GcbBuildDetails buildDetails = buildOperationDetails.getOperationMeta().getBuild();
    params.setBuildId(buildDetails.getId());
    params.setBuildName(buildOperationDetails.getName());
    return gcbDelegateResponseOf(params, buildDetails);
  }

  protected GcbDelegateResponse pollGcbBuild(final @NotNull GcbTaskParams params) {
    GcbBuildDetails build;
    do {
      sleep(Duration.ofSeconds(params.getPollFrequency()));
      build = gcbService.getBuild(params.getGcpConfig(), params.getEncryptedDataDetails(),
          params.getGcbOptions().getProjectId(), params.getBuildId());
      String gcbOutput = gcbService.fetchBuildLogs(
          params.getGcpConfig(), params.getEncryptedDataDetails(), build.getLogsBucket(), params.getBuildId());

      saveConsoleLogs(alreadyLogged, params.getActivityId(), params.getUnitName(),
          build.getStatus().getCommandExecutionStatus(), params.getAppId(), gcbOutput);
    } while (build.isWorking());
    return gcbDelegateResponseOf(params, build);
  }

  protected BuildOperationDetails triggerGcb(final @NotNull GcbTaskParams params) {
    RepoSource repoSource = new RepoSource();
    GcbTriggerBuildSpec.GcbTriggerSource source = params.getGcbOptions().getTriggerSpec().getSource();
    String sourceId = params.getGcbOptions().getTriggerSpec().getSourceId();
    switch (source) {
      case TAG:
        repoSource.setTagName(sourceId);
        break;
      case BRANCH:
        repoSource.setBranchName(sourceId);
        break;
      case COMMIT:
        repoSource.setCommitSha(sourceId);
        break;
      default:
        break;
    }
    return gcbService.runTrigger(params.getGcpConfig(), params.getEncryptedDataDetails(),
        params.getGcbOptions().getProjectId(), params.getGcbOptions().getTriggerSpec().getName(), repoSource);
  }

  // similar to JenkinsTask#sameConsoleLogs(activityId, stateName, commandExecutionStatus, appId, consoleOutput)
  void saveConsoleLogs(AtomicInteger logsCount, String activityId, String stateName,
      CommandExecutionStatus commandExecutionStatus, String appId, String consoleOutput) {
    if (isNotBlank(consoleOutput)) {
      String[] consoleLines = consoleOutput.split("\r?\n");
      int offset = alreadyLogged.get();
      for (int i = offset; i < consoleLines.length; i++) {
        Log log = aLog()
                      .withActivityId(activityId)
                      .withCommandUnitName(stateName)
                      .withAppId(appId)
                      .withLogLevel(INFO)
                      .withLogLine(consoleLines[i])
                      .withExecutionResult(commandExecutionStatus)
                      .build();
        logService.save(getAccountId(), log);
        logsCount.incrementAndGet();
      }
    }
  }
}
