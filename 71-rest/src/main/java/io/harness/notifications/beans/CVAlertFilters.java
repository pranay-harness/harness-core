package io.harness.notifications.beans;

import io.harness.data.structure.CollectionUtils;
import io.harness.notifications.conditions.CVFilters;
import lombok.Value;

import java.util.List;

@Value
public class CVAlertFilters implements CVFilters {
  private List<String> appIds;
  private List<String> envIds;
  private List<String> cvConfigurationIds;

  @Override
  public List<String> getAppIds() {
    return CollectionUtils.emptyIfNull(appIds);
  }

  @Override
  public List<String> getEnvIds() {
    return CollectionUtils.emptyIfNull(envIds);
  }

  @Override
  public List<String> getCVConfigurationIds() {
    return CollectionUtils.emptyIfNull(cvConfigurationIds);
  }
}
