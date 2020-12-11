package io.harness.ngtriggers.beans.scm;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class Repository {
  private String name;
  private String namespace;
  private String link;
  private String branch;
  private boolean isPrivate;
  private String httpURL;
  private String sshURL;
  private String slug; // Repository name along with namespace
}
