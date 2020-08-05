package io.harness.ng.core.services.api;

import io.harness.stream.BoundedInputStream;
import software.wings.security.encryption.EncryptedData;

import java.util.List;

public interface NGSecretFileService {
  EncryptedData create(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier,
      String secretManagerIdentifier, String name, String description, List<String> tags, BoundedInputStream file);

  boolean update(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier,
      String description, List<String> tags, BoundedInputStream file);
}
