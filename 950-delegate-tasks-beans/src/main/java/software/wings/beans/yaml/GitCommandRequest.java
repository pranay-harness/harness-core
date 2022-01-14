package software.wings.beans.yaml;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.util.concurrent.TimeUnit;

/**
 * Created by anubhaw on 10/27/17.
 */
@OwnedBy(HarnessTeam.DX)
@TargetModule(HarnessModule._970_API_SERVICES_BEANS)
public class GitCommandRequest implements GitCommand {
  protected GitCommandType gitCommandType;
  public static final long gitRequestTimeout = TimeUnit.MINUTES.toMillis(20);

  public GitCommandRequest(GitCommandType gitCommandType) {
    this.gitCommandType = gitCommandType;
  }

  @Override
  public GitCommandType getGitCommandType() {
    return gitCommandType;
  }
}
