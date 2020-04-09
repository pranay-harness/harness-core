package io.harness.delegate.task.mixin;

import static io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator.HttpCapabilityDetailsLevel.DOMAIN;
import static io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator.HttpCapabilityDetailsLevel.PATH;
import static io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator.HttpCapabilityDetailsLevel.QUERY;
import static io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability;
import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.KAMAL;
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
    assertThat(expression.getQuery()).isNull();

    HttpConnectionExecutionCapability withQuery =
        buildHttpConnectionExecutionCapability("http://35.239.148.216:8080/api/v1/query?query=up");
    assertThat(withQuery.getQuery()).isNull();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testBuildHttpConnectionExecutionCapability_defaultHttpCapabilityDetailsLevel() {
    HttpConnectionExecutionCapability capability =
        buildHttpConnectionExecutionCapability("http://domain.com:8080/path/1/2/3?q1=1&q2=2");
    assertThat(capability.getHost()).isEqualTo("domain.com");
    assertThat(capability.getScheme()).isEqualTo("http");
    assertThat(capability.getPort()).isEqualTo(8080);
    assertThat(capability.getPath()).isEqualTo("path/1/2/3");
    assertThat(capability.getUrl()).isNull();
    assertThat(capability.getQuery()).isNull();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testBuildHttpConnectionExecutionCapability_withHttpCapabilityDetailsLevelDomain() {
    HttpConnectionExecutionCapability capability =
        buildHttpConnectionExecutionCapability("http://domain.com:8080/path/1/2/3?q1=1&q2=2", DOMAIN);
    assertThat(capability.getHost()).isEqualTo("domain.com");
    assertThat(capability.getScheme()).isEqualTo("http");
    assertThat(capability.getPort()).isEqualTo(8080);
    assertThat(capability.getPath()).isNull();
    assertThat(capability.getUrl()).isNull();
    assertThat(capability.getQuery()).isNull();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testBuildHttpConnectionExecutionCapability_withHttpCapabilityDetailsLevelPath() {
    HttpConnectionExecutionCapability capability =
        buildHttpConnectionExecutionCapability("http://domain.com:8080/path/1/2/3?q1=1&q2=2", PATH);
    assertThat(capability.getHost()).isEqualTo("domain.com");
    assertThat(capability.getScheme()).isEqualTo("http");
    assertThat(capability.getPort()).isEqualTo(8080);
    assertThat(capability.getPath()).isEqualTo("path/1/2/3");
    assertThat(capability.getUrl()).isNull();
    assertThat(capability.getQuery()).isNull();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testBuildHttpConnectionExecutionCapability_withHttpCapabilityDetailsLevelQuery() {
    HttpConnectionExecutionCapability capability =
        buildHttpConnectionExecutionCapability("http://domain.com:8080/path/1/2/3?q1=1&q2=2", QUERY);
    assertThat(capability.getHost()).isEqualTo("domain.com");
    assertThat(capability.getScheme()).isEqualTo("http");
    assertThat(capability.getPort()).isEqualTo(8080);
    assertThat(capability.getPath()).isEqualTo("path/1/2/3");
    assertThat(capability.getUrl()).isNull();
    assertThat(capability.getQuery()).isEqualTo("q1=1&q2=2");
  }
}
