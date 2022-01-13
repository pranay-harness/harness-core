/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.event.handlers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.engine.OrchestrationEngine;
import io.harness.pms.contracts.execution.events.AdviserResponseRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@OwnedBy(HarnessTeam.PIPELINE)
@Singleton
public class AdviserResponseRequestProcessor implements SdkResponseProcessor {
  @Inject private OrchestrationEngine orchestrationEngine;

  @Override
  public void handleEvent(SdkResponseEventProto event) {
    AdviserResponseRequest request = event.getAdviserResponseRequest();
    orchestrationEngine.processAdviserResponse(event.getAmbiance(), request.getAdviserResponse());
  }
}
