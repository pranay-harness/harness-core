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
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PL)
@Slf4j
public class AccessControlPreferenceServiceImpl implements AccessControlPreferenceService {
  private final ScheduledExecutorService executorService;
  private static final String ENFORCE_NG_ACCESS_CONTROL = "ENFORCE_NG_ACCESS_CONTROL";
  private final AccessControlPreferenceDAO accessControlPreferenceDAO;
  private volatile boolean globallyEnabled;
  private final FeatureFlagsClient featureFlagClient;

  @Inject
  public AccessControlPreferenceServiceImpl(AccessControlPreferenceDAO accessControlPreferenceDAO,
      @Named("PRIVILEGED") FeatureFlagsClient featureFlagClient) {
    this.accessControlPreferenceDAO = accessControlPreferenceDAO;
    this.featureFlagClient = featureFlagClient;
    executorService = Executors.newSingleThreadScheduledExecutor(
        new ThreadFactoryBuilder().setNameFormat("global-accesscontrol-enabled-feature-reconciliation").build());
    executorService.scheduleAtFixedRate(this::updateGlobalPreference, 1, 1, TimeUnit.MINUTES);
  }

  private void updateGlobalPreference() {
    log.info("Trying to sync the value of NG_ACCESS_CONTROL_ENFORCED flag");
    try {
      FeatureFlagDTO featureFlagDTO =
          RestClientUtils.getResponse(featureFlagClient.getFeatureFlagName(ENFORCE_NG_ACCESS_CONTROL));
      this.globallyEnabled = Optional.ofNullable(featureFlagDTO).map(FeatureFlagDTO::getEnabled).orElse(false);
      log.info("Successfully synced the value of NG_ACCESS_CONTROL_ENFORCED flag");
    } catch (Exception exception) {
      log.error("Failed to sync value of NG_ACCESS_CONTROL_ENFORCED flag with error", exception);
    }
  }

  @Override
  public boolean isAccessControlEnabled(String accountIdentifier) {
    Optional<AccessControlPreference> accessControlPreferenceOptional = get(accountIdentifier);
    return accessControlPreferenceOptional.map(AccessControlPreference::isAccessControlEnabled)
        .orElseGet(() -> globallyEnabled);
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
