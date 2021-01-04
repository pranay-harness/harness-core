package software.wings.beans;

import lombok.Builder;
import lombok.Data;

/**
 * This repository info contains fields for UI purpose to show complete url of git repository, displayUrl which is short
 * url of repository and repository provider
 */
@Data
@Builder
public class GitRepositoryInfo {
  private String url;
  private String displayUrl;
  private GitProvider provider;

  public enum GitProvider { GITHUB, BITBUCKET, GITLAB, UNKNOWN }
}
