package io.harness.governance.pipeline.service;

import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_ERROR;

import com.google.inject.Inject;

import io.harness.governance.pipeline.service.model.PipelineGovernanceConfig;
import io.harness.governance.pipeline.service.model.PipelineGovernanceConfig.PipelineGovernanceConfigKeys;
import io.harness.logging.AutoLogContext;
import io.harness.persistence.AccountLogContext;
import io.harness.persistence.HQuery;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.dl.WingsPersistence;
import software.wings.features.PipelineGovernanceFeature;
import software.wings.features.api.AccountId;
import software.wings.features.api.RestrictedApi;

import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;

@Slf4j
@ParametersAreNonnullByDefault
public class PipelineGovernanceServiceImpl implements PipelineGovernanceService {
  @Inject private WingsPersistence persistence;

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
      return persistence.delete(PipelineGovernanceConfig.class, uuid);
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
  public PipelineGovernanceConfig update(
      @AccountId final String accountId, final String uuid, final PipelineGovernanceConfig config) {
    logger.info("Updating pipeline governance standard  with uuid: {}", uuid);
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      Query<PipelineGovernanceConfig> query =
          persistence.createQuery(PipelineGovernanceConfig.class).field(PipelineGovernanceConfigKeys.uuid).equal(uuid);

      UpdateOperations<PipelineGovernanceConfig> updateOperations =
          persistence.createUpdateOperations(PipelineGovernanceConfig.class)
              .set(PipelineGovernanceConfigKeys.rules, config.getRules())
              .set(PipelineGovernanceConfigKeys.name, config.getName())
              .set(PipelineGovernanceConfigKeys.restrictions, config.getRestrictions())
              .set(PipelineGovernanceConfigKeys.description, config.getDescription());

      return persistence.findAndModify(query, updateOperations, WingsPersistence.upsertReturnNewOptions);
    }
  }

  @Override
  @RestrictedApi(PipelineGovernanceFeature.class)
  public PipelineGovernanceConfig add(@AccountId final String accountId, final PipelineGovernanceConfig config) {
    try (AutoLogContext ignore = new AccountLogContext(accountId, OVERRIDE_ERROR)) {
      logger.info("Adding pipeline governance standard");
      persistence.save(config);
      return config;
    }
  }
}
