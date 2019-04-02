package software.wings.verification.log;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.common.VerificationConstants.CRON_POLL_INTERVAL_IN_MINUTES;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.github.reinert.jjschema.Attributes;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.stencils.DefaultValue;
import software.wings.verification.CVConfiguration;

@Data
@EqualsAndHashCode(callSuper = true)
public class LogsCVConfiguration extends CVConfiguration {
  @Attributes(title = "Search Keywords", required = true) @DefaultValue("*exception*") protected String query;

  private long baselineStartMinute = -1;
  private long baselineEndMinute = -1;

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

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  @JsonPropertyOrder({"type", "harnessApiVersion"})
  public static class LogsCVConfigurationYaml extends CVConfigurationYaml {
    private String query;
    private long baselineStartMinute;
    private long baselineEndMinute;
  }
}
