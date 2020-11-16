package io.harness.ngtriggers.beans.scm;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WebhookBaseAttributes {
  private String message;
  private String link;
  private String before;
  private String after;
  private String ref;
  private String source;
  private String target;
  private String authorLogin;
  private String authorName;
  private String authorEmail;
  private String authorAvatar;
  private String sender;
  private String action;
}
