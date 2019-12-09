package software.wings.helpers.ext.azure.devops;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class AzureArtifactsPackageFile {
  private String name;
  private List<AzureArtifactsPackageFile> children;
  private AzureArtifactsProtocolMetadata protocolMetadata;
}