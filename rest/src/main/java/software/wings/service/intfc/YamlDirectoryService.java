package software.wings.service.intfc;

import org.hibernate.validator.constraints.NotEmpty;
import software.wings.yaml.directory.DirectoryNode;

/**
 * Yaml Directory Service.
 *
 * @author bsollish
 */
public interface YamlDirectoryService {
  /**
   * Find by account id.
   *
   * @param accountId the account id
   * @return the application
   */
  DirectoryNode get(@NotEmpty String accountId);
}
