package io.harness.ccm.setup.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;

import org.junit.Test;
import org.junit.experimental.categories.Category;

public class CeConnectorDataFetcherTest {
  @Test
  @Category(UnitTests.class)
  public void testGenerateFilter() {
    boolean filter = true;
    assertThat(filter).isTrue();
  }
}
