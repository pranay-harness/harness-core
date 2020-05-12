package software.wings.helpers.ext.azure.devops;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.dev.OwnedBy;
import lombok.Data;

import java.util.List;

@OwnedBy(CDC)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class AzureArtifactsPackageVersions {
  private int count;
  private List<AzureArtifactsPackageVersion> value;
}