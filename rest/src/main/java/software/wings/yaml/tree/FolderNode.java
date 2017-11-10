package software.wings.yaml.tree;

import software.wings.yaml.gitSync.YamlGitConfig.SyncMode;

import java.util.List;

/**
 * @author rktummala on 10/19/17
 */
public class FolderNode extends Node {
  private List<Node> children;
  private String yamlType;

  private SyncMode syncMode;
  private boolean syncEnabled;
}
