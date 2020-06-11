package software.wings.delegatetasks;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.google.inject.Inject;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateTaskResponse;
import io.harness.delegate.task.TaskParameters;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.NotImplementedException;
import software.wings.beans.DelegateTaskPackage;
import software.wings.beans.GcpConfig;
import software.wings.beans.command.GcbTaskParams;
import software.wings.helpers.ext.gcb.GcbService;
import software.wings.helpers.ext.gcb.models.BuildOperationDetails;
import software.wings.helpers.ext.gcb.models.RepoSource;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.sm.states.GcbState;
import software.wings.sm.states.GcbState.GcbDelegateResponse;

import java.util.List;
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
    GcpConfig config = params.getGcpConfig();
    List<EncryptedDataDetail> encryptedDataDetails = params.getEncryptedDataDetails();
    RepoSource repoSource = new RepoSource();
    repoSource.setBranchName(params.getBranchName());
    BuildOperationDetails build =
        gcbService.runTrigger(config, encryptedDataDetails, params.getProjectId(), params.getTriggerId(), repoSource);
    return GcbDelegateResponse.builder()
        .activityId(params.getActivityId())
        .build(build.getOperationMeta().getBuild())
        .build();
  }
}
