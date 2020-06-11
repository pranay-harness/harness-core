package software.wings.api;

import static io.harness.rule.OwnerRule.VGLIJIN;
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
  private final GcbExecutionData gcbExecutionData =
      GcbExecutionData.builder().activityId(ACTIVITY_ID).buildId(BUILD_ID).buildUrl(BUILD_URL).build();

  private static final Map<String, ExecutionDataValue> expected = ImmutableMap.of("activityId",
      executionDataValue("Activity Id", ACTIVITY_ID), "buildNumber", executionDataValue("Build Number", BUILD_ID),
      "description", executionDataValue("Description", null), "build", executionDataValue("Build Url", BUILD_URL));

  @Before
  public void setup() {
    gcbExecutionData.setErrorMsg("Err");
    gcbExecutionData.setStatus(ExecutionStatus.FAILED);
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
}
