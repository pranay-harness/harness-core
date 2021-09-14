/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.sdk.core.execution.invokers;

import static io.harness.rule.OwnerRule.SAHIL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.exceptionmanager.ExceptionManager;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.sdk.core.PmsSdkCoreTestBase;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.rule.Owner;

import java.util.ArrayList;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(HarnessTeam.PIPELINE)
public class StrategyHelperTest extends PmsSdkCoreTestBase {
  @Mock ExceptionManager exceptionManager;
  @InjectMocks StrategyHelper strategyHelper;

  @Test
  @Owner(developers = SAHIL)
  @Category(UnitTests.class)
  public void testHandleException() {
    InvalidRequestException exception = new InvalidRequestException("Hello world");
    when(exceptionManager.buildResponseFromException(exception)).thenReturn(new ArrayList<>());
    StepResponse stepResponse = strategyHelper.handleException(exception);
    assertThat(stepResponse.getStatus()).isEqualTo(Status.FAILED);
  }
}
