package software.wings.utils;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HttpUtilTest {
  @Test
  public void testValidUrl() {
    assertTrue(HttpUtil.validUrl("http://localhost"));
    assertTrue(HttpUtil.validUrl("https://localhost"));
    assertTrue(HttpUtil.validUrl("http://localhost/"));
    assertTrue(HttpUtil.validUrl("https://localhost/"));
    assertTrue(HttpUtil.validUrl("http://localhost.com"));
    assertTrue(HttpUtil.validUrl("https://localhost.com"));
    assertTrue(HttpUtil.validUrl("http://127.0.0.1"));
    assertTrue(HttpUtil.validUrl("https://127.0.0.1"));
    assertTrue(HttpUtil.validUrl("http://google.com"));
    assertTrue(HttpUtil.validUrl("https://google.com"));
    assertTrue(HttpUtil.validUrl("http://shortenedUrl"));
    assertTrue(HttpUtil.validUrl("https://shortenedUrl/"));
    assertTrue(HttpUtil.validUrl("http://toli:123"));

    assertFalse(HttpUtil.validUrl("invalidUrl"));
    assertFalse(HttpUtil.validUrl("invalidUrl"));
    assertFalse(HttpUtil.validUrl("abc://invalid.com"));
    assertFalse(HttpUtil.validUrl("abc://invalid.com"));
  }
}
