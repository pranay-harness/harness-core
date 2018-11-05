package software.wings.service.impl;

import static io.harness.exception.WingsException.USER_ADMIN;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.auth0.jwt.interfaces.Claim;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import software.wings.security.SecretManager;
import software.wings.security.SecretManager.JWT_CATEGORY;
import software.wings.service.intfc.DownloadTokenService;

import java.util.Map;

@Singleton
public class DownloadTokenServiceImpl implements DownloadTokenService {
  public static final String CLAIM_KEY = "resource";
  @Inject private SecretManager secretManager;

  @Override
  public String createDownloadToken(String resource) {
    return secretManager.generateJWTToken(ImmutableMap.of(CLAIM_KEY, resource), JWT_CATEGORY.EXTERNAL_SERVICE_SECRET);
  }

  @Override
  public void validateDownloadToken(String resource, String jwtToken) {
    Map<String, Claim> claimMap = secretManager.verifyJWTToken(jwtToken, JWT_CATEGORY.EXTERNAL_SERVICE_SECRET);
    String resourceFromClaim = claimMap.get(CLAIM_KEY) == null ? null : claimMap.get(CLAIM_KEY).asString();
    if (!equalsIgnoreCase(resourceFromClaim, resource)) {
      throw new WingsException(ErrorCode.INVALID_TOKEN, USER_ADMIN);
    }
  }
}
