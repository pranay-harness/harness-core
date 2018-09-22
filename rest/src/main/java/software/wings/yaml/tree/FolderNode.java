package software.wings.yaml.tree;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import software.wings.yaml.gitSync.YamlGitConfig.SyncMode;

import java.util.List;

/**
 * @author rktummala on 10/19/17
 */
@SuppressFBWarnings("UUF_UNUSED_FIELD")
public class FolderNode extends Node {
  private List<Node> children;
  private String yamlType;

  private SyncMode syncMode;
  private boolean syncEnabled;
}
