/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.functional.batchprocessing;

import static io.harness.rule.OwnerRule.AVMOHAN;
import static io.harness.rule.OwnerRule.UTSAV;

import static org.assertj.core.api.Assertions.assertThatCode;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.FunctionalTests;
import io.harness.rule.Owner;
import io.harness.testframework.framework.BatchProcessingExecutor;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PL)
public class BatchProcessingFunctionalTest extends CategoryTest {
  private static final BatchProcessingExecutor batchProcessingExecutor = new BatchProcessingExecutor();

  @Test
  @Owner(developers = {AVMOHAN, UTSAV})
  @Category(FunctionalTests.class)
  @Ignore("Functional Flakiness fixing. Needs to be fixed")
  public void shouldEnsureBatchProcessingStartsUp() throws Exception {
    assertThatCode(() -> batchProcessingExecutor.ensureBatchProcessing(getClass())).doesNotThrowAnyException();
  }
}
