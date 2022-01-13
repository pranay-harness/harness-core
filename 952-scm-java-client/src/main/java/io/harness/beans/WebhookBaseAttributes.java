/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(DX)
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
