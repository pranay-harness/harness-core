/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.skip.factory;

import static io.harness.rule.OwnerRule.ALEXEI;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.harness.OrchestrationVisualizationTestBase;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.steps.SkipType;
import io.harness.rule.Owner;
import io.harness.skip.skipper.impl.NoOpSkipper;
import io.harness.skip.skipper.impl.SkipNodeSkipper;
import io.harness.skip.skipper.impl.SkipTreeSkipper;

import com.google.inject.Inject;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(HarnessTeam.PIPELINE)
public class VertexSkipperFactoryTest extends OrchestrationVisualizationTestBase {
  @Inject private VertexSkipperFactory vertexSkipperFactory;

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestObtainVertexSkipper() {
    assertThat(vertexSkipperFactory.obtainVertexSkipper(SkipType.SKIP_NODE)).isInstanceOf(SkipNodeSkipper.class);
    assertThat(vertexSkipperFactory.obtainVertexSkipper(SkipType.SKIP_TREE)).isInstanceOf(SkipTreeSkipper.class);
    assertThat(vertexSkipperFactory.obtainVertexSkipper(SkipType.NOOP)).isInstanceOf(NoOpSkipper.class);

    assertThatThrownBy(() -> vertexSkipperFactory.obtainVertexSkipper(SkipType.UNRECOGNIZED))
        .isInstanceOf(UnsupportedOperationException.class)
        .hasMessageContaining(format("Unsupported skipper type : [%s]", SkipType.UNRECOGNIZED));
  }
}
