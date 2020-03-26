package software.wings.service.impl.yaml;

import lombok.Data;

@Data
public class GitToHarnessErrorCommitStats {
  String _id;
  Integer failedCount;
  Long createdAt;
}
