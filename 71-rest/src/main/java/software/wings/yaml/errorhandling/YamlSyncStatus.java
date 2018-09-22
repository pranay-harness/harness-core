package software.wings.yaml.errorhandling;

import lombok.Data;

/**
 * @author rktummala on 12/18/17
 */
@Data
public class YamlSyncStatus {
  private String yamlFilePath;
  private Status syncStatus;
  private String message;
  private String latestContent;

  private enum Status { SUCCESS, FAILURE }
}
