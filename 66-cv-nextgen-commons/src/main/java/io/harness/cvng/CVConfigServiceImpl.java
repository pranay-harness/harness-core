package io.harness.cvng;

import static java.util.stream.Collectors.toList;

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
  public CVConfig save(CVConfig cvConfig) {
    Preconditions.checkArgument(cvConfig.getUuid() == null, "UUID should be null when creating CVConfig");
    hPersistence.save(cvConfig);
    return cvConfig;
  }

  @Override
  public List<CVConfig> save(List<CVConfig> cvConfigs) {
    return cvConfigs.stream().map(this ::save).collect(Collectors.toList());
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

  @Override
  public List<String> getProductNames(String accountId, String connectorId) {
    Preconditions.checkNotNull(accountId, "accountId can not be null");
    Preconditions.checkNotNull(connectorId, "ConnectorId can not be null");
    return hPersistence.createQuery(CVConfig.class)
        .filter(CVConfigKeys.connectorId, connectorId)
        .filter(CVConfigKeys.accountId, accountId)
        .project(CVConfigKeys.productName, true)
        .asList()
        .stream()
        .map(cvConfig -> cvConfig.getProductName())
        .distinct()
        .sorted()
        .collect(toList());
  }
}
