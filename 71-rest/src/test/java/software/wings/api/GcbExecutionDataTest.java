package software.wings.api;

import static io.harness.rule.OwnerRule.VGLIJIN;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.api.ExecutionDataValue.executionDataValue;
import static software.wings.helpers.ext.gcb.models.GcbBuildStatus.WORKING;
import static software.wings.sm.states.GcbState.GcbDelegateResponse.gcbDelegateResponseOf;

import com.google.common.collect.ImmutableMap;

import io.harness.CategoryTest;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.command.GcbTaskParams;
import software.wings.helpers.ext.gcb.models.GcbArtifacts;
import software.wings.helpers.ext.gcb.models.GcbBuildDetails;
import software.wings.sm.states.GcbState.GcbDelegateResponse;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GcbExecutionDataTest extends CategoryTest {
  public static final String ACTIVITY_ID = "activityId";
  public static final String CREATE_TIME = "2020-01-01T22:00:00.000000Z";
  public static final String BUILD_NAME = "2020-01-01T22:00:00.000000Z";
  public static final String BUILD_ID = "buildId";
  public static final String BUILD_URL = "https://gcb.com/testjob/11";
  public static final String LOGS_URL = "https://logs.com";
  public static final Map<String, String> SUBSTITUTIONS = ImmutableMap.of("$1", "1");
  public static final List<String> TAGS = Collections.singletonList("blue");
  private final GcbExecutionData gcbExecutionData =
      GcbExecutionData.builder().activityId(ACTIVITY_ID).buildId(BUILD_ID).buildUrl(BUILD_URL).build();

  private static final Map<String, ExecutionDataValue> expected =
      ImmutableMap.<String, ExecutionDataValue>builder()
          .put("activityId", executionDataValue("Activity Id", ACTIVITY_ID))
          .put("buildNumber", executionDataValue("Build Number", BUILD_ID))
          .put("build", executionDataValue("Build Url", BUILD_URL))
          .put("substitutions", executionDataValue("Substitutions", SUBSTITUTIONS))
          .put("logUrl", executionDataValue("Logs Url", LOGS_URL))
          .put("createTime", executionDataValue("Created Time", CREATE_TIME))
          .put("tags", executionDataValue("Tags", TAGS))
          .put("name", executionDataValue("Name", BUILD_NAME))
          .put("status", executionDataValue("Status", WORKING))
          .put("images", executionDataValue("Images", Arrays.asList("a", "b", "c")))
          .build();

  @Before
  public void setup() {
    gcbExecutionData.setErrorMsg("Err");
    gcbExecutionData.setStatus(ExecutionStatus.FAILED);
    gcbExecutionData.setTags(TAGS);
    gcbExecutionData.setBuildUrl(BUILD_URL);
    gcbExecutionData.setSubstitutions(SUBSTITUTIONS);
    gcbExecutionData.setLogUrl(LOGS_URL);
    gcbExecutionData.setCreateTime(CREATE_TIME);
    gcbExecutionData.setName(BUILD_NAME);
    gcbExecutionData.setBuildStatus(WORKING);
    GcbArtifacts artifacts = new GcbArtifacts();
    artifacts.setImages(Arrays.asList("a", "b", "c"));
    gcbExecutionData.setArtifacts(artifacts);
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
    assertThat(gcbExecutionData.getBuildStatus()).isEqualTo(WORKING);
    assertThat(gcbExecutionData.getTags()).isEqualTo(TAGS);
    assertThat(gcbExecutionData.getLogUrl()).isEqualTo(LOGS_URL);
    assertThat(gcbExecutionData.getSubstitutions()).isEqualTo(SUBSTITUTIONS);
    assertThat(gcbExecutionData.getCreateTime()).isEqualTo(CREATE_TIME);
    assertThat(gcbExecutionData.getName()).isEqualTo(BUILD_NAME);
    GcbArtifacts artifacts = new GcbArtifacts();
    artifacts.setImages(Arrays.asList("a", "b", "c"));
    assertThat(gcbExecutionData.getArtifacts()).isEqualTo(artifacts);
  }

  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void withDelegateResponse() {
    GcbArtifacts artifacts = new GcbArtifacts();
    artifacts.setImages(Arrays.asList("a", "b", "c"));

    GcbExecutionData expected = new GcbExecutionData(ACTIVITY_ID, GcbExecutionData.GCB_URL + BUILD_ID, BUILD_ID, TAGS,
        WORKING, BUILD_NAME, BUILD_NAME, SUBSTITUTIONS, LOGS_URL, artifacts);

    GcbDelegateResponse delegateResponse =
        gcbDelegateResponseOf(GcbTaskParams.builder().buildId(BUILD_ID).buildName(BUILD_NAME).build(),
            GcbBuildDetails.builder()
                .tags(TAGS)
                .status(WORKING)
                .createTime(CREATE_TIME)
                .substitutions(SUBSTITUTIONS)
                .logUrl(LOGS_URL)
                .artifacts(artifacts)
                .build());
    GcbExecutionData actual = new GcbExecutionData(ACTIVITY_ID, null, null, null, null, null, null, null, null, null)
                                  .withDelegateResponse(delegateResponse);
    assertThat(actual).isEqualTo(expected);
  }
}
