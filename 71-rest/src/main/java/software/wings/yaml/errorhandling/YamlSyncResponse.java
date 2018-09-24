package software.wings.yaml.errorhandling;

import lombok.Data;

import java.util.List;

/**
 * @author rktummala on 12/18/17
 */
@Data
public class YamlSyncResponse {
  private List<YamlSyncStatus> syncStatusList;
}
