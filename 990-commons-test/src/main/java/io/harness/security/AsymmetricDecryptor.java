/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.security;

import io.harness.scm.ScmSecret;
import io.harness.scm.SecretName;

import com.google.common.base.Charsets;
import com.google.inject.Inject;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class AsymmetricDecryptor {
  private PrivateKey privateKey;

  @Inject
  public AsymmetricDecryptor(ScmSecret scmSecret) throws NoSuchAlgorithmException, InvalidKeySpecException {
    final byte[] testPrivateKeys = scmSecret.decrypt(SecretName.builder().value("test_private_key").build());
    PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(testPrivateKeys);
    KeyFactory kf = KeyFactory.getInstance("RSA");
    privateKey = kf.generatePrivate(spec);
  }

  public String decryptText(byte[] msg) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException,
                                               BadPaddingException, IllegalBlockSizeException {
    Cipher cipher = Cipher.getInstance("RSA");
    cipher.init(Cipher.DECRYPT_MODE, privateKey);
    return new String(cipher.doFinal(msg), Charsets.UTF_8);
  }
}
