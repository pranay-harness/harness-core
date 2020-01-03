package io.harness.delegate.beans.executioncapability;

import static io.harness.rule.OwnerRule.PRASHANT;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class HttpConnectionExecutionCapabilityTest extends CategoryTest {
  public static final String HOST_NAME = "SOME_HOST";
  private static final String PORT_HTTP = "80";
  private static final String PORT_HTTPS = "443";
  private static final String SCHEME_HTTPS = "HTTPS";
  private static final String SCHEME_HTTP = "HTTP";

  private static final String URL_HTTP = SCHEME_HTTP + "://" + HOST_NAME + ":" + PORT_HTTP;
  private static final String URL_HTTPS = SCHEME_HTTPS + "://" + HOST_NAME + ":" + PORT_HTTPS;
  ;

  private HttpConnectionExecutionCapability httpConnectionExecutionCapability;
  private HttpConnectionExecutionCapability httpsConnectionExecutionCapability;

  @Before
  public void setUp() throws Exception {
    httpConnectionExecutionCapability =
        HttpConnectionExecutionCapability.builder().hostName(HOST_NAME).scheme(SCHEME_HTTP).url(URL_HTTP).build();

    httpsConnectionExecutionCapability =
        HttpConnectionExecutionCapability.builder().hostName(HOST_NAME).scheme(SCHEME_HTTPS).url(URL_HTTPS).build();
  }

  @After
  public void tearDown() throws Exception {
    httpConnectionExecutionCapability = null;
    httpsConnectionExecutionCapability = null;
  }

  @Test
  @Owner(developers = PRASHANT)
  @Category(UnitTests.class)
  public void fetchCapabilityBasis() {
    String capabilityBasis = httpConnectionExecutionCapability.fetchCapabilityBasis();
    assertThat(URL_HTTP).isEqualTo(capabilityBasis);

    capabilityBasis = httpsConnectionExecutionCapability.fetchCapabilityBasis();
    assertThat(URL_HTTPS).isEqualTo(capabilityBasis);
  }
}