package io.harness.governance.pipeline.service;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import com.google.inject.Inject;

import io.harness.governance.pipeline.service.model.PipelineGovernanceConfig;
import io.harness.governance.pipeline.service.model.PipelineGovernanceConfig.PipelineGovernanceConfigKeys;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.AccountLogContext;
import io.harness.persistence.HQuery;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.Event;
import software.wings.dl.WingsPersistence;
import software.wings.features.PipelineGovernanceFeature;
import software.wings.features.api.AccountId;
import software.wings.features.api.RestrictedApi;
import software.wings.service.impl.AuditServiceHelper;

import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;

@Slf4j
@ParametersAreNonnullByDefault
public class PipelineGovernanceServiceImpl implements PipelineGovernanceService {
  @Inject private WingsPersistence persistence;
  @Inject private AuditServiceHelper auditServiceHelper;

  @Override
  public PipelineGovernanceConfig get(final String uuid) {
    return persistence.createQuery(PipelineGovernanceConfig.class)
        .field(PipelineGovernanceConfigKeys.uuid)
        .equal(uuid)
        .get();
  }

  @Override
  public boolean delete(final String accountId, final String uuid) {
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      logger.info("Deleting pipeline governance standard  with uuid: {}", uuid);
      PipelineGovernanceConfig config = get(uuid);
      boolean deleted = persistence.delete(PipelineGovernanceConfig.class, uuid);
      if (deleted) {
        auditServiceHelper.reportDeleteForAuditingUsingAccountId(accountId, config);
      }
      return deleted;
    }
  }

  @Override
  public List<PipelineGovernanceConfig> list(final String accountId) {
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      logger.info("Fetching all pipeline governance standards");
      return persistence.createQuery(PipelineGovernanceConfig.class, HQuery.excludeCount)
          .field(PipelineGovernanceConfigKeys.accountId)
          .equal(accountId)
          .asList();
    }
  }

  @Override
  @RestrictedApi(PipelineGovernanceFeature.class)
  public PipelineGovernanceConfig add(@AccountId final String accountId, final PipelineGovernanceConfig config) {
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      logger.info("Adding pipeline governance standard");
      PipelineGovernanceConfig existingConfig = get(config.getUuid());
      persistence.save(config);
      if (existingConfig == null) {
        auditServiceHelper.reportForAuditingUsingAccountId(accountId, null, config, Event.Type.CREATE);
      } else {
        auditServiceHelper.reportForAuditingUsingAccountId(accountId, existingConfig, config, Event.Type.UPDATE);
      }
      return config;
    }
  }
}
