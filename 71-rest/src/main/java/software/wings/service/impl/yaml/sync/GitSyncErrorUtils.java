package software.wings.service.impl.yaml.sync;

import static software.wings.yaml.errorhandling.GitSyncError.GitSyncDirection.GIT_TO_HARNESS;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import software.wings.yaml.errorhandling.GitSyncError;
import software.wings.yaml.errorhandling.GitToHarnessErrorDetails;

@UtilityClass
@Slf4j
public class GitSyncErrorUtils {
  public static final String EMPTY_STR = "";
  public static final Long DEFAULT_COMMIT_TIME = 0L;

  public static boolean isGitToHarnessSyncError(GitSyncError gitSyncError) {
    return GIT_TO_HARNESS.name().equals(gitSyncError.getGitSyncDirection());
  }

  public static String getCommitIdOfError(GitSyncError error) {
    if (isGitToHarnessSyncError(error)) {
      GitToHarnessErrorDetails gitToHarnessErrorDetails = (GitToHarnessErrorDetails) error.getAdditionalErrorDetails();
      return gitToHarnessErrorDetails.getGitCommitId();
    }
    logger.warn("The commitId is specific to the git to harness error, it should not be called for harness to git");
    return EMPTY_STR;
  }

  public static String getYamlContentOfError(GitSyncError error) {
    if (isGitToHarnessSyncError(error)) {
      GitToHarnessErrorDetails gitToHarnessErrorDetails = (GitToHarnessErrorDetails) error.getAdditionalErrorDetails();
      return gitToHarnessErrorDetails.getYamlContent();
    }
    logger.warn("The yaml content is specific to the git to harness error, it should not be called for harness to git");
    return EMPTY_STR;
  }

  public static Long getCommitTimeOfError(GitSyncError error) {
    if (isGitToHarnessSyncError(error)) {
      GitToHarnessErrorDetails gitToHarnessErrorDetails = (GitToHarnessErrorDetails) error.getAdditionalErrorDetails();
      return gitToHarnessErrorDetails.getCommitTime();
    }

    logger.warn("The commitTime is specific to the git to harness error, it should not be called for harness to git");
    return DEFAULT_COMMIT_TIME;
  }
}
