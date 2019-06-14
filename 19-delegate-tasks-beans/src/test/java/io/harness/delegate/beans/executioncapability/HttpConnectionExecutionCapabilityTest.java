package io.harness.delegate.beans.executioncapability;

import static org.junit.Assert.assertEquals;

import io.harness.category.element.UnitTests;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.List;

public class HttpConnectionExecutionCapabilityTest {
  public static final String HOST_NAME = "SOME_HOST";
  private static final String PORT_HTTP = "80";
  private static final String PORT_HTTPS = "443";
  private static final String SCHEME_HTTPS = "HTTPS";
  private static final String SCHEME_HTTP = "HTTP";

  private static final String URL_HTTP = SCHEME_HTTP + "://" + HOST_NAME;
  private static final String URL_HTTPS = SCHEME_HTTPS + "://" + HOST_NAME;

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
  @Category(UnitTests.class)
  public void processExecutorArguments() {
    List<String> execArgs = httpConnectionExecutionCapability.processExecutorArguments();
    assertEquals(execArgs.size(), 5);
    assertEquals(execArgs.get(3), HOST_NAME);
    assertEquals(execArgs.get(4), PORT_HTTP);

    execArgs = httpsConnectionExecutionCapability.processExecutorArguments();
    assertEquals(execArgs.size(), 5);
    assertEquals(execArgs.get(3), HOST_NAME);
    assertEquals(execArgs.get(4), PORT_HTTPS);
  }

  @Test
  @Category(UnitTests.class)
  public void fetchCapabilityBasis() {
    String capabilityBasis = httpConnectionExecutionCapability.fetchCapabilityBasis();
    assertEquals(capabilityBasis, URL_HTTP);

    capabilityBasis = httpsConnectionExecutionCapability.fetchCapabilityBasis();
    assertEquals(capabilityBasis, URL_HTTPS);
  }
}