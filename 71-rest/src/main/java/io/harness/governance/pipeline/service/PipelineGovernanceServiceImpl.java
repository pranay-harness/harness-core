package io.harness.governance.pipeline.service;

import com.google.inject.Inject;

import io.harness.governance.pipeline.service.model.PipelineGovernanceConfig;
import io.harness.governance.pipeline.service.model.PipelineGovernanceConfig.PipelineGovernanceConfigKeys;
import io.harness.persistence.HQuery;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.dl.WingsPersistence;
import software.wings.features.PipelineGovernanceFeature;
import software.wings.features.api.AccountId;
import software.wings.features.api.RestrictedApi;

import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;

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
  public boolean delete(final String uuid) {
    return persistence.delete(PipelineGovernanceConfig.class, uuid);
  }

  @Override
  public List<PipelineGovernanceConfig> list(final String accountId) {
    return persistence.createQuery(PipelineGovernanceConfig.class, HQuery.excludeCount)
        .field(PipelineGovernanceConfigKeys.accountId)
        .equal(accountId)
        .asList();
  }

  @Override
  @RestrictedApi(PipelineGovernanceFeature.class)
  public PipelineGovernanceConfig update(
      @AccountId final String accountId, final String uuid, final PipelineGovernanceConfig config) {
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

  @Override
  @RestrictedApi(PipelineGovernanceFeature.class)
  public PipelineGovernanceConfig add(@AccountId final String accountId, final PipelineGovernanceConfig config) {
    persistence.save(config);
    return config;
  }
}
