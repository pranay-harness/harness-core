package io.harness.notifications.conditions;

import java.util.List;

public interface ManualInterventionFilters {
  List<String> getAppIds();
  List<String> getEnvIds();
}
