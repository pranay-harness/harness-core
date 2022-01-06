/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.verification.log;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static software.wings.common.VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES;

import software.wings.service.impl.analysis.FeedbackPriority;
import software.wings.stencils.DefaultValue;
import software.wings.verification.CVConfiguration;
import software.wings.verification.CVConfigurationYaml;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.github.reinert.jjschema.Attributes;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;

@Data
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "LogsCVConfigurationKeys")
public class LogsCVConfiguration extends CVConfiguration {
  @Attributes(title = "Search Keywords", required = true) @DefaultValue("*exception*") protected String query;

  private long baselineStartMinute = -1;
  private long baselineEndMinute = -1;
  private boolean is247LogsV2;
  private FeedbackPriority alertPriority = FeedbackPriority.P5;

  public void setQuery(String query) {
    this.query = isNotEmpty(query) ? query.trim() : query;
  }

  /**
   * Sets the floor value for Baseline Start Minute
   * @param baselineStartMinute
   */
  public void setBaselineStartMinute(long baselineStartMinute) {
    if (Math.floorMod(baselineStartMinute - 1, CRON_POLL_INTERVAL_IN_MINUTES) != 0) {
      baselineStartMinute -= Math.floorMod(baselineStartMinute - 1, CRON_POLL_INTERVAL_IN_MINUTES);
    }
    this.baselineStartMinute = baselineStartMinute;
  }

  /**
   * Sets the floor value for Baseline End Minute
   * @param baselineEndMinute
   */
  public void setBaselineEndMinute(long baselineEndMinute) {
    this.baselineEndMinute = baselineEndMinute - Math.floorMod(baselineEndMinute, CRON_POLL_INTERVAL_IN_MINUTES);
  }

  /**
   * Sets the Exact Baseline Start Minute
   * @param baselineStartMinute
   */
  public void setExactBaselineStartMinute(long baselineStartMinute) {
    this.baselineStartMinute = baselineStartMinute;
  }

  /**
   * Sets the Exact Baseline End Minute
   * @param baselineEndMinute
   */
  public void setExactBaselineEndMinute(long baselineEndMinute) {
    this.baselineEndMinute = baselineEndMinute;
  }

  @Override
  public CVConfiguration deepCopy() {
    LogsCVConfiguration clonedConfig = new LogsCVConfiguration();
    super.copy(clonedConfig);
    clonedConfig.setQuery(this.getQuery());
    return clonedConfig;
  }

  protected void copy(LogsCVConfiguration cvConfiguration) {
    super.copy(cvConfiguration);
    cvConfiguration.setQuery(this.getQuery());
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  @JsonPropertyOrder({"type", "harnessApiVersion"})
  public static class LogsCVConfigurationYaml extends CVConfigurationYaml {
    private String query;
    private Long baselineStartMinute;
    private Long baselineEndMinute;
    private String alertPriority;
  }
}
