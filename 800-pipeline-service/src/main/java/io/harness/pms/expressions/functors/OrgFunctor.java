/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.expressions.functors;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.EngineFunctorException;
import io.harness.expression.LateBindingValue;
import io.harness.network.SafeHttpCall;
import io.harness.ng.core.dto.OrganizationResponse;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.organization.remote.OrganizationClient;
import io.harness.pms.contracts.ambiance.Ambiance;

import java.util.Optional;

@OwnedBy(PIPELINE)
public class OrgFunctor implements LateBindingValue {
  private final OrganizationClient organizationClient;
  private final Ambiance ambiance;

  public OrgFunctor(OrganizationClient organizationClient, Ambiance ambiance) {
    this.organizationClient = organizationClient;
    this.ambiance = ambiance;
  }

  @Override
  public Object bind() {
    String accountId = AmbianceHelper.getAccountId(ambiance);
    String orgIdentifier = AmbianceHelper.getOrgIdentifier(ambiance);
    if (EmptyPredicate.isEmpty(accountId) || EmptyPredicate.isEmpty(orgIdentifier)) {
      return null;
    }

    try {
      Optional<OrganizationResponse> resp =
          SafeHttpCall.execute(organizationClient.getOrganization(orgIdentifier, accountId)).getData();
      return resp.map(OrganizationResponse::getOrganization).orElse(null);
    } catch (Exception ex) {
      throw new EngineFunctorException(String.format("Invalid organization: %s", orgIdentifier), ex);
    }
  }
}
