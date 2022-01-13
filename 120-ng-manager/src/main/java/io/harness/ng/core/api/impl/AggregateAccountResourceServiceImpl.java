/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.api.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.connector.ConnectorModule.DEFAULT_CONNECTOR_SERVICE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.connector.services.ConnectorService;
import io.harness.ng.core.api.AggregateAccountResourceService;
import io.harness.ng.core.api.DelegateDetailsService;
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
  private final DelegateDetailsService delegateDetailsService;

  @Inject
  public AggregateAccountResourceServiceImpl(final NGSecretServiceV2 secretService,
      @Named(DEFAULT_CONNECTOR_SERVICE) final ConnectorService defaultConnectorService,
      final DelegateDetailsService delegateDetailsService) {
    this.secretServiceV2 = secretService;
    this.defaultConnectorService = defaultConnectorService;
    this.delegateDetailsService = delegateDetailsService;
  }

  @Override
  public AccountResourcesDTO getAccountResourcesDTO(String accountIdentifier) {
    final long secretsCount = secretServiceV2.count(accountIdentifier, null, null);
    final long connectorsCount = defaultConnectorService.count(accountIdentifier, null, null);
    final long delegatesCount = delegateDetailsService.getDelegateGroupCount(accountIdentifier, null, null);

    return AccountResourcesDTO.builder()
        .connectorsCount(connectorsCount)
        .secretsCount(secretsCount)
        .delegatesCount(delegatesCount)
        .build();
  }
}
