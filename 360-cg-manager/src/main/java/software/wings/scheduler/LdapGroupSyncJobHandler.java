/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.scheduler;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import static java.time.Duration.ofMinutes;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ff.FeatureFlagService;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.filter.MorphiaFilterExpander;
import io.harness.mongo.iterator.provider.MorphiaPersistenceProvider;
import io.harness.workers.background.AccountStatusBasedEntityProcessController;

import software.wings.app.MainConfiguration;
import software.wings.beans.sso.SSOSettings;
import software.wings.beans.sso.SSOSettings.SSOSettingsKeys;
import software.wings.beans.sso.SSOType;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.dl.WingsPersistence;
import software.wings.features.LdapFeature;
import software.wings.features.api.PremiumFeature;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AuthService;
import software.wings.service.intfc.SSOService;
import software.wings.service.intfc.SSOSettingService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;
import software.wings.service.intfc.security.SecretManager;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.security.SecureRandom;
import java.util.concurrent.ExecutorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;

@OwnedBy(PL)
@Slf4j
public class LdapGroupSyncJobHandler implements MongoPersistenceIterator.Handler<SSOSettings> {
  private static final SecureRandom random = new SecureRandom();
  private static final String SSO_PROVIDER_ID_KEY = "ssoId";

  public static final String GROUP = "LDAP_GROUP_SYNC_CRON_JOB";
  private static final int POLL_INTERVAL = 900; // Seconds

  public static final long MIN_LDAP_SYNC_TIMEOUT = 60 * 1000L; // 1 minute
  public static final long MAX_LDAP_SYNC_TIMEOUT = 3 * 60 * 1000L; // 3 minute

  @Inject private PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject private MongoTemplate mongoTemplate;
  @Inject private FeatureFlagService featureFlagService;
  @Inject private ExecutorService executorService;
  @Inject private SSOSettingService ssoSettingService;
  @Inject private SSOService ssoService;
  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private SecretManager secretManager;
  @Inject private UserService userService;
  @Inject private AuthService authService;

  @Inject private UserGroupService userGroupService;
  @Inject private AccountService accountService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject @Named(LdapFeature.FEATURE_NAME) private PremiumFeature ldapFeature;
  @Inject private MorphiaPersistenceProvider<SSOSettings> persistenceProvider;
  @Inject private LdapGroupSyncJobHelper ldapGroupSyncJobHelper;
  @Inject private MainConfiguration mainConfiguration;

  public void registerIterators() {
    LdapSyncJobConfig ldapSyncJobConfig = mainConfiguration.getLdapSyncJobConfig();
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name("LdapGroupSyncTask")
            .poolSize(ldapSyncJobConfig.getPoolSize())
            .interval(ofMinutes(ldapSyncJobConfig.getSyncInterval()))
            .build(),
        SSOSettings.class,
        MongoPersistenceIterator.<SSOSettings, MorphiaFilterExpander<SSOSettings>>builder()
            .clazz(SSOSettings.class)
            .fieldName(SSOSettingsKeys.nextIteration)
            .targetInterval(ofMinutes(60))
            .acceptableNoAlertDelay(ofMinutes(80))
            .handler(this)
            .entityProcessController(new AccountStatusBasedEntityProcessController<>(accountService))
            .filterExpander(query -> query.field(SSOSettingsKeys.type).equal(SSOType.LDAP))
            .schedulingType(REGULAR)
            .persistenceProvider(persistenceProvider)
            .redistribute(true));
  }

  @Override
  public void handle(SSOSettings ssoSettings) {
    ldapGroupSyncJobHelper.syncJob(ssoSettings);
  }
}
