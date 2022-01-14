package io.harness.ccm.cluster;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.persistence.HQuery.excludeValidate;

import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.HPersistence;

import software.wings.infra.InfrastructureDefinition;
import software.wings.infra.InfrastructureDefinition.InfrastructureDefinitionKeys;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(CE)
public class InfrastructureDefinitionDao {
  static final String cloudProviderField = InfrastructureDefinitionKeys.infrastructure + "."
      + "cloudProviderId";

  private HPersistence persistence;

  @Inject
  public InfrastructureDefinitionDao(HPersistence persistence) {
    this.persistence = persistence;
  }

  public List<InfrastructureDefinition> list(String cloudProviderId) {
    return persistence.createQuery(InfrastructureDefinition.class, excludeValidate)
        .filter(cloudProviderField, cloudProviderId)
        .asList();
  }
}
