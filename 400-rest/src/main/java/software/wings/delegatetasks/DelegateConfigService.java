package software.wings.delegatetasks;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.ConfigFile;

import java.io.IOException;
import java.util.List;

/**
 * Created by peeyushaggarwal on 1/9/17.
 */
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public interface DelegateConfigService {
  List<ConfigFile> getConfigFiles(String appId, String envId, String uuid, String hostId, String accountId)
      throws IOException;
}
