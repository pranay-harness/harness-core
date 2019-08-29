package io.harness.governance.pipeline.service;

import com.google.inject.Inject;

import io.harness.governance.pipeline.model.PipelineGovernanceConfig;
import io.harness.governance.pipeline.model.PipelineGovernanceConfig.PipelineGovernanceConfigKeys;
import io.harness.persistence.HQuery;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.dl.WingsPersistence;

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
  public PipelineGovernanceConfig update(final String uuid, final PipelineGovernanceConfig config) {
    Query<PipelineGovernanceConfig> query =
        persistence.createQuery(PipelineGovernanceConfig.class).field(PipelineGovernanceConfigKeys.uuid).equal(uuid);

    UpdateOperations<PipelineGovernanceConfig> updateOperations =
        persistence.createUpdateOperations(PipelineGovernanceConfig.class)
            .set(PipelineGovernanceConfigKeys.rules, config.getRules())
            .set(PipelineGovernanceConfigKeys.name, config.getName())
            .set(PipelineGovernanceConfigKeys.description, config.getDescription());

    return persistence.findAndModify(query, updateOperations, WingsPersistence.upsertReturnNewOptions);
  }

  @Override
  public PipelineGovernanceConfig add(final PipelineGovernanceConfig config) {
    persistence.save(config);
    return config;
  }
}
