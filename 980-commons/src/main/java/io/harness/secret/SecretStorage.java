package io.harness.secret;

import java.io.IOException;
import java.util.Optional;

public interface SecretStorage {
  Optional<String> getSecretBy(String secretReference) throws IOException;
}
