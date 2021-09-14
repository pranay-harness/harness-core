/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.helpers.ext.vault;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import java.io.IOException;

/**
 * The absolute path format is (it has to started with a '/'):
 *  /foo/bar/SampleSecret#MyKeyName
 */

@OwnedBy(PL)
public interface VaultRestClient {
  boolean writeSecret(String authToken, String namespace, String secretEngine, String fullPath, String value)
      throws IOException;

  boolean deleteSecret(String authToken, String namespace, String secretEngine, String fullPath) throws IOException;

  String readSecret(String authToken, String namespace, String secretEngine, String fullPath) throws IOException;

  VaultSecretMetadata readSecretMetadata(String authToken, String namespace, String secretEngine, String fullPath)
      throws IOException;
}
