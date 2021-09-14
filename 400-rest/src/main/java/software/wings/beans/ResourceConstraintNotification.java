/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

@OwnedBy(CDC)
@EqualsAndHashCode(callSuper = true)
public class ResourceConstraintNotification extends Notification {
  @Getter @Setter private String displayText;

  public ResourceConstraintNotification() {
    super(NotificationType.INFORMATION);
  }
}
