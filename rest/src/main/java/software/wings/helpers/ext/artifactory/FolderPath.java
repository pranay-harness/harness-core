package software.wings.helpers.ext.artifactory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

/**
 * Created by sgurubelli on 10/3/17.
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class FolderPath {
  private String repo;
  private String path;
  private String uri;
  private boolean folder;
}
