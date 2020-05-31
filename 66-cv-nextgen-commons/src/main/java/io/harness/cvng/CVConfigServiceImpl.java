package io.harness.cvng;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import io.harness.cvng.core.services.entities.CVConfig;
import io.harness.cvng.core.services.entities.CVConfig.CVConfigKeys;
import io.harness.persistence.HPersistence;
import org.mongodb.morphia.query.Query;

import java.util.List;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

public class CVConfigServiceImpl implements CVConfigService {
  @Inject HPersistence hPersistence;
  @Override
  public CVConfig save(@NotNull String accountId, CVConfig cvConfig) {
    Preconditions.checkArgument(cvConfig.getUuid() == null, "UUID should be null when creating CVConfig");
    hPersistence.save(cvConfig);
    return cvConfig;
  }

  @Override
  public List<CVConfig> save(String accountId, List<CVConfig> cvConfigs) {
    return cvConfigs.stream().map(cvConfig -> save(accountId, cvConfig)).collect(Collectors.toList());
  }

  @Nullable
  @Override
  public CVConfig get(@NotNull String cvConfigId) {
    return hPersistence.get(CVConfig.class, cvConfigId);
  }

  @Override
  public void update(CVConfig cvConfig) {
    Preconditions.checkArgument(cvConfig.getUuid() != null, "Trying to update a CVConfig with empty UUID.");
    hPersistence.save(cvConfig);
  }

  @Override
  public void update(List<CVConfig> cvConfigs) {
    Preconditions.checkArgument(cvConfigs.stream().allMatch(cvConfig -> cvConfig.getUuid() != null),
        "Trying to update a CVConfig with empty UUID.");
    cvConfigs.forEach(this ::update); // TODO: implement batch update
  }

  @Override
  public void delete(@NotNull String cvConfigId) {
    hPersistence.delete(CVConfig.class, cvConfigId);
  }

  @Override
  public void delete(List<String> cvConfigIds) {
    cvConfigIds.forEach(this ::delete); // TODO: implement batch delete
  }

  @Override
  public List<CVConfig> list(@NotNull String accountId, String connectorId) {
    Query<CVConfig> query = hPersistence.createQuery(CVConfig.class)
                                .filter(CVConfigKeys.accountId, accountId)
                                .filter(CVConfigKeys.connectorId, connectorId);
    return query.asList();
  }
}
