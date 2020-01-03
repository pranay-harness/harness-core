package software.wings.api;

import static io.harness.rule.OwnerRule.SRINIVAS;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.ImmutableMap;

import io.harness.CategoryTest;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.sm.states.ParameterEntry;

import java.util.ArrayList;
import java.util.List;

public class BambooExecutionDataTest extends CategoryTest {
  private BambooExecutionData bambooExecutionData;
  @Before
  public void setup() {
    ParameterEntry parameterEntry = new ParameterEntry();
    parameterEntry.setKey("MyKey");
    parameterEntry.setValue("MyValue");
    List<ParameterEntry> parameterEntryList = new ArrayList<>();
    parameterEntryList.add(parameterEntry);
    bambooExecutionData = BambooExecutionData.builder()
                              .planName("testPlan")
                              .buildUrl("http://bamboo/testPlan/11")
                              .parameters(parameterEntryList)
                              .build();

    bambooExecutionData.setErrorMsg("Err");
    bambooExecutionData.setBuildStatus("ERROR");
    bambooExecutionData.setStatus(ExecutionStatus.FAILED);
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetExecutionSummary() {
    assertThat(bambooExecutionData.getExecutionSummary())
        .containsAllEntriesOf(ImmutableMap.of("planName",
            ExecutionDataValue.builder().displayName("Plan Name").value("testPlan").build(), "buildUrl",
            ExecutionDataValue.builder().displayName("Build Url").value("http://bamboo/testPlan/11").build(),
            "buildStatus", ExecutionDataValue.builder().displayName("Build Status").value("ERROR").build()));
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldGetExecutionDetails() {
    assertThat(bambooExecutionData.getExecutionDetails())
        .containsAllEntriesOf(ImmutableMap.of("planName",
            ExecutionDataValue.builder().displayName("Plan Name").value("testPlan").build(), "buildUrl",
            ExecutionDataValue.builder().displayName("Build Url").value("http://bamboo/testPlan/11").build(),
            "buildStatus", ExecutionDataValue.builder().displayName("Build Status").value("ERROR").build()));
  }
}
