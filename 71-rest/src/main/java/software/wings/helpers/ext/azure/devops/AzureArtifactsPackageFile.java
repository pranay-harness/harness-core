package software.wings.helpers.ext.azure.devops;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.dev.OwnedBy;
import lombok.Data;

import java.util.List;

@OwnedBy(CDC)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class AzureArtifactsPackageFile {
  private String name;
  private List<AzureArtifactsPackageFile> children;
  private AzureArtifactsProtocolMetadata protocolMetadata;
}