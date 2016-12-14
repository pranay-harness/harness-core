package software.wings.service.impl;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

import software.wings.beans.ErrorCodes;
import software.wings.common.UUIDGenerator;
import software.wings.exception.WingsException;
import software.wings.service.intfc.DownloadTokenService;

import javax.cache.Cache;
import javax.cache.Caching;
import javax.cache.configuration.Configuration;

/**
 * Created by peeyushaggarwal on 12/13/16.
 */
public class DownloadTokenServiceImpl implements DownloadTokenService {
  @Override
  public String createDownloadToken(String resource) {
    Cache<String, String> cache = Caching.getCache("downloadTokenCache", String.class, String.class);
    if (cache == null) {
      cache = Caching.getCachingProvider().getCacheManager().createCache(
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
          });
    }
    String token = UUIDGenerator.getUuid();
    cache.put(token, resource);
    return token;
  }

  @Override
  public void validateDownloadToken(String resource, String token) {
    Cache<String, String> cache = Caching.getCache("downloadTokenCache", String.class, String.class);
    if (cache == null) {
      cache = Caching.getCachingProvider().getCacheManager().createCache(
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
          });
    }
    String cachedResource = cache.get(token);
    if (!equalsIgnoreCase(cachedResource, resource)) {
      throw new WingsException(ErrorCodes.INVALID_TOKEN);
    } else {
      cache.remove(token, resource);
    }
  }
}
