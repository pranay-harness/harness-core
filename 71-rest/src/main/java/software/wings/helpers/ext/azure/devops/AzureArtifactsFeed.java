package software.wings.helpers.ext.azure.devops;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureArtifactsFeed {
  private String id;
  private String name;
  private String fullyQualifiedName;
  private AzureDevopsProject project;
}