/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.secretmanagerclient;

import java.util.List;
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
@FieldNameConstants(innerTypeName = "NGEncryptedDataMetadataKeys")
public class NGEncryptedDataMetadata extends NGMetadata {
  private String accountIdentifier;
  private String orgIdentifier;
  private String projectIdentifier;
  private List<String> tags;
  private String secretManagerIdentifier;
  private String secretManagerName;
  private String description;
  private boolean draft;
}
