package io.harness.delegate.beans.executioncapability;

import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class HttpConnectionExecutionCapabilityTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testFetchCapabilityBasis() {
    HttpConnectionExecutionCapability original = HttpConnectionExecutionCapability.builder().url("dummy").build();
    assertThat(original.fetchCapabilityBasis()).isEqualTo("dummy");

    HttpConnectionExecutionCapability noPortPath =
        HttpConnectionExecutionCapability.builder().scheme("http").host("domain").port(-1).build();
    assertThat(noPortPath.fetchCapabilityBasis()).isEqualTo("http://domain");

    HttpConnectionExecutionCapability noPort =
        HttpConnectionExecutionCapability.builder().scheme("http").host("domain").port(-1).path("path").build();
    assertThat(noPort.fetchCapabilityBasis()).isEqualTo("http://domain/path");

    HttpConnectionExecutionCapability noPath =
        HttpConnectionExecutionCapability.builder().scheme("http").host("domain").port(80).build();
    assertThat(noPath.fetchCapabilityBasis()).isEqualTo("http://domain:80");

    HttpConnectionExecutionCapability all =
        HttpConnectionExecutionCapability.builder().scheme("http").host("domain").port(80).path("path").build();
    assertThat(all.fetchCapabilityBasis()).isEqualTo("http://domain:80/path");
  }
}