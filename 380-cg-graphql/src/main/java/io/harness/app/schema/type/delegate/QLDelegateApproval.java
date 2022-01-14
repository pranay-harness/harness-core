package io.harness.app.schema.type.delegate;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.DelegateApproval;
import io.harness.exception.InvalidRequestException;

import software.wings.graphql.schema.type.QLEnum;

@OwnedBy(DEL)
public enum QLDelegateApproval implements QLEnum {
  ACTIVATE,
  REJECT;

  @Override
  public String getStringValue() {
    return this.name();
  }

  public static DelegateApproval toDelegateApproval(QLDelegateApproval qlDelegateApproval) {
    if (qlDelegateApproval == ACTIVATE) {
      return DelegateApproval.ACTIVATE;
    }
    if (qlDelegateApproval == REJECT) {
      return DelegateApproval.REJECT;
    }
    throw new InvalidRequestException("Invalid delegate approval value ");
  }
}
