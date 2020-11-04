package io.harness.beans.execution;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WebhookGitUser {
  private String gitId;
  private String name;
  private String email;
  private String avatar;
}
