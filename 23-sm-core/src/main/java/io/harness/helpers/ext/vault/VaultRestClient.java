package io.harness.helpers.ext.vault;

import io.harness.helpers.ext.vault.VaultSecretMetadata;

import java.io.IOException;

/**
 * The absolute path format is (it has to started with a '/'):
 *  /foo/bar/SampleSecret#MyKeyName
 *
 * Created by rsingh on 11/3/17.
 */
public interface VaultRestClient {
  boolean writeSecret(String authToken, String secretEngine, String fullPath, String value) throws IOException;

  boolean deleteSecret(String authToken, String secretEngine, String fullPath) throws IOException;

  String readSecret(String authToken, String secretEngine, String fullPath) throws IOException;

  VaultSecretMetadata readSecretMetadata(String authToken, String secretEngine, String fullPath) throws IOException;

  boolean renewToken(String authToken) throws IOException;
}