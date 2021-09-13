package software.wings.helpers.ext.artifactory;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

/**
 * Created by sgurubelli on 10/3/17.
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@OwnedBy(CDC)
public class FolderPath {
  private String repo;
  private String path;
  private String uri;
  private boolean folder;
  private String node; // used in Nexus 3 private apis
}
