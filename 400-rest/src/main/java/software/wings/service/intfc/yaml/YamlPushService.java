package software.wings.service.intfc.yaml;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.git.model.ChangeType;

import software.wings.beans.Event.Type;

import java.util.concurrent.Future;

@OwnedBy(HarnessTeam.DX)
@TargetModule(HarnessModule._870_CG_YAML)
public interface YamlPushService {
  <T> Future<?> pushYamlChangeSet(
      String accountId, T oldEntity, T newEntity, Type type, boolean syncFromGit, boolean isRename);

  <R, T> void pushYamlChangeSet(String accountId, R helperEntity, T entity, Type type, boolean syncFromGit);

  void pushYamlChangeSet(String accountId, String appId, ChangeType changeType, boolean syncFromGit);
}
