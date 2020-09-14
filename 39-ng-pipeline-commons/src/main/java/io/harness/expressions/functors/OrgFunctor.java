package io.harness.expressions.functors;

import io.harness.ambiance.Ambiance;
import io.harness.common.AmbianceHelper;
import io.harness.data.structure.EmptyPredicate;
import io.harness.expression.LateBindingValue;
import io.harness.ng.core.services.OrganizationService;

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
    return EmptyPredicate.isEmpty(accountId) || EmptyPredicate.isEmpty(orgIdentifier)
        ? null
        : organizationService.get(accountId, orgIdentifier);
  }
}
