/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.execution.export.metadata;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;

import lombok.Builder;
import lombok.Value;

@OwnedBy(CDC)
@Value
@Builder
public class EmbeddedUserMetadata {
  String name;
  String email;

  static EmbeddedUserMetadata fromEmbeddedUser(EmbeddedUser embeddedUser) {
    if (embeddedUser == null || (embeddedUser.getName() == null && embeddedUser.getEmail() == null)) {
      return null;
    }

    return EmbeddedUserMetadata.builder().name(embeddedUser.getName()).email(embeddedUser.getEmail()).build();
  }
}
