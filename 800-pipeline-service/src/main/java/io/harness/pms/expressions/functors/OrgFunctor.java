package io.harness.pms.expressions.functors;

import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.FunctorException;
import io.harness.expression.LateBindingValue;
import io.harness.network.SafeHttpCall;
import io.harness.ng.core.dto.OrganizationResponse;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.organizationmanagerclient.remote.OrganizationManagerClient;
import io.harness.pms.contracts.ambiance.Ambiance;

import java.util.Optional;

public class OrgFunctor implements LateBindingValue {
  private final OrganizationManagerClient organizationManagerClient;
  private final Ambiance ambiance;

  public OrgFunctor(OrganizationManagerClient organizationManagerClient, Ambiance ambiance) {
    this.organizationManagerClient = organizationManagerClient;
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
          SafeHttpCall.execute(organizationManagerClient.getOrganization(orgIdentifier, accountId)).getData();
      return resp.map(OrganizationResponse::getOrganization).orElse(null);
    } catch (Exception ex) {
      throw new FunctorException(String.format("Invalid organization: %s", orgIdentifier), ex);
    }
  }
}
