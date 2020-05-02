package software.wings.security.encryption.migration;

import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.FeatureName.CONNECTORS_REF_SECRETS;
import static software.wings.beans.FeatureName.CONNECTORS_REF_SECRETS_MIGRATION;
import static software.wings.beans.SettingAttribute.VALUE_TYPE_KEY;
import static software.wings.service.impl.SettingServiceHelper.ATTRIBUTES_USING_REFERENCES;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.dropwizard.lifecycle.Managed;
import org.mongodb.morphia.query.Query;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SettingAttribute.SettingAttributeKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FeatureFlagService;

import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

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
        executorService.scheduleWithFixedDelay(this ::run, 0, 10, TimeUnit.MINUTES);
  }

  @Override
  public void stop() throws InterruptedException {
    settingAttributeFeatureFlagCheckerFuture.cancel(true);
    executorService.shutdown();
    executorService.awaitTermination(2, TimeUnit.SECONDS);
  }

  @VisibleForTesting
  public void run() {
    boolean isMigrationGloballyEnabled = featureFlagService.isGlobalEnabled(CONNECTORS_REF_SECRETS_MIGRATION);
    Set<String> accountIds = featureFlagService.getAccountIds(CONNECTORS_REF_SECRETS_MIGRATION);

    if (!isMigrationGloballyEnabled && accountIds.isEmpty()) {
      return;
    }

    if (featureFlagService.isGlobalEnabled(CONNECTORS_REF_SECRETS)) {
      return;
    }

    if (isMigrationGloballyEnabled) {
      Query<SettingAttribute> query = wingsPersistence.createQuery(SettingAttribute.class)
                                          .field(SettingAttributeKeys.secretsMigrated)
                                          .notEqual(Boolean.TRUE)
                                          .field(VALUE_TYPE_KEY)
                                          .in(ATTRIBUTES_USING_REFERENCES);

      SettingAttribute settingAttribute = query.get();

      if (settingAttribute == null) {
        featureFlagService.enableGlobally(CONNECTORS_REF_SECRETS);
        return;
      }
    }

    for (String accountId : accountIds) {
      boolean isConnectorReferenceEnabledForAccount = featureFlagService.isEnabled(CONNECTORS_REF_SECRETS, accountId);
      if (!isConnectorReferenceEnabledForAccount) {
        Query<SettingAttribute> query = wingsPersistence.createQuery(SettingAttribute.class)
                                            .field(SettingAttributeKeys.secretsMigrated)
                                            .notEqual(Boolean.TRUE)
                                            .field(VALUE_TYPE_KEY)
                                            .in(ATTRIBUTES_USING_REFERENCES)
                                            .field(ACCOUNT_ID_KEY)
                                            .equal(accountId);
        SettingAttribute settingAttribute = query.get();
        if (settingAttribute == null) {
          featureFlagService.enableAccount(CONNECTORS_REF_SECRETS, accountId);
        }
      }
    }
  }
}
