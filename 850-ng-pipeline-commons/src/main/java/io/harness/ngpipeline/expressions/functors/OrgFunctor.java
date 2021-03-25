package io.harness.ngpipeline.expressions.functors;

import static io.harness.data.structure.HasPredicate.hasNone;

import io.harness.expression.LateBindingValue;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;

public class OrgFunctor implements LateBindingValue {
  private final OrganizationService organizationService;
  private final Ambiance ambiance;

  public OrgFunctor(OrganizationService organizationService, Ambiance ambiance) {
    this.organizationService = organizationService;
    this.ambiance = ambiance;
  }

  @Override
  public Object bind() {
    String accountId = AmbianceHelper.getAccountId(ambiance);
    String orgIdentifier = AmbianceHelper.getOrgIdentifier(ambiance);
    return hasNone(accountId) || hasNone(orgIdentifier) ? null : organizationService.get(accountId, orgIdentifier);
  }
}
