/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import javax.crypto.SecretKey;

/**
 * Created by mike@ on 4/24/17.
 */
@OwnedBy(PL)
public interface EncryptionInterface {
  SecretKey getSecretKey();

  byte[] encrypt(byte[] content);

  byte[] decrypt(byte[] encrypted);
}
