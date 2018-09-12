package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import io.harness.exception.WingsException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.service.intfc.DownloadTokenService;
import software.wings.utils.CacheHelper;

import javax.cache.Cache;

/**
 * Created by peeyushaggarwal on 12/13/16.
 */
public class DownloadTokenServiceTest extends WingsBaseTest {
  @Mock private Cache<String, String> cache;

  @Mock private CacheHelper cacheHelper;

  @Inject @InjectMocks private DownloadTokenService downloadTokenService;

  @Before
  public void setUp() {
    when(cacheHelper.getCache("downloadTokenCache", String.class, String.class)).thenReturn(cache);
  }

  @Test
  public void shouldCreateToken() {
    when(cache.get(anyString())).thenReturn("resource");
    String token = downloadTokenService.createDownloadToken("resource");
    assertThat(token).isNotEmpty();
    assertThat(cache.get(token)).isEqualTo("resource");
  }

  @Test
  public void shouldValidateToken() {
    cache.put("token", "resource");
    when(cache.get("token")).thenReturn("resource");
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
