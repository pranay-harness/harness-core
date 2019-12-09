package software.wings.helpers.ext.azure.devops;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
class AzureArtifactsPackageVersions {
  private int count;
  private List<AzureArtifactsPackageVersion> value;
}