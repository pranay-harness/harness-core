package software.wings.service.impl;

import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import software.wings.beans.ApiKeyEntry;
import software.wings.service.intfc.ApiKeyService;

/**
 * {@link ApiKeyService} implementation for Harness Community Edition.
 */
@Singleton
public class ApiKeyServiceCommunityEdition extends ApiKeyServiceImpl implements ApiKeyService {
  @Override
  public ApiKeyEntry generate(String accountId, ApiKeyEntry apiKeyEntry) {
    throw new WingsException(ErrorCode.FEAT_UNAVAILABLE_IN_COMMUNITY_VERSION,
        "Creating API keys is not allowed in Community version of Harness", WingsException.USER);
  }

  @Override
  public ApiKeyEntry update(String uuid, String accountId, ApiKeyEntry apiKeyEntry) {
    throw new WingsException(ErrorCode.FEAT_UNAVAILABLE_IN_COMMUNITY_VERSION,
        "API keys are not a part of Community version of Harness", WingsException.USER);
  }
}
