/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.execution.export.request;

import static io.harness.rule.OwnerRule.GARVIT;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.execution.export.request.ExportExecutionsRequest.ExportExecutionsRequestKeys;
import io.harness.rule.Owner;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ExportExecutionsRequestTest extends CategoryTest {
  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testObtainNextIteration() {
    ExportExecutionsRequest request = RequestTestUtils.prepareExportExecutionsRequest();
    assertThat(request.obtainNextIteration(ExportExecutionsRequestKeys.nextIteration)).isEqualTo(1);
    assertThat(request.obtainNextIteration(ExportExecutionsRequestKeys.nextCleanupIteration)).isEqualTo(2);
    assertThatThrownBy(() -> request.obtainNextIteration("random")).isInstanceOf(IllegalStateException.class);
  }

  @Test
  @Owner(developers = GARVIT)
  @Category(UnitTests.class)
  public void testUpdateNextIteration() {
    ExportExecutionsRequest request = RequestTestUtils.prepareExportExecutionsRequest();
    request.updateNextIteration(ExportExecutionsRequestKeys.nextIteration, 3L);
    assertThat(request.getNextIteration()).isEqualTo(3);

    request.updateNextIteration(ExportExecutionsRequestKeys.nextCleanupIteration, 4L);
    assertThat(request.getNextCleanupIteration()).isEqualTo(4);

    assertThatThrownBy(() -> request.updateNextIteration("random", 5L)).isInstanceOf(IllegalStateException.class);
  }
}
