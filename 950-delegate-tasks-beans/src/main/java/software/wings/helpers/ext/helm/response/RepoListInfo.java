package software.wings.helpers.ext.helm.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RepoListInfo {
  private String repoName;
  private String repoUrl;
}
