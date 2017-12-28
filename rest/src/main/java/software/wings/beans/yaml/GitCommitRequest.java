package software.wings.beans.yaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by anubhaw on 10/16/17.
 */

@Data
@EqualsAndHashCode(callSuper = false)
@Builder
public class GitCommitRequest extends GitCommandRequest {
  private List<GitFileChange> gitFileChanges = new ArrayList<>();

  public GitCommitRequest() {
    super(GitCommandType.COMMIT);
  }

  public GitCommitRequest(List<GitFileChange> gitFileChanges) {
    super(GitCommandType.COMMIT);
    this.gitFileChanges = gitFileChanges;
  }
}
