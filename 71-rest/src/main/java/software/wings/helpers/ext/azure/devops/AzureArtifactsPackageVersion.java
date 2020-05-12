package software.wings.helpers.ext.azure.devops;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.annotations.dev.OwnedBy;
import lombok.Data;

import java.util.List;

@OwnedBy(CDC)
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class AzureArtifactsPackageVersion {
  private String id;
  private String version;
  private String publishDate;
  private List<AzureArtifactsPackageFile> files;
}