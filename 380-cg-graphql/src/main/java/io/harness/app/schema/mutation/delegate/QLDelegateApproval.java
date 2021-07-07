package software.wings.graphql.schema.mutation.delegate;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.DelegateApproval;

import software.wings.graphql.schema.type.QLEnum;

@OwnedBy(PL)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
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
    return null;
  }
}
