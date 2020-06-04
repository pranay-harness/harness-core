package software.wings.beans.approval;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import java.util.List;

@OwnedBy(CDC)
public enum ConditionalOperator {
  AND {
    @Override
    public boolean applyOperator(List<Boolean> values) {
      return !values.contains(Boolean.FALSE);
    }
  },
  OR {
    @Override
    public boolean applyOperator(List<Boolean> values) {
      return values.contains(Boolean.TRUE);
    }
  };
  public abstract boolean applyOperator(List<Boolean> values);
}
