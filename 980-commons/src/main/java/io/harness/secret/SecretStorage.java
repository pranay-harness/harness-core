package io.harness.secret;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import java.io.IOException;
import java.util.Optional;

@OwnedBy(PL)
public interface SecretStorage extends AutoCloseable {
  Optional<String> getSecretBy(String secretReference) throws IOException;
}
