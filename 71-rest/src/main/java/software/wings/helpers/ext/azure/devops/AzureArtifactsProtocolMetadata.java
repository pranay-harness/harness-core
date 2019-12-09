package software.wings.helpers.ext.azure.devops;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class AzureArtifactsProtocolMetadata {
  private AzureArtifactsProtocolMetadataData data;
}