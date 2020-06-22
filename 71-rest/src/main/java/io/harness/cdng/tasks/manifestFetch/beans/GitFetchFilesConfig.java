package io.harness.cdng.tasks.manifestFetch.beans;

import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.GitConfig;

import java.util.List;

@Value
@Builder
public class GitFetchFilesConfig {
  private String identifier;
  private List<String> paths;
  private GitConfig gitConfig;
  private GitStore gitStore;
  private List<EncryptedDataDetail> encryptedDataDetails;
  private boolean succeedIfFileNotFound;
}
