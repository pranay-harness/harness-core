package io.harness.cvng.core.jobs;

import io.harness.cvng.activity.entities.Activity;
import io.harness.cvng.activity.entities.ActivitySource;
import io.harness.cvng.alert.entities.AlertRule;
import io.harness.cvng.alert.entities.AlertRuleAnomaly;
import io.harness.cvng.cdng.entities.CVNGStepTask;
import io.harness.cvng.core.entities.CVConfig;
import io.harness.cvng.core.entities.MetricPack;
import io.harness.cvng.core.entities.MonitoredService;
import io.harness.cvng.core.entities.MonitoringSourcePerpetualTask;
import io.harness.cvng.core.entities.ServiceDependency;
import io.harness.cvng.core.entities.TimeSeriesThreshold;
import io.harness.cvng.core.entities.Webhook;
import io.harness.cvng.core.entities.changeSource.ChangeSource;
import io.harness.cvng.core.services.api.CVConfigService;
import io.harness.cvng.core.services.api.DeleteEntityByHandler;
import io.harness.cvng.core.services.api.MonitoringSourcePerpetualTaskService;
import io.harness.cvng.core.services.api.monitoredService.MonitoredServiceService;
import io.harness.cvng.dashboard.entities.HeatMap;
import io.harness.cvng.servicelevelobjective.entities.SLOHealthIndicator;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelIndicator;
import io.harness.cvng.servicelevelobjective.entities.ServiceLevelObjective;
import io.harness.cvng.servicelevelobjective.entities.UserJourney;
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
    // Add the service for project level default deletion
    final List<Class<? extends PersistentEntity>> deleteEntitiesWithDefaultHandler =
        Arrays.asList(VerificationJob.class, Activity.class, ActivitySource.class, AlertRule.class, MetricPack.class,
            HeatMap.class, TimeSeriesThreshold.class, AlertRuleAnomaly.class, CVNGStepTask.class,
            ServiceLevelObjective.class, UserJourney.class, ServiceLevelIndicator.class, ChangeSource.class,
            Webhook.class, ServiceDependency.class, SLOHealthIndicator.class);
    ENTITIES_MAP = new HashMap<>();
    deleteEntitiesWithDefaultHandler.forEach(entity -> ENTITIES_MAP.put(entity, DeleteEntityByHandler.class));

    // Add the service for project level custom deletion
    ENTITIES_MAP.put(CVConfig.class, CVConfigService.class);
    ENTITIES_MAP.put(MonitoringSourcePerpetualTask.class, MonitoringSourcePerpetualTaskService.class);
    ENTITIES_MAP.put(MonitoredService.class, MonitoredServiceService.class);
  }
}
