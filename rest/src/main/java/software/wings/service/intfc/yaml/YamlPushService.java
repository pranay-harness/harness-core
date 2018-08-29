package software.wings.service.intfc.yaml;

import software.wings.beans.Event.Type;

public interface YamlPushService {
  <T> void pushYamlChangeSet(
      String accountId, T oldEntity, T newEntity, Type type, boolean syncFromGit, boolean isRename);
}
