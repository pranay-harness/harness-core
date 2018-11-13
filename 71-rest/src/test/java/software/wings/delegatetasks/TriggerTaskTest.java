package software.wings.delegatetasks;

import static java.util.Arrays.asList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.DelegateTask.Builder.aDelegateTask;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.GitConfig;
import software.wings.beans.TaskType;
import software.wings.beans.command.CommandExecutionResult.CommandExecutionStatus;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.beans.yaml.GitFile;
import software.wings.helpers.ext.trigger.request.TriggerDeploymentNeededRequest;
import software.wings.helpers.ext.trigger.response.TriggerDeploymentNeededResponse;
import software.wings.helpers.ext.trigger.response.TriggerResponse;
import software.wings.service.intfc.GitService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.WingsTestConstants;

import java.util.ArrayList;
import java.util.List;

public class TriggerTaskTest extends WingsBaseTest {
  private static final String CURRENT_COMMIT_ID = "currentCommitId";
  private static final String OLD_COMMIT_ID = "oldCommitId";

  @Mock private EncryptionService encryptionService;
  @Mock private GitService gitService;

  @InjectMocks
  private TriggerTask triggerTask = (TriggerTask) TaskType.TRIGGER_TASK.getDelegateRunnableTask(
      "delegateId", aDelegateTask().build(), notifyResponseData -> {}, () -> true);

  @Before
  public void setUp() throws Exception {}

  @Test
  public void testDeploymentNeeded() {
    TriggerDeploymentNeededRequest triggerRequest = getTriggerDeploymentNeededRequest();
    triggerTask.run(new Object[] {triggerRequest});

    verify(gitService)
        .fetchFilesBetweenCommits(triggerRequest.getGitConfig(), triggerRequest.getCurrentCommitId(),
            triggerRequest.getOldCommitId(), triggerRequest.getGitConnectorId());
  }

  @Test
  public void testDeploymentNeededTrueCase() {
    TriggerDeploymentNeededRequest triggerRequest = getTriggerDeploymentNeededRequest();
    triggerRequest.setFilePaths(asList("abc/ghi.txt"));

    List<GitFile> gitFileList = new ArrayList<>();
    gitFileList.add(GitFile.builder().filePath("abc/def.txt").build());
    gitFileList.add(GitFile.builder().filePath("abc/ghi.txt").build());
    GitFetchFilesResult gitFetchFilesResult = GitFetchFilesResult.builder().files(gitFileList).build();

    when(gitService.fetchFilesBetweenCommits(any(), any(), any(), any())).thenReturn(gitFetchFilesResult);

    TriggerDeploymentNeededResponse triggerResponse =
        (TriggerDeploymentNeededResponse) triggerTask.run(new Object[] {triggerRequest});
    assertThat(triggerResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(triggerResponse.isDeploymentNeeded()).isEqualTo(true);
  }

  @Test
  public void testDeploymentNeededFalseCase() {
    TriggerDeploymentNeededRequest triggerRequest = getTriggerDeploymentNeededRequest();
    triggerRequest.setFilePaths(asList("abc/xyz.txt"));

    List<GitFile> gitFileList = new ArrayList<>();
    gitFileList.add(GitFile.builder().filePath("abc/def.txt").build());
    gitFileList.add(GitFile.builder().filePath("abc/ghi.txt").build());
    GitFetchFilesResult gitFetchFilesResult = GitFetchFilesResult.builder().files(gitFileList).build();

    when(gitService.fetchFilesBetweenCommits(any(), any(), any(), any())).thenReturn(gitFetchFilesResult);

    TriggerDeploymentNeededResponse triggerResponse =
        (TriggerDeploymentNeededResponse) triggerTask.run(new Object[] {triggerRequest});
    assertThat(triggerResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(triggerResponse.isDeploymentNeeded()).isEqualTo(false);
  }

  @Test
  public void testDeploymentNeededException() {
    TriggerDeploymentNeededRequest triggerRequest = getTriggerDeploymentNeededRequest();
    when(gitService.fetchFilesBetweenCommits(any(), any(), any(), any()))
        .thenThrow(new WingsException(ErrorCode.YAML_GIT_SYNC_ERROR).addParam("message", ""));

    TriggerResponse triggerResponse = triggerTask.run(new Object[] {triggerRequest});
    assertThat(triggerResponse.getCommandExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
  }

  private TriggerDeploymentNeededRequest getTriggerDeploymentNeededRequest() {
    return TriggerDeploymentNeededRequest.builder()
        .accountId(WingsTestConstants.ACCOUNT_ID)
        .gitConfig(GitConfig.builder().build())
        .gitConnectorId(WingsTestConstants.SETTING_ID)
        .currentCommitId(CURRENT_COMMIT_ID)
        .oldCommitId(OLD_COMMIT_ID)
        .build();
  }
}