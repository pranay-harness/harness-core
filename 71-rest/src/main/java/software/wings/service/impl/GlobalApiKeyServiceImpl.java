package software.wings.service.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.WingsException;
import software.wings.beans.GlobalApiKey;
import software.wings.beans.GlobalApiKey.ProviderType;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptionUtils;
import software.wings.service.intfc.GlobalApiKeyService;
import software.wings.utils.CryptoUtil;
import software.wings.utils.Validator;

import java.nio.charset.Charset;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
public class GlobalApiKeyServiceImpl implements GlobalApiKeyService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public String generate(ProviderType providerType) {
    String apiKey;
    GlobalApiKey apiKeyObject = getApiKeyObject(providerType);
    if (apiKeyObject != null) {
      throw new WingsException("Global api key already exists for the given providerType.", WingsException.USER);
    } else {
      apiKey = getNewKey();
      wingsPersistence.save(GlobalApiKey.builder()
                                .encryptedKey(EncryptionUtils.encrypt(apiKey.getBytes(Charset.forName("UTF-8")), null))
                                .providerType(providerType)
                                .build());
    }

    return apiKey;
  }

  private String getNewKey() {
    int KEY_LEN = 80;
    return CryptoUtil.secureRandAlphaNumString(KEY_LEN);
  }

  @Override
  public String get(ProviderType providerType) {
    GlobalApiKey globalApiKey =
        wingsPersistence.createQuery(GlobalApiKey.class).filter("providerType", providerType).get();
    Validator.notNullCheck("global api key null for provider type " + providerType, globalApiKey);
    return getDecryptedKey(globalApiKey.getEncryptedKey());
  }

  private String getDecryptedKey(byte[] encryptedKey) {
    return new String(EncryptionUtils.decrypt(encryptedKey, null), Charset.forName("UTF-8"));
  }

  private GlobalApiKey getApiKeyObject(ProviderType providerType) {
    return wingsPersistence.createQuery(GlobalApiKey.class).filter("providerType", providerType).get();
  }

  @Override
  public void delete(ProviderType providerType) {
    GlobalApiKey apiKeyObject = getApiKeyObject(providerType);
    if (apiKeyObject != null) {
      wingsPersistence.delete(GlobalApiKey.class, apiKeyObject.getUuid());
    }
  }
}