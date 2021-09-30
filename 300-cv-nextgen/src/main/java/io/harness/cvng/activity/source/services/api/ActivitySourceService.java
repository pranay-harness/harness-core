package io.harness.cvng.activity.source.services.api;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cvng.activity.entities.ActivitySource;
import io.harness.cvng.core.services.api.DeleteEntityByHandler;

import javax.validation.constraints.NotNull;
@OwnedBy(HarnessTeam.CV)
public interface ActivitySourceService extends DeleteEntityByHandler<ActivitySource> {
  ActivitySource getActivitySource(@NotNull String activitySourceId);
}
