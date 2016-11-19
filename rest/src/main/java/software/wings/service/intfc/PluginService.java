package software.wings.service.intfc;

import software.wings.beans.AccountPlugin;

import java.util.List;
import java.util.Map;

/**
 * Created by peeyushaggarwal on 10/20/16.
 */
public interface PluginService {
  List<AccountPlugin> getInstalledPlugins(String accountId);

  Map<String, Map<String, Object>> getPluginSettingSchema(String accountId);
}
