package io.harness.ccm.cluster;

import static io.harness.persistence.HQuery.excludeValidate;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import software.wings.infra.InfrastructureDefinition;
import software.wings.infra.InfrastructureDefinition.InfrastructureDefinitionKeys;

import java.util.List;

@Slf4j
@Singleton
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
