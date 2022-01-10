/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.instance.dashboard;

import software.wings.beans.infrastructure.instance.InvocationCount;

import java.util.List;

/**
 * @author rktummala on 08/13/17
 */
public class InstanceStats {
  private long totalCount;
  // TODO rename this to instanceSummaryList
  private List<EntitySummary> entitySummaryList;
  private InvocationCount invocationCount;

  public InvocationCount getInvocationCount() {
    return invocationCount;
  }

  public void setInvocationCount(InvocationCount invocationCount) {
    this.invocationCount = invocationCount;
  }

  public long getTotalCount() {
    return totalCount;
  }

  private void setTotalCount(long totalCount) {
    this.totalCount = totalCount;
  }

  public List<EntitySummary> getEntitySummaryList() {
    return entitySummaryList;
  }

  private void setEntitySummaryList(List<EntitySummary> entitySummaryList) {
    this.entitySummaryList = entitySummaryList;
  }

  public static final class Builder {
    private long totalCount;
    private List<EntitySummary> entitySummaryList;
    private InvocationCount invocationCount;

    private Builder() {}

    public static Builder anInstanceSummaryStats() {
      return new Builder();
    }

    public Builder withTotalCount(long totalCount) {
      this.totalCount = totalCount;
      return this;
    }

    public Builder withEntitySummaryList(List<EntitySummary> entitySummaryList) {
      this.entitySummaryList = entitySummaryList;
      return this;
    }

    public Builder withInvocationCount(InvocationCount invocationCount) {
      this.invocationCount = invocationCount;
      return this;
    }

    public Builder but() {
      return anInstanceSummaryStats().withTotalCount(totalCount).withEntitySummaryList(entitySummaryList);
    }

    public InstanceStats build() {
      InstanceStats instanceSummaryStats = new InstanceStats();
      instanceSummaryStats.setEntitySummaryList(entitySummaryList);
      instanceSummaryStats.setTotalCount(totalCount);
      instanceSummaryStats.setInvocationCount(invocationCount);
      return instanceSummaryStats;
    }
  }
}
