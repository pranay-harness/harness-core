package io.harness.delegate.task.aws;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.scm.genericgitconnector.GitConfigDTO;
import io.harness.delegate.beans.storeconfig.GitStoreDelegateConfig;
import io.harness.git.model.GitBaseRequest;
import io.harness.logging.LogCallback;

@OwnedBy(CDP)
public interface AwsBaseHelper {
    GitBaseRequest getGitBaseRequestForConfigFile(
            String accountId, GitStoreDelegateConfig confileFileGitStore, GitConfigDTO configFileGitConfigDTO);

    String fetchAwsSamAppDirectory(GitBaseRequest gitBaseRequestForConfigFile, String accountId,
                                   String workspace, String currentStateFileId, GitStoreDelegateConfig confileFileGitStore, LogCallback logCallback,
                                   String scriptPath, String workingDir);

    String resolveBaseDir(String accountId);
}
