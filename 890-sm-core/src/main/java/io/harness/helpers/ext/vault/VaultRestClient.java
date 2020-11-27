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
  boolean writeSecret(String authToken, String secretEngine, String fullPath, String value) throws IOException;

  boolean deleteSecret(String authToken, String secretEngine, String fullPath) throws IOException;

  String readSecret(String authToken, String secretEngine, String fullPath) throws IOException;

  VaultSecretMetadata readSecretMetadata(String authToken, String secretEngine, String fullPath) throws IOException;

  boolean renewToken(String authToken) throws IOException;
}
