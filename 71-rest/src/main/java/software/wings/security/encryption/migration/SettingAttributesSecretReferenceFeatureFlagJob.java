package software.wings.security.encryption.migration;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.beans.FeatureName.CONNECTORS_REF_SECRETS;

import static software.wings.beans.SettingAttribute.VALUE_TYPE_KEY;
import static software.wings.service.impl.SettingServiceHelper.ATTRIBUTES_USING_REFERENCES;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ff.FeatureFlagService;

import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.dl.WingsPersistence;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.dropwizard.lifecycle.Managed;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.mongodb.morphia.query.Query;

@OwnedBy(PL)
@Singleton
public class SettingAttributesSecretReferenceFeatureFlagJob implements Managed {
  @Inject private FeatureFlagService featureFlagService;
  @Inject private WingsPersistence wingsPersistence;
  private static final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor(
      new ThreadFactoryBuilder().setNameFormat("setting-attributes-feature-flag-checker").build());
  private Future settingAttributeFeatureFlagCheckerFuture;

  @Override
  public void start() {
    settingAttributeFeatureFlagCheckerFuture =
        executorService.scheduleWithFixedDelay(this::run, 0, 10, TimeUnit.MINUTES);
  }

  @Override
  public void stop() throws InterruptedException {
    settingAttributeFeatureFlagCheckerFuture.cancel(true);
    executorService.shutdown();
    executorService.awaitTermination(2, TimeUnit.SECONDS);
  }

  @VisibleForTesting
  public void run() {
    if (featureFlagService.isGlobalEnabled(CONNECTORS_REF_SECRETS)) {
      return;
    }

    Query<SettingAttribute> query = wingsPersistence.createQuery(SettingAttribute.class)
                                        .field(SettingAttributeKeys.secretsMigrated)
                                        .notEqual(Boolean.TRUE)
                                        .field(VALUE_TYPE_KEY)
                                        .in(ATTRIBUTES_USING_REFERENCES);

    SettingAttribute settingAttribute = query.get();

    if (settingAttribute == null) {
      featureFlagService.enableGlobally(CONNECTORS_REF_SECRETS);
    }
  }
}
