package io.harness.notifications.beans;

import io.harness.data.structure.CollectionUtils;

import java.util.List;
import javax.annotation.Nonnull;
import lombok.Value;

@Value
public class ManualInterventionAlertFilters implements io.harness.notifications.conditions.ManualInterventionFilters {
  private List<String> appIds;
  private List<String> envIds;

  @Override
  @Nonnull
  public List<String> getAppIds() {
    return CollectionUtils.emptyIfNull(appIds);
  }

  @Override
  @Nonnull
  public List<String> getEnvIds() {
    return CollectionUtils.emptyIfNull(envIds);
  }
}
