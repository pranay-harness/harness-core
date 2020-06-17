package software.wings.api;

import static io.harness.rule.OwnerRule.VGLIJIN;
import static java.lang.String.valueOf;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.api.ExecutionDataValue.executionDataValue;

import com.google.common.collect.ImmutableMap;

import io.harness.CategoryTest;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Map;

public class GcbExecutionDataTest extends CategoryTest {
  public static final String ACTIVITY_ID = "activityId";
  public static final String BUILD_ID = "buildId";
  public static final String BUILD_URL = "https://gcb.com/testjob/11";
  public static final String DESCRIPTION = "DESCRIPTION";
  public static final Map<String, String> METADATA = ImmutableMap.of("1", "2");
  public static final Map<String, String> ENV_VARS = ImmutableMap.of("$1", "1");
  public static final Map<String, String> JOB_PARAMS = ImmutableMap.of(".", ".");
  private final GcbExecutionData gcbExecutionData =
      GcbExecutionData.builder().activityId(ACTIVITY_ID).buildId(BUILD_ID).buildUrl(BUILD_URL).build();

  private static final Map<String, ExecutionDataValue> expected =
      ImmutableMap.<String, ExecutionDataValue>builder()
          .put("activityId", executionDataValue("Activity Id", ACTIVITY_ID))
          .put("buildNumber", executionDataValue("Build Number", BUILD_ID))
          .put("description", executionDataValue("Description", DESCRIPTION))
          .put("envVars", executionDataValue("Environment Variables", ENV_VARS))
          .put("metadata", executionDataValue("Meta-Data", valueOf(METADATA)))
          .put("jobParameters", executionDataValue("Job Parameters", JOB_PARAMS))
          .build();

  @Before
  public void setup() {
    gcbExecutionData.setErrorMsg("Err");
    gcbExecutionData.setStatus(ExecutionStatus.FAILED);
    gcbExecutionData.setDescription(DESCRIPTION);
    gcbExecutionData.setMetadata(METADATA);
    gcbExecutionData.setEnvVars(ENV_VARS);
    gcbExecutionData.setJobParameters(JOB_PARAMS);
  }

  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void shouldGetExecutionSummary() {
    assertThat(gcbExecutionData.getExecutionSummary()).containsAllEntriesOf(expected);
  }

  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void shouldGetExecutionDetails() {
    assertThat(gcbExecutionData.getExecutionDetails()).containsAllEntriesOf(expected);
  }

  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void shouldBeEqualToSelf() {
    assertThat(gcbExecutionData).isEqualTo(gcbExecutionData);
  }

  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void lombokGettersTe() {
    assertThat(gcbExecutionData.getActivityId()).isEqualTo(ACTIVITY_ID);
    assertThat(gcbExecutionData.getBuildId()).isEqualTo(BUILD_ID);
    assertThat(gcbExecutionData.getBuildUrl()).isEqualTo(BUILD_URL);
    assertThat(gcbExecutionData.getDescription()).isEqualTo(DESCRIPTION);
    assertThat(gcbExecutionData.getMetadata()).isEqualTo(METADATA);
    assertThat(gcbExecutionData.getEnvVars()).isEqualTo(ENV_VARS);
    assertThat(gcbExecutionData.getJobParameters()).isEqualTo(JOB_PARAMS);
  }
}
