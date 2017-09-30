package software.wings.service.intfc.yaml;

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
   * @return the directory node (the full account setup/"config-as-code" directory)
   */
  public DirectoryNode getDirectory(@NotEmpty String accountId);

  /**
   * Get Directory (tree/sub-tree structure) by entityId, optionally filtered by nodes ("branches") that have custom git
   * sync
   *
   * @param accountId the account id
   * @param entityId the entity id
   * @param filterCustomGitSync flag for whether it or not it should filter/"prune" custom git sync "branches"
   * @return the directory node (top of the requested "tree")
   */
  public DirectoryNode getDirectory(@NotEmpty String accountId, String entityId, boolean filterCustomGitSync);

  public DirectoryNode pushDirectory(@NotEmpty String accountId, boolean filterCustomGitSync);
}
