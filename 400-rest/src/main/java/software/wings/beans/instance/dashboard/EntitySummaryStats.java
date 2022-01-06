/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.instance.dashboard;

/**
 * @author rktummala on 08/13/17
 */
public class EntitySummaryStats {
  private EntitySummary entitySummary;
  private long count;

  public EntitySummary getEntitySummary() {
    return entitySummary;
  }

  public void setEntitySummary(EntitySummary entitySummary) {
    this.entitySummary = entitySummary;
  }

  public long getCount() {
    return count;
  }

  public void setCount(long count) {
    this.count = count;
  }

  public static final class Builder {
    private EntitySummary entitySummary;
    private long count;

    private Builder() {}

    public static Builder anEntitySummaryStats() {
      return new Builder();
    }

    public Builder entitySummary(EntitySummary entitySummary) {
      this.entitySummary = entitySummary;
      return this;
    }

    public Builder count(long count) {
      this.count = count;
      return this;
    }

    public EntitySummaryStats build() {
      EntitySummaryStats entitySummaryStats = new EntitySummaryStats();
      entitySummaryStats.setEntitySummary(entitySummary);
      entitySummaryStats.setCount(count);
      return entitySummaryStats;
    }
  }
}
