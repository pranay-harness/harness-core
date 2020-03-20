package io.harness.delegate.task.mixin;

import static io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability;
import static io.harness.rule.OwnerRule.GEORGE;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.executioncapability.HttpConnectionExecutionCapability;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class HttpConnectionExecutionCapabilityGeneratorTest extends CategoryTest {
  @Test
  @Owner(developers = GEORGE)
  @Category(UnitTests.class)
  public void testFetchCapabilityBasis() {
    HttpConnectionExecutionCapability noPortPath = buildHttpConnectionExecutionCapability("http://domain");
    assertThat(noPortPath.getUrl()).isNull();
    assertThat(noPortPath.fetchCapabilityBasis()).isEqualTo("http://domain");

    HttpConnectionExecutionCapability noPort = buildHttpConnectionExecutionCapability("http://domain/path");
    assertThat(noPort.getUrl()).isNull();
    assertThat(noPort.fetchCapabilityBasis()).isEqualTo("http://domain/path");

    HttpConnectionExecutionCapability all = buildHttpConnectionExecutionCapability("http://domain:80/path");
    assertThat(all.getUrl()).isNull();
    assertThat(all.fetchCapabilityBasis()).isEqualTo("http://domain:80/path");

    HttpConnectionExecutionCapability user = buildHttpConnectionExecutionCapability("http://user@domain:80/path");
    assertThat(user.getUrl()).isNull();
    assertThat(user.fetchCapabilityBasis()).isEqualTo("http://domain:80/path");

    HttpConnectionExecutionCapability userPass = buildHttpConnectionExecutionCapability("http://user:pass@domain");
    assertThat(userPass.getUrl()).isNull();
    assertThat(userPass.fetchCapabilityBasis()).isEqualTo("http://domain");

    HttpConnectionExecutionCapability bad = buildHttpConnectionExecutionCapability("http://domain.$$$");
    assertThat(bad.getUrl()).isNotNull();
    assertThat(bad.fetchCapabilityBasis()).isEqualTo("http://domain.$$$");

    HttpConnectionExecutionCapability expression = buildHttpConnectionExecutionCapability("http://${expression}");
    assertThat(expression.getUrl()).isNotNull();
    assertThat(expression.fetchCapabilityBasis()).isEqualTo("http://${expression}");
  }
}
