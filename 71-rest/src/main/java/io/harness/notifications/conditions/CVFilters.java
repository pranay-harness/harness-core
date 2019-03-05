package io.harness.notifications.conditions;

import java.util.List;

public interface CVFilters {
  List<String> getAppIds();
  List<String> getEnvIds();
  List<String> getCVConfigurationIds();
}
