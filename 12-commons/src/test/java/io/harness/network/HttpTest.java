package io.harness.network;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import io.harness.threading.Concurrent;
import okhttp3.OkHttpClient;
import org.junit.Test;

public class HttpTest {
  @Test
  public void testValidUrl() {
    assertTrue(Http.validUrl("http://localhost"));
    assertTrue(Http.validUrl("https://localhost"));
    assertTrue(Http.validUrl("http://localhost/"));
    assertTrue(Http.validUrl("https://localhost/"));
    assertTrue(Http.validUrl("http://localhost.com"));
    assertTrue(Http.validUrl("https://localhost.com"));
    assertTrue(Http.validUrl("http://127.0.0.1"));
    assertTrue(Http.validUrl("https://127.0.0.1"));
    assertTrue(Http.validUrl("http://google.com"));
    assertTrue(Http.validUrl("https://google.com"));
    assertTrue(Http.validUrl("http://shortenedUrl"));
    assertTrue(Http.validUrl("https://shortenedUrl/"));
    assertTrue(Http.validUrl("http://toli:123"));

    assertFalse(Http.validUrl("invalidUrl"));
    assertFalse(Http.validUrl("invalidUrl"));
    assertFalse(Http.validUrl("abc://invalid.com"));
    assertFalse(Http.validUrl("abc://invalid.com"));
  }

  @Test
  public void testShouldUseNonProxy() {
    assertTrue(Http.shouldUseNonProxy("http://wings.jenkins.com", "*.jenkins.com|*.localhost|*.sumologic.com"));
    assertTrue(
        Http.shouldUseNonProxy("http://wings.jenkins.com", "*wings.jenkins.com|*.localhost|*wings.sumologic.com"));
    assertTrue(Http.shouldUseNonProxy("http://wings.jenkins.com:80", "*.jenkins.com|*localhost.com|*.sumologic.com"));
    assertFalse(Http.shouldUseNonProxy("http://wings.jenkins.com", "*localhost.com|*.sumologic.com"));
  }

  @Test
  public void testGetDomain() {
    assertEquals("localhost.com", Http.getDomain("http://localhost.com/temp"));
    assertEquals("localhost.com", Http.getDomain("https://localhost.com/temp"));
    assertEquals("localhost.com", Http.getDomain("localhost.com:8080/temp"));
    assertEquals("localhost.com", Http.getDomain("localhost.com:8080"));
  }

  @Test
  public void concurrencyTest() {
    Concurrent.test(5, i -> { final OkHttpClient client = Http.getUnsafeOkHttpClient("https://harness.io"); });
  }
}
