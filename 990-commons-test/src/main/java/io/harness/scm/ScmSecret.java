/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.scm;

import io.harness.exception.ScmSecretException;

import com.bettercloud.vault.Vault;
import com.bettercloud.vault.VaultConfig;
import com.bettercloud.vault.VaultException;
import com.bettercloud.vault.response.AuthResponse;
import com.bettercloud.vault.response.LogicalResponse;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Properties;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Hex;

@Slf4j
public class ScmSecret {
  public static final String passphrase = System.getenv("HARNESS_GENERATION_PASSPHRASE");

  private static final SecretName roleId = new SecretName("vault_role_id");
  private static final SecretName secretId = new SecretName("vault_secret_id");

  @Getter private final Properties secrets;

  @Getter(lazy = true) private final Vault vault = connect();

  public Vault connect() {
    VaultConfig config = null;
    try {
      config =
          new VaultConfig().engineVersion(Integer.valueOf(2)).address("https://vault-internal.harness.io:8200").build();
      final AuthResponse authResponse =
          new Vault(config).auth().loginByAppRole(decryptToString(roleId), decryptToString(secretId));

      config = new VaultConfig()
                   .engineVersion(Integer.valueOf(2))
                   .address("https://vault-internal.harness.io:8200")
                   .token(authResponse.getAuthClientToken())
                   .build();

      return new Vault(config);
    } catch (VaultException e) {
      log.error("Unable to connect to Vault", e);
    }
    return null;
  }

  public ScmSecret() {
    secrets = new Properties();
    try (InputStream in = getClass().getResourceAsStream("/secrets.properties")) {
      secrets.load(in);
    } catch (IOException exception) {
      throw new ScmSecretException(exception);
    }
  }

  public boolean isInitialized() {
    return passphrase != null;
  }

  public byte[] decrypt(SecretName name) {
    return decrypt(secrets.getProperty(name.getValue()));
  }

  public byte[] decrypt(String cipheredSecretHex) {
    try {
      if (!isInitialized()) {
        return "You can't decrypt in this environment. You need to set HARNESS_GENERATION_PASSPHRASE in your environment!"
            .getBytes(StandardCharsets.UTF_8);
      }
      Key aesKey = new SecretKeySpec(Hex.decodeHex(passphrase.toCharArray()), "AES");
      Cipher cipher = Cipher.getInstance("AES");
      cipher.init(Cipher.DECRYPT_MODE, aesKey);
      return cipher.doFinal(Hex.decodeHex(cipheredSecretHex.toCharArray()));
    } catch (Exception e) {
      throw new ScmSecretException(e);
    }
  }

  public String decryptToString(SecretName name) {
    return decryptToString(secrets.getProperty(name.getValue()));
  }

  public String decryptToString(String cipheredSecretHex) {
    return new String(decrypt(cipheredSecretHex), StandardCharsets.UTF_8);
  }

  public char[] decryptToCharArray(SecretName name) {
    return decryptToCharArray(secrets.getProperty(name.getValue()));
  }

  public char[] decryptToCharArray(String cipheredSecretHex) {
    return decryptToString(cipheredSecretHex).toCharArray();
  }

  public static String encrypt(byte[] secret, String passphrase) {
    try {
      Key aesKey = new SecretKeySpec(Hex.decodeHex(passphrase.toCharArray()), "AES");
      Cipher cipher = Cipher.getInstance("AES");
      cipher.init(Cipher.ENCRYPT_MODE, aesKey);
      return Hex.encodeHexString(cipher.doFinal(secret));
    } catch (Exception e) {
      throw new ScmSecretException(e);
    }
  }

  public String encrypt(byte[] secret) {
    return encrypt(secret, passphrase);
  }

  public String obtain(String path, String key) throws VaultException {
    final LogicalResponse read = getVault().logical().read(path);
    return read.getData().get(key);
  }
}
