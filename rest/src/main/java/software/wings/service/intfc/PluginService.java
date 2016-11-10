package software.wings.service.intfc;

import com.fasterxml.jackson.databind.JsonNode;
import software.wings.beans.AccountPlugin;

import java.util.List;
import java.util.Map;

/**
 * Created by peeyushaggarwal on 10/20/16.
 */
public interface PluginService {
  List<AccountPlugin> getInstalledPlugins(String accountId);

  Map<String, JsonNode> getPluginSettingSchema(String accountId);
}
