/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.secretmanagerclient;

import io.harness.ng.core.common.beans.NGTag;

import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@SuperBuilder
@FieldNameConstants(innerTypeName = "NGSecretManagerMetadataKeys")
public class NGSecretManagerMetadata extends NGMetadata {
  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private List<NGTag> tags;
  private String description;
  private Boolean harnessManaged;
  @Builder.Default private Boolean deleted = Boolean.FALSE;

  public boolean getHarnessManaged() {
    return Boolean.TRUE.equals(harnessManaged);
  }

  public boolean getDeleted() {
    return Boolean.TRUE.equals(deleted);
  }
}
