package io.harness.cdng.tasks.manifestFetch.step;

import io.harness.data.Outcome;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import software.wings.beans.yaml.GitFile;

import java.io.Serializable;
import java.util.List;

@Value
@Builder
public class ManifestFetchOutcome implements Outcome {
  private List<ManifestDataDetails> manifestDataDetailsForSpec;
  private List<ManifestDataDetails> manifestDataDetailsForOverrides;

  @Data
  @Builder
  public static class ManifestDataDetails implements Serializable {
    String identifier;
    private List<GitFile> gitFiles;
  }
}
