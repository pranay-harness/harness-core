package software.wings.service.impl;

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

import com.google.inject.Singleton;

import software.wings.beans.ErrorCode;
import software.wings.common.UUIDGenerator;
import software.wings.exception.WingsException;
import software.wings.service.intfc.DownloadTokenService;
import software.wings.utils.CacheHelper;

import javax.cache.Cache;

/**
 * Created by peeyushaggarwal on 12/13/16.
 */
@Singleton
public class DownloadTokenServiceImpl implements DownloadTokenService {
  @Override
  public String createDownloadToken(String resource) {
    Cache<String, String> cache = CacheHelper.getCache("downloadTokenCache", String.class, String.class);
    String token = UUIDGenerator.getUuid();
    cache.put(token, resource);
    return token;
  }

  @Override
  public void validateDownloadToken(String resource, String token) {
    Cache<String, String> cache = CacheHelper.getCache("downloadTokenCache", String.class, String.class);
    String cachedResource = cache.get(token);
    if (!equalsIgnoreCase(cachedResource, resource)) {
      throw new WingsException(ErrorCode.INVALID_TOKEN);
    } else {
      cache.remove(token, resource);
    }
  }
}
