package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.services.ConnectorService;
import io.harness.ng.core.api.AggregateAccountResourceService;
import io.harness.ng.core.api.NGSecretServiceV2;
import io.harness.ng.core.dto.AccountResourcesDTO;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

@OwnedBy(PL)
@Singleton
public class AggregateAccountResourceServiceImpl implements AggregateAccountResourceService {
  private final NGSecretServiceV2 secretServiceV2;
  private final ConnectorService defaultConnectorService;

  @Inject
  public AggregateAccountResourceServiceImpl(
      NGSecretServiceV2 secretService, @Named(DEFAULT_CONNECTOR_SERVICE) ConnectorService defaultConnectorService) {
    this.secretServiceV2 = secretService;
    this.defaultConnectorService = defaultConnectorService;
  }
  @Override
  public AccountResourcesDTO getAccountResourcesDTO(String accountIdentifier) {
    long secretsCount = secretServiceV2.count(accountIdentifier, null, null);
    long connectorsCount = defaultConnectorService.count(accountIdentifier, null, null);

    return AccountResourcesDTO.builder().connectorsCount(connectorsCount).secretsCount(secretsCount).build();
  }
}
