package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.Before;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.exception.WingsException;
import software.wings.service.intfc.DownloadTokenService;
import software.wings.utils.CacheHelper;

import javax.cache.Cache;
import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 12/13/16.
 */
@software.wings.rules.Cache(cacheName = "downloadTokenCache", keyType = String.class, valueType = String.class)
public class DownloadTokenServiceTest extends WingsBaseTest {
  private Cache<String, String> cache;

  @Inject private DownloadTokenService downloadTokenService;

  @Before
  public void setUp() {
    cache = CacheHelper.getCache("downloadTokenCache", String.class, String.class);
  }

  @Test
  public void shouldCreateToken() {
    String token = downloadTokenService.createDownloadToken("resource");
    assertThat(token).isNotEmpty();
    assertThat(cache.get(token)).isEqualTo("resource");
  }

  @Test
  public void shouldValidateToken() {
    cache.put("token", "resource");
    downloadTokenService.validateDownloadToken("resource", "token");
  }

  @Test
  public void shouldThrowExceptionWhenNoTokenFoundOnValidation() {
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> downloadTokenService.validateDownloadToken("resource", "token"));
  }

  @Test
  public void shouldThrowExceptionWhenResourceDoesntMatchOnValiation() {
    cache.put("token", "resource");
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> downloadTokenService.validateDownloadToken("resource1", "token"));
  }
}
