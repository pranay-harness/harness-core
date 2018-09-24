package software.wings.beans;

import com.github.zafarkhaja.semver.Version;
import software.wings.settings.SettingValue;

import java.util.List;

/**
 * Created by peeyushaggarwal on 10/20/16.
 */
public interface WingsPlugin {
  String getType();
  Class<? extends SettingValue> getSettingClass();
  List<PluginCategory> getPluginCategories();
  boolean isEnabled();
  Version getVersion();
}
