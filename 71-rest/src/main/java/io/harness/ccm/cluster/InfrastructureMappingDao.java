package io.harness.ccm.cluster;

import static io.harness.persistence.HQuery.excludeValidate;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMapping.InfrastructureMappingKeys;

import java.util.List;

@Slf4j
@Singleton
public class InfrastructureMappingDao {
  private HPersistence persistence;

  @Inject
  public InfrastructureMappingDao(HPersistence persistence) {
    this.persistence = persistence;
  }

  public List<InfrastructureMapping> list(String cloudProviderId) {
    return persistence.createQuery(InfrastructureMapping.class, excludeValidate)
        .filter(InfrastructureMappingKeys.computeProviderSettingId, cloudProviderId)
        .asList();
  }
}
