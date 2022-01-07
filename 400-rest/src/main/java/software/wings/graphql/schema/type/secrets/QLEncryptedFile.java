/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.schema.type.secrets;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLEncryptedFileKeys")
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLEncryptedFile implements QLSecret {
  private String secretManagerId;
  private String name;
  private QLSecretType secretType;
  private String id;
  private QLUsageScope usageScope;
  private boolean scopedToAccount;
  private boolean inheritScopesFromSM;
}
