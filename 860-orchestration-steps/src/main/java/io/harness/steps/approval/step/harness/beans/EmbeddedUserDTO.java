/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.steps.approval.step.harness.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(HarnessTeam.CDC)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmbeddedUserDTO {
  private String name;
  private String email;

  public static EmbeddedUserDTO fromEmbeddedUser(EmbeddedUser user) {
    if (user == null) {
      return null;
    }
    return EmbeddedUserDTO.builder().name(user.getName()).email(user.getEmail()).build();
  }
}
