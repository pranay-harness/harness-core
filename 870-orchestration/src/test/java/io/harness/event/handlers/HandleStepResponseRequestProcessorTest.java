/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.event.handlers;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.SAHIL;

import static org.mockito.Mockito.verify;

import io.harness.OrchestrationTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.engine.OrchestrationEngine;
import io.harness.pms.contracts.execution.events.HandleStepResponseRequest;
import io.harness.pms.contracts.execution.events.SdkResponseEventProto;
import io.harness.pms.contracts.execution.events.SdkResponseEventType;
import io.harness.rule.Owner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

@OwnedBy(HarnessTeam.PIPELINE)
public class HandleStepResponseRequestProcessorTest extends OrchestrationTestBase {
  @Mock OrchestrationEngine engine;
  @InjectMocks HandleStepResponseRequestProcessor handleStepResponseEventHandler;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
  }

  @After
  public void verifyMocks() {
    Mockito.verifyNoMoreInteractions(engine);
  }

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testHandleEvent() {
    String nodeExecutionId = generateUuid();
    HandleStepResponseRequest handleStepResponseRequest = HandleStepResponseRequest.newBuilder().build();
    handleStepResponseEventHandler.handleEvent(SdkResponseEventProto.newBuilder()
                                                   .setNodeExecutionId(nodeExecutionId)
                                                   .setHandleStepResponseRequest(handleStepResponseRequest)
                                                   .setSdkResponseEventType(SdkResponseEventType.HANDLE_STEP_RESPONSE)
                                                   .build());
    verify(engine).handleStepResponse(nodeExecutionId, handleStepResponseRequest.getStepResponse());
  }
}
