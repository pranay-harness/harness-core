package software.wings.core.managerConfiguration;

import java.util.List;

public interface ConfigChangeListener { void onConfigChange(List<ConfigChangeEvent> events); }
