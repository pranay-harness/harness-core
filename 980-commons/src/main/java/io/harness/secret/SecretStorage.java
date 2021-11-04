package io.harness.secret;

import java.io.IOException;

public interface SecretStorage {
  String getSecretBy(String secretReference) throws IOException;
}
