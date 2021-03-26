package io.harness.gitsync.common.service;

import io.harness.EntityType;
import io.harness.common.EntityReference;
import io.harness.gitsync.PushInfo;
import io.harness.gitsync.common.beans.InfoForGitPush;

public interface HarnessToGitHelperService {
  InfoForGitPush getInfoForPush(String yamlGitConfigId, String branch, String filePath, String accountId,
      EntityReference entityReference, EntityType entityType);

  void postPushOperation(PushInfo pushInfo);
}
