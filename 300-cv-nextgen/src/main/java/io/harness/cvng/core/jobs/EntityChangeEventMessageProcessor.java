package io.harness.cvng.core.jobs;

import io.harness.cvng.activity.entities.Activity;
import io.harness.cvng.activity.entities.ActivitySource;
import io.harness.cvng.activity.source.services.api.ActivitySourceService;
import io.harness.cvng.alert.entities.AlertRule;
import io.harness.cvng.alert.entities.AlertRuleAnomaly;
import io.harness.cvng.cd10.entities.CD10Mapping;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.entities.TimeSeriesThreshold;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.DeleteEntityByHandler;
import io.harness.cvng.dashboard.entities.HeatMap;
import io.harness.cvng.verificationjob.entities.VerificationJob;
import io.harness.persistence.PersistentEntity;

import com.google.common.annotations.VisibleForTesting;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class EntityChangeEventMessageProcessor implements ConsumerMessageProcessor {
  @VisibleForTesting
  static final Map<Class<? extends PersistentEntity>, Class<? extends DeleteEntityByHandler>> ENTITIES_MAP;

  static {
    ENTITIES_MAP = new HashMap<>();
    ENTITIES_MAP.put(CVConfig.class, CVConfigService.class);
    ENTITIES_MAP.put(ActivitySource.class, ActivitySourceService.class);
    final List<Class<? extends PersistentEntity>> deleteEntitiesWithDefaultHandler =
        Arrays.asList(VerificationJob.class, Activity.class, AlertRule.class, CD10Mapping.class, MetricPack.class,
            HeatMap.class, TimeSeriesThreshold.class, AlertRuleAnomaly.class);
    deleteEntitiesWithDefaultHandler.forEach(entity -> ENTITIES_MAP.put(entity, DeleteEntityByHandler.class));
  }
}
