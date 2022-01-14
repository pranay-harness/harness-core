package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(DX)
public class Repository {
  private String id;
  private String name;
  private String namespace;
  private String link;
  private String branch;
  private boolean isPrivate;
  private String httpURL;
  private String sshURL;
  private String slug; // Repository name along with namespace
}
