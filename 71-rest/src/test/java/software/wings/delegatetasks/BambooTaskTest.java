package software.wings.delegatetasks;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.RAGHU;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Maps;

import io.harness.CategoryTest;
import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import software.wings.beans.BambooConfig;
import software.wings.beans.TaskType;
import software.wings.helpers.ext.bamboo.BambooService;
import software.wings.helpers.ext.bamboo.Result;
import software.wings.helpers.ext.bamboo.Status;
import software.wings.sm.states.FilePathAssertionEntry;
import software.wings.sm.states.ParameterEntry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by sgurubelli on 8/30/17.
 */
public class BambooTaskTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock BambooService bambooService;

  @InjectMocks
  private BambooTask bambooTask = (BambooTask) TaskType.BAMBOO.getDelegateRunnableTask("delid1",
      DelegateTask.builder().async(true).data(TaskData.builder().timeout(DEFAULT_ASYNC_CALL_TIMEOUT).build()).build(),
      notifyResponseData -> {}, () -> true);

  private String bambooUrl = "http://localhost:9095/";
  private String userName = "admin";
  private char[] password = "pass1".toCharArray();
  private String planKey = "TOD-TOD";
  private String buildResultKey = "TOD-TOD-85";
  private List<ParameterEntry> parameters = new ArrayList<>();
  private List<FilePathAssertionEntry> filePathAssertionEntries = new ArrayList<>();

  private BambooConfig bambooConfig =
      BambooConfig.builder().bambooUrl("http://localhost:9095/").username(userName).password(password).build();

  @Before
  public void setUp() throws Exception {
    Map<String, String> evaluatedParameters = Maps.newLinkedHashMap();
    if (isNotEmpty(parameters)) {
      parameters.forEach(
          parameterEntry -> { evaluatedParameters.put(parameterEntry.getKey(), parameterEntry.getValue()); });
    }
    when(bambooService.triggerPlan(bambooConfig, null, planKey, evaluatedParameters)).thenReturn(buildResultKey);
    when(bambooService.getBuildResultStatus(bambooConfig, null, buildResultKey))
        .thenReturn(Status.builder().finished(true).build());
  }
  @Test
  @Owner(developers = RAGHU)
  @Category(UnitTests.class)
  public void shouldExecuteSuccessfullyWhenBuildPasses() throws Exception {
    when(bambooService.getBuildResult(bambooConfig, null, planKey))
        .thenReturn(Result.builder().buildResultKey(buildResultKey).buildState("Successful").build());
    bambooTask.run(bambooConfig, null, planKey, parameters);
    verify(bambooService).triggerPlan(bambooConfig, null, planKey, Collections.emptyMap());
    verify(bambooService).getBuildResult(bambooConfig, null, buildResultKey);
  }
}
