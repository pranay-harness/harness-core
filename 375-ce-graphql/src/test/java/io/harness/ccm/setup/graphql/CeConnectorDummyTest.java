/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.setup.graphql;

import static io.harness.annotations.dev.HarnessTeam.CE;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;

import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CE)
public class CeConnectorDummyTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void testGenerateFilter() {
    boolean filter = true;
    assertThat(filter).isTrue();
  }
}
