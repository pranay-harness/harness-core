package io.harness.ngpipeline.expressions.functors;

import io.harness.annotations.dev.ToBeDeleted;
import io.harness.data.structure.EmptyPredicate;
import io.harness.expression.LateBindingValue;
import io.harness.ng.core.services.OrganizationService;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;

@Deprecated
@ToBeDeleted
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
