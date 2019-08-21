package io.harness.network;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.threading.Concurrent;
import okhttp3.OkHttpClient;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class HttpTest extends CategoryTest {
  @Test
  @Category(UnitTests.class)
  public void testValidUrl() {
    assertThat(Http.validUrl("http://localhost")).isTrue();
    assertThat(Http.validUrl("https://localhost")).isTrue();
    assertThat(Http.validUrl("http://localhost/")).isTrue();
    assertThat(Http.validUrl("https://localhost/")).isTrue();
    assertThat(Http.validUrl("http://localhost.com")).isTrue();
    assertThat(Http.validUrl("https://localhost.com")).isTrue();
    assertThat(Http.validUrl("http://127.0.0.1")).isTrue();
    assertThat(Http.validUrl("https://127.0.0.1")).isTrue();
    assertThat(Http.validUrl("http://google.com")).isTrue();
    assertThat(Http.validUrl("https://google.com")).isTrue();
    assertThat(Http.validUrl("http://shortenedUrl")).isTrue();
    assertThat(Http.validUrl("https://shortenedUrl/")).isTrue();
    assertThat(Http.validUrl("http://toli:123")).isTrue();

    assertFalse(Http.validUrl("invalidUrl"));
    assertFalse(Http.validUrl("invalidUrl"));
    assertFalse(Http.validUrl("abc://invalid.com"));
    assertFalse(Http.validUrl("abc://invalid.com"));
  }

  @Test
  @Category(UnitTests.class)
  public void testShouldUseNonProxy() {
    assertThat(Http.shouldUseNonProxy("http://wings.jenkins.com", "*.jenkins.com|*.localhost|*.sumologic.com"))
        .isTrue();
    assertThat(
        Http.shouldUseNonProxy("http://wings.jenkins.com", "*wings.jenkins.com|*.localhost|*wings.sumologic.com"))
        .isTrue();
    assertThat(Http.shouldUseNonProxy("http://wings.jenkins.com:80", "*.jenkins.com|*localhost.com|*.sumologic.com"))
        .isTrue();
    assertFalse(Http.shouldUseNonProxy("http://wings.jenkins.com", "*localhost.com|*.sumologic.com"));
  }

  @Test
  @Category(UnitTests.class)
  public void testGetDomain() {
    assertEquals("localhost.com", Http.getDomain("http://localhost.com/temp"));
    assertEquals("localhost.com", Http.getDomain("https://localhost.com/temp"));
    assertEquals("localhost.com", Http.getDomain("localhost.com:8080/temp"));
    assertEquals("localhost.com", Http.getDomain("localhost.com:8080"));
  }

  @Test
  @Category(UnitTests.class)
  public void testGetDomainWithPort() {
    assertEquals("localhost.com", Http.getDomainWithPort("http://localhost.com/temp"));
    assertEquals("localhost.com", Http.getDomainWithPort("http://localhost.com/"));
    assertEquals("localhost.com", Http.getDomainWithPort("https://localhost.com/temp"));
    assertEquals("localhost.com:5000", Http.getDomainWithPort("http://localhost.com:5000/temp"));
    assertEquals("localhost.com:8080", Http.getDomainWithPort("localhost.com:8080/temp"));
    assertEquals("localhost.com:8080", Http.getDomainWithPort("localhost.com:8080"));
  }

  @Test
  @Category(UnitTests.class)
  public void testGetBaseUrl() {
    assertEquals("http://localhost.com/", Http.getBaseUrl("http://localhost.com/temp"));
    assertEquals("http://localhost.com/", Http.getBaseUrl("http://localhost.com/"));
    assertEquals("https://localhost.com/", Http.getBaseUrl("https://localhost.com/temp"));
    assertEquals("http://localhost.com:5000/", Http.getBaseUrl("http://localhost.com:5000/temp"));
    assertEquals("http://localhost.com:8080/", Http.getBaseUrl("localhost.com:8080/temp"));
    assertEquals("https://localhost.com:8443/", Http.getBaseUrl("https://localhost.com:8443"));
  }

  @Test
  @Category(UnitTests.class)
  public void testJoinHostPort() {
    assertEquals("localhost:443", Http.joinHostPort("localhost", "443"));
    assertEquals("127.0.0.1:443", Http.joinHostPort("127.0.0.1", "443"));
    assertEquals("[2001:0db8:85a3:0000:0000:8a2e:0370:7334]:443",
        Http.joinHostPort("2001:0db8:85a3:0000:0000:8a2e:0370:7334", "443"));
  }

  @Test
  @Category(UnitTests.class)
  public void concurrencyTest() {
    Concurrent.test(5, i -> { final OkHttpClient client = Http.getUnsafeOkHttpClient("https://harness.io"); });
  }
}
