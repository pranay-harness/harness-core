/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.graphql.schema.type.secrets;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.QLEnum;

@TargetModule(HarnessModule._380_CG_GRAPHQL)
public enum QLSecretType implements QLEnum {
  ENCRYPTED_TEXT,
  ENCRYPTED_FILE,
  WINRM_CREDENTIAL,
  SSH_CREDENTIAL;

  @Override
  public String getStringValue() {
    return this.name();
  }
}
