package io.harness.helpers;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.SecretKey;
import io.harness.beans.SecretManagerConfig;
import io.harness.secretkey.SecretKeyConstants;
import io.harness.secretkey.SecretKeyService;

import software.wings.beans.LocalEncryptionConfig;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@OwnedBy(HarnessTeam.PL)
@Singleton
public class LocalEncryptorHelper {
  @Inject @Named(SecretKeyConstants.AES_SECRET_KEY) private SecretKeyService secretKeyService;

  public void populateConfigForEncryption(SecretManagerConfig secretManagerConfig) {
    SecretKey secretKey = secretKeyService.createSecretKey();
    ((LocalEncryptionConfig) secretManagerConfig).setSecretKey(secretKey);
  }
}
