package io.harness.accesscontrol.preference.services;

import io.harness.accesscontrol.preference.persistence.daos.AccessControlPreferenceDAO;
import io.harness.accesscontrol.preference.persistence.models.AccessControlPreference;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ff.FeatureFlagDTO;
import io.harness.ff.FeatureFlagsClient;
import io.harness.remote.client.RestClientUtils;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PL)
@Slf4j
public class AccessControlPreferenceServiceImpl implements AccessControlPreferenceService {
  private final ScheduledExecutorService executorService;
  private static final String ENFORCE_NG_ACCESS_CONTROL_FF = "ENFORCE_NG_ACCESS_CONTROL";
  private final AccessControlPreferenceDAO accessControlPreferenceDAO;
  private final AtomicBoolean isEnabledForAllAccounts = new AtomicBoolean();
  private final FeatureFlagsClient featureFlagClient;

  @Inject
  public AccessControlPreferenceServiceImpl(AccessControlPreferenceDAO accessControlPreferenceDAO,
      @Named("PRIVILEGED") FeatureFlagsClient featureFlagClient) {
    this.accessControlPreferenceDAO = accessControlPreferenceDAO;
    this.featureFlagClient = featureFlagClient;
    executorService = Executors.newSingleThreadScheduledExecutor(
        new ThreadFactoryBuilder().setNameFormat("global-accesscontrol-enabled-feature-reconciliation").build());
    executorService.scheduleAtFixedRate(this::updateGlobalPreference, 0, 1, TimeUnit.MINUTES);
  }

  private void updateGlobalPreference() {
    try {
      FeatureFlagDTO featureFlagDTO =
          RestClientUtils.getResponse(featureFlagClient.getFeatureFlagName(ENFORCE_NG_ACCESS_CONTROL_FF));
      boolean isEnabled = Optional.ofNullable(featureFlagDTO).map(FeatureFlagDTO::getEnabled).orElse(false);
      if (isEnabledForAllAccounts.get() != isEnabled) {
        isEnabledForAllAccounts.set(isEnabled);
        log.info("Successfully synced the value of ENFORCE_NG_ACCESS_CONTROL_FF flag to : {}", isEnabled);
      }
    } catch (Exception exception) {
      log.error("Failed to sync ENFORCE_NG_ACCESS_CONTROL_FF feature flag", exception);
    }
  }

  @Override
  public boolean isAccessControlEnabled(String accountIdentifier) {
    Optional<AccessControlPreference> accessControlPreferenceOptional = get(accountIdentifier);
    return accessControlPreferenceOptional.map(AccessControlPreference::isAccessControlEnabled)
        .orElseGet(() -> isEnabledForAllAccounts.get());
  }

  private Optional<AccessControlPreference> get(String accountIdentifier) {
    return accessControlPreferenceDAO.getByAccountId(accountIdentifier);
  }

  @Override
  public boolean upsertAccessControlEnabled(String accountIdentifier, boolean enabled) {
    Optional<AccessControlPreference> aclPreferenceOptional = get(accountIdentifier);
    AccessControlPreference accessControlPreference =
        aclPreferenceOptional.orElseGet(() -> AccessControlPreference.builder().accountId(accountIdentifier).build());
    accessControlPreference.setAccessControlEnabled(enabled);
    return accessControlPreferenceDAO.save(accessControlPreference) != null;
  }
}
