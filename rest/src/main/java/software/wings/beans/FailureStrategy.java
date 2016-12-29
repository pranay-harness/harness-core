package software.wings.beans;

import java.util.ArrayList;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * Created by rishi on 10/31/16.
 */
public class FailureStrategy {
  @NotNull @Size(min = 1) private List<FailureType> failureTypes = new ArrayList<>();
  @NotNull @Size(min = 1) private List<RepairAction> repairActions = new ArrayList<>();

  public List<FailureType> getFailureTypes() {
    return failureTypes;
  }

  public void setFailureTypes(List<FailureType> failureTypes) {
    this.failureTypes = failureTypes;
  }

  public List<RepairAction> getRepairActions() {
    return repairActions;
  }

  public void setRepairActions(List<RepairAction> repairActions) {
    this.repairActions = repairActions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    FailureStrategy that = (FailureStrategy) o;

    if (failureTypes != null ? !failureTypes.equals(that.failureTypes) : that.failureTypes != null)
      return false;
    return repairActions != null ? repairActions.equals(that.repairActions) : that.repairActions == null;
  }

  @Override
  public int hashCode() {
    int result = failureTypes != null ? failureTypes.hashCode() : 0;
    result = 31 * result + (repairActions != null ? repairActions.hashCode() : 0);
    return result;
  }

  @Override
  public String toString() {
    return "FailureStrategy{"
        + "failureTypes=" + failureTypes + ", repairActions=" + repairActions + '}';
  }

  public static final class FailureStrategyBuilder {
    private List<FailureType> failureTypes = new ArrayList<>();
    private List<RepairAction> repairActions = new ArrayList<>();

    private FailureStrategyBuilder() {}

    public static FailureStrategyBuilder aFailureStrategy() {
      return new FailureStrategyBuilder();
    }

    public FailureStrategyBuilder addFailureTypes(FailureType failureType) {
      this.failureTypes.add(failureType);
      return this;
    }

    public FailureStrategyBuilder addRepairActions(RepairAction repairAction) {
      this.repairActions.add(repairAction);
      return this;
    }

    public FailureStrategy build() {
      FailureStrategy failureStrategy = new FailureStrategy();
      failureStrategy.setFailureTypes(failureTypes);
      failureStrategy.setRepairActions(repairActions);
      return failureStrategy;
    }
  }
}
