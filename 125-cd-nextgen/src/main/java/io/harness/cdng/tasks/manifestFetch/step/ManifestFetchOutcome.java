package io.harness.cdng.tasks.manifestFetch.step;

import io.harness.data.Outcome;
import io.harness.git.model.GitFile;

import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("manifestFetchOutcome")
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
