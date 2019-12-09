package software.wings.helpers.ext.azure.devops;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureArtifactsPackageVersion {
  private String id;
  private String version;
  private String publishDate;
  private List<AzureArtifactsPackageFile> files;
}