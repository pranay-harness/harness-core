package software.wings.service.intfc.yaml;

import io.harness.annotations.dev.BreakDependencyOn;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.GitConfig;
import software.wings.beans.GitOperationContext;
import software.wings.beans.yaml.GitDiffResult;
import software.wings.beans.yaml.GitFetchFilesRequest;
import software.wings.beans.yaml.GitFetchFilesResult;
import software.wings.beans.yaml.GitFilesBetweenCommitsRequest;

/**
 * Created by anubhaw on 10/16/17.
 */

/**
 * The interface Git client.
 */
// Use git client V2 instead of this.
@OwnedBy(HarnessTeam.DX)
@TargetModule(HarnessModule._970_API_SERVICES_BEANS)
@BreakDependencyOn("software.wings.beans.GitConfig")
public interface GitClient {
  void ensureRepoLocallyClonedAndUpdated(GitOperationContext gitOperationContext);

  GitDiffResult diff(GitOperationContext gitOperationContext, boolean excludeFilesOutsideSetupFolder);

  @Deprecated String validate(GitConfig gitConfig);

  GitFetchFilesResult fetchFilesByPath(GitConfig gitConfig, GitFetchFilesRequest gitRequest);

  GitFetchFilesResult fetchFilesBetweenCommits(GitConfig gitConfig, GitFilesBetweenCommitsRequest gitRequest);

  void downloadFiles(GitConfig gitConfig, GitFetchFilesRequest gitRequest, String destinationDirectory);
}
