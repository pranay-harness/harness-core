/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.secretmanagerclient.dto.awssecretmanager;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.FieldNameConstants;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(PL)
@Getter
@Setter
@Builder
@FieldNameConstants(innerTypeName = "AwsSMManualCredentialConfigKeys")
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("io.harness.secretmanagerclient.dto.awssecretmanager.AwsSecretManagerManualCredential")
public class AwsSMManualCredentialConfig implements AwsSMCredentialSpecConfig {
  String accessKey;
  String secretKey;
}
