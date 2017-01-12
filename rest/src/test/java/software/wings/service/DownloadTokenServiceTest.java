package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import org.junit.Before;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.exception.WingsException;
import software.wings.service.intfc.DownloadTokenService;

import java.util.Optional;
import javax.cache.Cache;
import javax.cache.Caching;
import javax.cache.configuration.Configuration;
import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 12/13/16.
 */
@software.wings.rules.Cache
public class DownloadTokenServiceTest extends WingsBaseTest {
  private Cache<String, String> cache =
      Optional.ofNullable(Caching.getCache("downloadTokenCache", String.class, String.class))
          .orElseGet(()
                         -> Caching.getCachingProvider().getCacheManager().createCache(
                             "downloadTokenCache", new Configuration<String, String>() {
                               private static final long serialVersionUID = 1L;

                               @Override
                               public Class<String> getKeyType() {
                                 return String.class;
                               }

                               @Override
                               public Class<String> getValueType() {
                                 return String.class;
                               }

                               @Override
                               public boolean isStoreByValue() {
                                 return true;
                               }
                             }));

  @Inject private DownloadTokenService downloadTokenService;

  @Before
  public void setUp() {
    cache = Optional.ofNullable(Caching.getCache("downloadTokenCache", String.class, String.class))
                .orElseGet(()
                               -> Caching.getCachingProvider().getCacheManager().createCache(
                                   "downloadTokenCache", new Configuration<String, String>() {
                                     private static final long serialVersionUID = 1L;

                                     @Override
                                     public Class<String> getKeyType() {
                                       return String.class;
                                     }

                                     @Override
                                     public Class<String> getValueType() {
                                       return String.class;
                                     }

                                     @Override
                                     public boolean isStoreByValue() {
                                       return true;
                                     }
                                   }));
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
