package software.wings.helpers.ext.azure.devops;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureArtifactsPackageFileInfo {
  private String name;
  private long size;

  public AzureArtifactsPackageFileInfo(String name, long size) {
    this.name = name;
    this.size = size;
  }
}