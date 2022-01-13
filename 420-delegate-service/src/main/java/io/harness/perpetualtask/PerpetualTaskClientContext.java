/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.perpetualtask;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import java.util.Map;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "PerpetualTaskClientContextKeys")
@OwnedBy(HarnessTeam.DEL)
@TargetModule(HarnessModule._920_DELEGATE_SERVICE_BEANS)
public class PerpetualTaskClientContext {
  // Unique key (provided by caller) that references the perpetual task in question
  private String clientId;

  // This is a set of arbitrary client parameters that will allow for the task to be identified from the
  // client that requested it and will provide the necessary task parameters.
  private Map<String, String> clientParams;

  // Alternatively the caller might provide the task parameters directly to be stored with the task.
  // In this case we are not going to make a request back for them.
  private byte[] executionBundle;

  // Last time the context was updated.
  private long lastContextUpdated;
}
