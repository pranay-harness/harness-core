/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.yaml.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.yaml.BaseYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@OwnedBy(CDC)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class WebhookEventYaml extends BaseYaml {
  private String eventType;
  private String action;

  @Builder
  public WebhookEventYaml(String eventType, String action) {
    this.eventType = eventType;
    this.action = action;
  }
}
