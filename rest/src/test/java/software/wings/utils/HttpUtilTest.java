package software.wings.utils;

import org.junit.Assert;
import org.junit.Test;

public class HttpUtilTest {
  @Test
  public void testValidUrl() {
    Assert.assertTrue(HttpUtil.validUrl("http://localhost"));
    Assert.assertTrue(HttpUtil.validUrl("https://localhost"));
    Assert.assertTrue(HttpUtil.validUrl("http://localhost/"));
    Assert.assertTrue(HttpUtil.validUrl("https://localhost/"));
    Assert.assertTrue(HttpUtil.validUrl("http://localhost.com"));
    Assert.assertTrue(HttpUtil.validUrl("https://localhost.com"));
    Assert.assertTrue(HttpUtil.validUrl("http://127.0.0.1"));
    Assert.assertTrue(HttpUtil.validUrl("https://127.0.0.1"));
    Assert.assertTrue(HttpUtil.validUrl("http://google.com"));
    Assert.assertTrue(HttpUtil.validUrl("https://google.com"));
    Assert.assertTrue(HttpUtil.validUrl("http://shortenedUrl"));
    Assert.assertTrue(HttpUtil.validUrl("https://shortenedUrl/"));
    Assert.assertTrue(HttpUtil.validUrl("http://toli:123"));

    Assert.assertFalse(HttpUtil.validUrl("invalidUrl"));
    Assert.assertFalse(HttpUtil.validUrl("invalidUrl"));
    Assert.assertFalse(HttpUtil.validUrl("abc://invalid.com"));
    Assert.assertFalse(HttpUtil.validUrl("abc://invalid.com"));
  }
}
