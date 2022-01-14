/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.instance.dashboard;

import java.util.List;
import java.util.Map;

/**
 * @author rktummala on 08/13/17
 */
public class InstanceSummaryStats {
  private long totalCount;
  /**
   * Key - groupByEntityType, Value - List&lt;EntitySummaryStats&gt;
   */
  private Map<String, List<EntitySummaryStats>> countMap;

  public long getTotalCount() {
    return totalCount;
  }

  public void setTotalCount(long totalCount) {
    this.totalCount = totalCount;
  }

  public Map<String, List<EntitySummaryStats>> getCountMap() {
    return countMap;
  }

  public void setCountMap(Map<String, List<EntitySummaryStats>> countMap) {
    this.countMap = countMap;
  }

  public static final class Builder {
    private long totalCount;
    private Map<String, List<EntitySummaryStats>> countMap;

    private Builder() {}

    public static Builder anInstanceSummaryStats() {
      return new Builder();
    }

    public Builder totalCount(long totalCount) {
      this.totalCount = totalCount;
      return this;
    }

    public Builder countMap(Map<String, List<EntitySummaryStats>> countMap) {
      this.countMap = countMap;
      return this;
    }

    public InstanceSummaryStats build() {
      InstanceSummaryStats instanceSummaryStats = new InstanceSummaryStats();
      instanceSummaryStats.setTotalCount(totalCount);
      instanceSummaryStats.setCountMap(countMap);
      return instanceSummaryStats;
    }
  }
}
