/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.changestreamsframework;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.PersistentEntity;

import lombok.AllArgsConstructor;
import lombok.Value;

@Value
@AllArgsConstructor
@OwnedBy(HarnessTeam.CE)
public class ChangeTrackingInfo<T extends PersistentEntity> {
  private Class<T> morphiaClass;
  private ChangeSubscriber<T> changeSubscriber;
  private String resumeToken;
}
