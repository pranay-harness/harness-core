package io.harness.cdng.tasks.manifestFetch.step;

import io.harness.git.model.GitFile;
import io.harness.pms.sdk.core.data.Outcome;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.io.Serializable;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@TypeAlias("manifestFetchOutcome")
@JsonTypeName("manifestFetchOutcome")
public class ManifestFetchOutcome implements Outcome {
  List<ManifestDataDetails> manifestDataDetailsForSpec;
  List<ManifestDataDetails> manifestDataDetailsForOverrides;

  @Override
  public String getType() {
    return "manifestFetchOutcome";
  }

  @Data
  @Builder
  public static class ManifestDataDetails implements Serializable {
    String identifier;
    private List<GitFile> gitFiles;
  }
}
