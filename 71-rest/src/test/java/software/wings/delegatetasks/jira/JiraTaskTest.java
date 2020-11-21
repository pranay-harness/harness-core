package software.wings.delegatetasks.jira;

import static io.harness.rule.OwnerRule.AGORODETKI;
import static io.harness.rule.OwnerRule.ROHITKARELIA;

import static org.assertj.core.api.Assertions.assertThat;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.spy;

import io.harness.CategoryTest;
import io.harness.beans.ExecutionStatus;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateTaskPackage;
import io.harness.delegate.beans.TaskData;
import io.harness.jira.JiraAction;
import io.harness.jira.JiraCustomFieldValue;
import io.harness.rule.Owner;

import software.wings.api.jira.JiraExecutionData;
import software.wings.beans.JiraConfig;
import software.wings.beans.jira.JiraTaskParameters;
import software.wings.service.intfc.security.EncryptionService;

import com.google.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.rcarz.jiraclient.Field;
import net.rcarz.jiraclient.Issue.FluentCreate;
import net.rcarz.jiraclient.Issue.FluentUpdate;
import net.rcarz.jiraclient.TimeTracking;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({FluentCreate.class, FluentUpdate.class})
@PowerMockIgnore({"javax.net.ssl.*", "javax.security.auth.x500.X500Principal"})
public class JiraTaskTest extends CategoryTest {
  @Inject
  @InjectMocks
  JiraTask jiraTask =
      new JiraTask(DelegateTaskPackage.builder().data(TaskData.builder().build()).build(), null, null, null);

  @Mock private EncryptionService encryptionService;

  @Captor ArgumentCaptor<String> fieldArgumentCaptor;
  @Captor ArgumentCaptor<TimeTracking> timeTrackingArgumentCaptor;
  @Captor ArgumentCaptor<Object> valuesArgumentCaptor;

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldSetTimeTrackingPropertiesOnTicketCreate() {
    FluentCreate fluentCreate = mock(FluentCreate.class);
    Map<String, JiraCustomFieldValue> customFields = new HashMap<>();
    customFields.put("TimeTracking:OriginalEstimate", new JiraCustomFieldValue("timetracking", "1d 2h"));
    customFields.put("TimeTracking:RemainingEstimate", new JiraCustomFieldValue("timetracking", "4h"));
    JiraTaskParameters parameters = JiraTaskParameters.builder().customFields(customFields).build();

    jiraTask.setCustomFieldsOnCreate(parameters, fluentCreate);
    Mockito.verify(fluentCreate).field(fieldArgumentCaptor.capture(), timeTrackingArgumentCaptor.capture());

    String capturedField = fieldArgumentCaptor.getValue();
    TimeTracking capturedTimeTracking = timeTrackingArgumentCaptor.getValue();

    assertThat(capturedField).isEqualTo(Field.TIME_TRACKING);
    assertThat(capturedTimeTracking.getOriginalEstimate())
        .isEqualTo(customFields.get("TimeTracking:OriginalEstimate").getFieldValue());
    assertThat(capturedTimeTracking.getRemainingEstimate())
        .isEqualTo(customFields.get("TimeTracking:RemainingEstimate").getFieldValue());
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldSetAllCustomFieldsOnTicketCreate() {
    FluentCreate fluentCreate = mock(FluentCreate.class);
    Map<String, JiraCustomFieldValue> customFields = new HashMap<>();
    customFields.put("TimeTracking:OriginalEstimate", new JiraCustomFieldValue("timetracking", "1d 2h"));
    customFields.put("TimeTracking:RemainingEstimate", new JiraCustomFieldValue("timetracking", "4h"));
    customFields.put("someCustomField", new JiraCustomFieldValue("multiselect", "first,second"));
    customFields.put("otherCustomField", new JiraCustomFieldValue("any", "value"));

    JiraTaskParameters parameters = JiraTaskParameters.builder().customFields(customFields).build();

    jiraTask.setCustomFieldsOnCreate(parameters, fluentCreate);
    Mockito.verify(fluentCreate, Mockito.times(3)).field(fieldArgumentCaptor.capture(), valuesArgumentCaptor.capture());

    List<String> capturedFields = fieldArgumentCaptor.getAllValues();
    List<Object> capturedValues = valuesArgumentCaptor.getAllValues();

    assertThat(capturedFields).containsExactlyInAnyOrder(Field.TIME_TRACKING, "someCustomField", "otherCustomField");
    assertThat(capturedValues.size()).isEqualTo(3);
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldSetTimeTrackingPropertiesOnTicketUpdate() {
    FluentUpdate fluentUpdate = mock(FluentUpdate.class);
    Map<String, JiraCustomFieldValue> customFields = new HashMap<>();
    customFields.put("TimeTracking:OriginalEstimate", new JiraCustomFieldValue("timetracking", "1d 2h"));
    customFields.put("TimeTracking:RemainingEstimate", new JiraCustomFieldValue("timetracking", "4h"));

    JiraTaskParameters parameters = JiraTaskParameters.builder().customFields(customFields).build();

    jiraTask.setCustomFieldsOnUpdate(parameters, fluentUpdate);
    Mockito.verify(fluentUpdate).field(fieldArgumentCaptor.capture(), timeTrackingArgumentCaptor.capture());

    String capturedField = fieldArgumentCaptor.getValue();
    TimeTracking capturedTimeTracking = timeTrackingArgumentCaptor.getValue();

    assertThat(capturedField).isEqualTo(Field.TIME_TRACKING);
    assertThat(capturedTimeTracking.getOriginalEstimate())
        .isEqualTo(customFields.get("TimeTracking:OriginalEstimate").getFieldValue());
    assertThat(capturedTimeTracking.getRemainingEstimate())
        .isEqualTo(customFields.get("TimeTracking:RemainingEstimate").getFieldValue());
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldSetAllCustomFieldsOnTicketUpdate() {
    FluentUpdate fluentUpdate = mock(FluentUpdate.class);
    Map<String, JiraCustomFieldValue> customFields = new HashMap<>();
    customFields.put("TimeTracking:OriginalEstimate", new JiraCustomFieldValue("timetracking", "1d 2h"));
    customFields.put("TimeTracking:RemainingEstimate", new JiraCustomFieldValue("timetracking", "4h"));
    customFields.put("someCustomField", new JiraCustomFieldValue("multiselect", "first,second"));

    JiraTaskParameters parameters = JiraTaskParameters.builder().customFields(customFields).build();

    jiraTask.setCustomFieldsOnUpdate(parameters, fluentUpdate);
    Mockito.verify(fluentUpdate, Mockito.times(2)).field(fieldArgumentCaptor.capture(), valuesArgumentCaptor.capture());

    List<String> capturedFields = fieldArgumentCaptor.getAllValues();
    List<Object> capturedValues = valuesArgumentCaptor.getAllValues();

    assertThat(capturedFields).containsExactlyInAnyOrder(Field.TIME_TRACKING, "someCustomField");
    assertThat(capturedValues.size()).isEqualTo(2);
  }

  @Test
  @Owner(developers = ROHITKARELIA)
  @Category(UnitTests.class)
  public void shouldNotFailJiraApprovalOnException() {
    JiraTaskParameters parameters = getJiraTaskParametersForApproval();
    JiraTask spyJiraTask = spy(jiraTask);
    spyJiraTask.checkJiraApproval(parameters);
    JiraExecutionData jiraExecutionData = (JiraExecutionData) jiraTask.run(parameters);
    assertThat(jiraExecutionData.getExecutionStatus()).isEqualTo(ExecutionStatus.PAUSED);
  }

  private JiraTaskParameters getJiraTaskParametersForApproval() {
    String baseURL = "http://jira.com";
    JiraConfig jiraConfig = JiraConfig.builder().baseUrl(baseURL).username("username").password(new char['p']).build();
    return JiraTaskParameters.builder()
        .jiraAction(JiraAction.CHECK_APPROVAL)
        .issueId("ISSUE-10012")
        .jiraConfig(jiraConfig)
        .build();
  }
}
