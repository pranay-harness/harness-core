/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.beans;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import java.util.Set;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@OwnedBy(PL)
@RequiredArgsConstructor
@Value
@Getter
public class SecretParentsUpdateDetail {
  @NonNull String secretId;
  @NonNull Set<EncryptedDataParent> parentsToAdd;
  @NonNull Set<EncryptedDataParent> parentsToRemove;
}
