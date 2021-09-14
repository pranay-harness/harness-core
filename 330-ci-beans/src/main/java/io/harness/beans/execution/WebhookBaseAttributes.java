/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.beans.execution;

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
