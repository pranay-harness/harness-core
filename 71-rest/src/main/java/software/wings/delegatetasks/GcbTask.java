package software.wings.delegatetasks;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.threading.Morpheus.sleep;
import static software.wings.sm.states.GcbState.GcbDelegateResponse.gcbDelegateResponseOf;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.TaskParameters;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.command.GcbTaskParams;
import software.wings.helpers.ext.gcb.GcbService;
import software.wings.helpers.ext.gcb.models.GcbBuildDetails;
import software.wings.helpers.ext.gcb.models.RepoSource;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.sm.states.GcbState;
import software.wings.sm.states.GcbState.GcbDelegateResponse;

import java.time.Duration;
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
        throw new IllegalArgumentException("");
    }
  }

  protected GcbDelegateResponse startGcbBuild(final @NotNull GcbTaskParams params) {
    GcbBuildDetails buildDetails = triggerGcb(params);
    params.setBuildId(buildDetails.getId());
    return gcbDelegateResponseOf(params, buildDetails);
  }

  protected GcbDelegateResponse pollGcbBuild(final @NotNull GcbTaskParams params) {
    GcbBuildDetails build;
    do {
      sleep(Duration.ofSeconds(5)); // make this adjustable
      build = gcbService.getBuild(
          params.getGcpConfig(), params.getEncryptedDataDetails(), params.getProjectId(), params.getBuildId());
    } while (build.isWorking());
    return gcbDelegateResponseOf(params, build);
  }

  protected GcbBuildDetails triggerGcb(final @NotNull GcbTaskParams params) {
    RepoSource repoSource = new RepoSource();
    repoSource.setBranchName(params.getBranchName());
    return gcbService
        .runTrigger(params.getGcpConfig(), params.getEncryptedDataDetails(), params.getProjectId(),
            params.getTriggerId(), repoSource)
        .getOperationMeta()
        .getBuild();
  }
}
