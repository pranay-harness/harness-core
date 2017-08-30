package software.wings.helpers.ext.bamboo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;

/**
 * Created by sgurubelli on 8/28/17.
 */
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Result {
  private String buildResultKey;
  private String planName;
  private String projectName;
  private String buildState;
  private String buildNumber;
  private String buildStartedTime;
  private String buildCompletedTime;
  private String buildTestSummary;
  private String successfulTestCount;
  private String failedTestCount;
  private String skippedTestCount;
  private String buildReason;
  private String finished;
  private String buildUrl;
  private String lifeCycleState;
  private Link link = new Link();
  private Plan plan = new Plan();

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Link {
    private String href;
  }

  @Data
  @JsonIgnoreProperties(ignoreUnknown = true)
  public static class Plan {
    private String shortName;
    private String name;
    private Link link = new Link();
  }
}
