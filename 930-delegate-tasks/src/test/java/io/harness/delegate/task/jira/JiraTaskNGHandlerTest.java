package io.harness.delegate.task.jira;

import static io.harness.delegate.task.jira.JiraTaskNGHandler.JIRA_APPROVAL_FIELD_KEY;
import static io.harness.delegate.task.jira.JiraTaskNGHandler.ORIGINAL_ESTIMATE;
import static io.harness.delegate.task.jira.JiraTaskNGHandler.REMAINING_ESTIMATE;
import static io.harness.rule.OwnerRule.AGORODETKI;
import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.connector.jira.JiraConnectorDTO;
import io.harness.delegate.task.jira.JiraTaskNGParameters.JiraTaskNGParametersBuilder;
import io.harness.delegate.task.jira.response.JiraTaskNGResponse;
import io.harness.encryption.SecretRefData;
import io.harness.jira.JiraCustomFieldValue;
import io.harness.logging.CommandExecutionStatus;
import io.harness.rule.Owner;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.rcarz.jiraclient.Field;
import net.rcarz.jiraclient.Issue;
import net.rcarz.jiraclient.Issue.FluentCreate;
import net.rcarz.jiraclient.Issue.SearchResult;
import net.rcarz.jiraclient.JiraClient;
import net.rcarz.jiraclient.JiraException;
import net.rcarz.jiraclient.RestClient;
import net.rcarz.jiraclient.TimeTracking;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JiraClient.class, FluentCreate.class, Issue.class, JiraTaskNGHandler.class})
@PowerMockIgnore({"javax.net.ssl.*"})
public class JiraTaskNGHandlerTest extends CategoryTest {
  private final JiraTaskNGHandler jiraTaskNGHandler = new JiraTaskNGHandler();

  @Captor ArgumentCaptor<String> fieldArgumentCaptor;
  @Captor ArgumentCaptor<TimeTracking> timeTrackingArgumentCaptor;
  @Captor ArgumentCaptor<Object> valuesArgumentCaptor;

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void testValidateCredentials() throws Exception {
    JiraClient jiraClient = Mockito.mock(JiraClient.class);
    when(jiraClient.getProjects()).thenReturn(new ArrayList<>());
    PowerMockito.whenNew(JiraClient.class).withAnyArguments().thenReturn(jiraClient);

    JiraTaskNGResponse jiraTaskNGResponse =
        jiraTaskNGHandler.validateCredentials(createJiraTaskParametersBuilder().build());
    assertThat(jiraTaskNGResponse.getExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void testValidateCredentialsError() throws Exception {
    JiraClient jiraClient = Mockito.mock(JiraClient.class);
    when(jiraClient.getProjects()).thenThrow(new JiraException("Exception"));
    PowerMockito.whenNew(JiraClient.class).withAnyArguments().thenReturn(jiraClient);

    JiraTaskNGResponse jiraTaskNGResponse =
        jiraTaskNGHandler.validateCredentials(createJiraTaskParametersBuilder().build());
    assertThat(jiraTaskNGResponse.getExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void testCreateTicket() throws Exception {
    final JiraTaskNGParameters jiraTaskNGParameters = createJiraTaskParametersBuilder()
                                                          .summary("Summary")
                                                          .priority("Prioity")
                                                          .description("Description")
                                                          .project("project")
                                                          .issueType("Bug")
                                                          .labels(new ArrayList<>())
                                                          .build();

    JiraClient jiraClient = Mockito.mock(JiraClient.class);
    FluentCreate fluentCreate = Mockito.mock(FluentCreate.class);
    Issue issue = Mockito.mock(Issue.class);
    when(jiraClient.createIssue(any(), any())).thenReturn(fluentCreate);
    when(fluentCreate.field(any(), any())).thenReturn(fluentCreate);
    when(fluentCreate.execute()).thenReturn(issue);
    PowerMockito.whenNew(JiraClient.class).withAnyArguments().thenReturn(jiraClient);

    JiraTaskNGResponse jiraTaskNGResponse = jiraTaskNGHandler.createTicket(jiraTaskNGParameters);
    assertThat(jiraTaskNGResponse.getExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void testCreateTicketThrowJiraException() throws Exception {
    final JiraTaskNGParameters jiraTaskNGParameters = createJiraTaskParametersBuilder()
                                                          .summary("Summary")
                                                          .priority("Prioity")
                                                          .description("Description")
                                                          .project("project")
                                                          .issueType("Bug")
                                                          .labels(new ArrayList<>())
                                                          .build();

    JiraClient jiraClient = Mockito.mock(JiraClient.class);
    FluentCreate fluentCreate = Mockito.mock(FluentCreate.class);
    when(jiraClient.createIssue(any(), any())).thenReturn(fluentCreate);
    when(fluentCreate.field(any(), any())).thenReturn(fluentCreate);
    when(fluentCreate.execute()).thenThrow(new JiraException("Exception"));
    PowerMockito.whenNew(JiraClient.class).withAnyArguments().thenReturn(jiraClient);

    JiraTaskNGResponse jiraTaskNGResponse = jiraTaskNGHandler.createTicket(jiraTaskNGParameters);
    assertThat(jiraTaskNGResponse.getExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldSetTimeTrackingPropertiesOnTicketCreate() {
    FluentCreate fluentCreate = mock(FluentCreate.class);
    Map<String, JiraCustomFieldValue> customFields = new HashMap<>();
    customFields.put(ORIGINAL_ESTIMATE, new JiraCustomFieldValue(Field.TIME_TRACKING, "1d 2h"));
    customFields.put(REMAINING_ESTIMATE, new JiraCustomFieldValue(Field.TIME_TRACKING, "4h"));
    JiraTaskNGParameters parameters = JiraTaskNGParameters.builder().customFields(customFields).build();

    jiraTaskNGHandler.setCustomFieldsOnCreate(parameters, fluentCreate);
    Mockito.verify(fluentCreate).field(fieldArgumentCaptor.capture(), timeTrackingArgumentCaptor.capture());

    String capturedField = fieldArgumentCaptor.getValue();
    TimeTracking capturedTimeTracking = timeTrackingArgumentCaptor.getValue();

    assertThat(capturedField).isEqualTo(Field.TIME_TRACKING);
    assertThat(capturedTimeTracking.getOriginalEstimate())
        .isEqualTo(customFields.get(ORIGINAL_ESTIMATE).getFieldValue());
    assertThat(capturedTimeTracking.getRemainingEstimate())
        .isEqualTo(customFields.get(REMAINING_ESTIMATE).getFieldValue());
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldSetAllCustomFieldsOnTicketCreate() {
    FluentCreate fluentCreate = mock(FluentCreate.class);
    Map<String, JiraCustomFieldValue> customFields = new HashMap<>();
    customFields.put(ORIGINAL_ESTIMATE, new JiraCustomFieldValue(Field.TIME_TRACKING, "1d 2h"));
    customFields.put(REMAINING_ESTIMATE, new JiraCustomFieldValue(Field.TIME_TRACKING, "4h"));
    customFields.put("someCustomField", new JiraCustomFieldValue("multiselect", "first,second"));
    customFields.put("otherCustomField", new JiraCustomFieldValue("any", "value"));

    JiraTaskNGParameters parameters = JiraTaskNGParameters.builder().customFields(customFields).build();

    jiraTaskNGHandler.setCustomFieldsOnCreate(parameters, fluentCreate);
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
    Issue.FluentUpdate fluentUpdate = mock(Issue.FluentUpdate.class);
    Map<String, JiraCustomFieldValue> customFields = new HashMap<>();
    customFields.put(ORIGINAL_ESTIMATE, new JiraCustomFieldValue(Field.TIME_TRACKING, "1d 2h"));
    customFields.put(REMAINING_ESTIMATE, new JiraCustomFieldValue(Field.TIME_TRACKING, "4h"));

    JiraTaskNGParameters parameters = JiraTaskNGParameters.builder().customFields(customFields).build();

    jiraTaskNGHandler.setCustomFieldsOnUpdate(parameters, fluentUpdate);
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
    Issue.FluentUpdate fluentUpdate = mock(Issue.FluentUpdate.class);
    Map<String, JiraCustomFieldValue> customFields = new HashMap<>();
    customFields.put(ORIGINAL_ESTIMATE, new JiraCustomFieldValue(Field.TIME_TRACKING, "1d 2h"));
    customFields.put(REMAINING_ESTIMATE, new JiraCustomFieldValue(Field.TIME_TRACKING, "4h"));
    customFields.put("someCustomField", new JiraCustomFieldValue("multiselect", "first,second"));

    JiraTaskNGParameters parameters = JiraTaskNGParameters.builder().customFields(customFields).build();

    jiraTaskNGHandler.setCustomFieldsOnUpdate(parameters, fluentUpdate);
    Mockito.verify(fluentUpdate, Mockito.times(2)).field(fieldArgumentCaptor.capture(), valuesArgumentCaptor.capture());

    List<String> capturedFields = fieldArgumentCaptor.getAllValues();
    List<Object> capturedValues = valuesArgumentCaptor.getAllValues();

    assertThat(capturedFields).containsExactlyInAnyOrder(Field.TIME_TRACKING, "someCustomField");
    assertThat(capturedValues.size()).isEqualTo(2);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void testFetchIssues() throws Exception {
    JiraClient jiraClient = Mockito.mock(JiraClient.class);
    Issue issue = Mockito.mock(Issue.class);
    PowerMockito.whenNew(JiraClient.class).withAnyArguments().thenReturn(jiraClient);

    when(jiraClient.getIssue(any())).thenReturn(issue);
    when(issue.getField(any())).thenReturn(Collections.singletonMap(JIRA_APPROVAL_FIELD_KEY, "value"));

    JiraTaskNGResponse jiraTaskNGResponse =
        jiraTaskNGHandler.fetchIssue(createJiraTaskParametersBuilder().approvalField("approvalField").build());
    assertThat(jiraTaskNGResponse.getExecutionStatus()).isEqualTo(CommandExecutionStatus.RUNNING);
    assertThat(jiraTaskNGResponse.getCurrentStatus()).isEqualTo("value");
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void testFetchIssuesFailure() throws Exception {
    JiraClient jiraClient = Mockito.mock(JiraClient.class);
    PowerMockito.whenNew(JiraClient.class).withAnyArguments().thenReturn(jiraClient);

    when(jiraClient.getIssue(any())).thenThrow(new JiraException("Exception"));

    JiraTaskNGResponse jiraTaskNGResponse =
        jiraTaskNGHandler.fetchIssue(createJiraTaskParametersBuilder().approvalField("approvalField").build());
    assertThat(jiraTaskNGResponse.getExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void testGetStatuses() throws Exception {
    JiraClient jiraClient = Mockito.mock(JiraClient.class);
    RestClient restClient = Mockito.mock(RestClient.class);
    PowerMockito.whenNew(JiraClient.class).withAnyArguments().thenReturn(jiraClient);

    when(jiraClient.getRestClient()).thenReturn(restClient);
    when(restClient.get(any(URI.class))).thenReturn(new JSONArray());

    JiraTaskNGResponse jiraTaskNGResponse =
        jiraTaskNGHandler.getStatuses(createJiraTaskParametersBuilder().project("CDNG").build());
    assertThat(jiraTaskNGResponse.getExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(jiraTaskNGResponse.getStatuses()).isEmpty();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void testGetStatusesFailure() throws Exception {
    JiraClient jiraClient = Mockito.mock(JiraClient.class);
    RestClient restClient = Mockito.mock(RestClient.class);
    PowerMockito.whenNew(JiraClient.class).withAnyArguments().thenReturn(jiraClient);

    when(jiraClient.getRestClient()).thenReturn(restClient);
    when(restClient.get(any(URI.class))).thenThrow(new IOException());

    JiraTaskNGResponse jiraTaskNGResponse =
        jiraTaskNGHandler.getStatuses(createJiraTaskParametersBuilder().project("CDNG").build());
    assertThat(jiraTaskNGResponse.getExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void testGetProjects() throws Exception {
    JiraClient jiraClient = Mockito.mock(JiraClient.class);
    when(jiraClient.getProjects()).thenReturn(new ArrayList<>());
    PowerMockito.whenNew(JiraClient.class).withAnyArguments().thenReturn(jiraClient);

    JiraTaskNGResponse jiraTaskNGResponse = jiraTaskNGHandler.getProjects(createJiraTaskParametersBuilder().build());
    assertThat(jiraTaskNGResponse.getExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(jiraTaskNGResponse.getProjects()).isNotNull();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void testGetProjectsError() throws Exception {
    JiraClient jiraClient = Mockito.mock(JiraClient.class);
    when(jiraClient.getProjects()).thenThrow(new JiraException("Exception"));
    PowerMockito.whenNew(JiraClient.class).withAnyArguments().thenReturn(jiraClient);

    JiraTaskNGResponse jiraTaskNGResponse = jiraTaskNGHandler.getProjects(createJiraTaskParametersBuilder().build());
    assertThat(jiraTaskNGResponse.getExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void testGetFieldsAndOptions() throws Exception {
    JiraClient jiraClient = Mockito.mock(JiraClient.class);
    SearchResult searchResult = Mockito.mock(SearchResult.class);
    RestClient restClient = Mockito.mock(RestClient.class);

    when(jiraClient.getRestClient()).thenReturn(restClient);
    when(restClient.get(any(URI.class))).thenReturn(new JSONObject());
    when(jiraClient.searchIssues(any(), anyInt())).thenReturn(searchResult);
    PowerMockito.whenNew(JiraClient.class).withAnyArguments().thenReturn(jiraClient);

    JiraTaskNGResponse jiraTaskNGResponse =
        jiraTaskNGHandler.getFieldsOptions(createJiraTaskParametersBuilder().project("CDNG").build());
    assertThat(jiraTaskNGResponse.getExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void testGetFieldsAndOptionsFailure() throws Exception {
    JiraClient jiraClient = Mockito.mock(JiraClient.class);

    when(jiraClient.searchIssues(any(), anyInt())).thenThrow(new JiraException("Exception"));
    PowerMockito.whenNew(JiraClient.class).withAnyArguments().thenReturn(jiraClient);

    JiraTaskNGResponse jiraTaskNGResponse =
        jiraTaskNGHandler.getFieldsOptions(createJiraTaskParametersBuilder().build());
    assertThat(jiraTaskNGResponse.getExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void testGetCreateMetadata() throws Exception {
    final URI meta = URI.create("issue/createmeta");
    final URI resolution = URI.create(Field.RESOLUTION);
    JiraClient jiraClient = Mockito.mock(JiraClient.class);
    RestClient restClient = Mockito.mock(RestClient.class);

    when(jiraClient.getRestClient()).thenReturn(restClient);
    when(restClient.buildURI(any(), any())).thenReturn(meta);
    when(restClient.buildURI(any())).thenReturn(resolution);
    when(restClient.get(eq(meta))).thenReturn(new JSONObject().element("expand", "expand").element("projects", "[]"));
    when(restClient.get(eq(resolution))).thenReturn(new JSONArray());
    PowerMockito.whenNew(JiraClient.class).withAnyArguments().thenReturn(jiraClient);

    JiraTaskNGResponse jiraTaskNGResponse =
        jiraTaskNGHandler.getCreateMetadata(createJiraTaskParametersBuilder().project("CDNG").build());
    assertThat(jiraTaskNGResponse.getExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void testGetCreateMetadataFailure() throws Exception {
    JiraClient jiraClient = Mockito.mock(JiraClient.class);
    RestClient restClient = Mockito.mock(RestClient.class);

    when(jiraClient.getRestClient()).thenReturn(restClient);
    when(restClient.get(any(URI.class))).thenThrow(new IOException());
    PowerMockito.whenNew(JiraClient.class).withAnyArguments().thenReturn(jiraClient);

    JiraTaskNGResponse jiraTaskNGResponse =
        jiraTaskNGHandler.getCreateMetadata(createJiraTaskParametersBuilder().build());
    assertThat(jiraTaskNGResponse.getExecutionStatus()).isEqualTo(CommandExecutionStatus.FAILURE);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void testCheckApproval() throws Exception {
    final String approvalValue = "success";
    JiraClient jiraClient = Mockito.mock(JiraClient.class);
    Issue issue = Mockito.mock(Issue.class);
    PowerMockito.whenNew(JiraClient.class).withAnyArguments().thenReturn(jiraClient);

    when(jiraClient.getIssue(any())).thenReturn(issue);
    when(issue.getField(any())).thenReturn(Collections.singletonMap(JIRA_APPROVAL_FIELD_KEY, approvalValue));

    JiraTaskNGResponse jiraTaskNGResponse = jiraTaskNGHandler.checkJiraApproval(createJiraTaskParametersBuilder()
                                                                                    .issueId("issueId")
                                                                                    .approvalField("approvalField")
                                                                                    .approvalValue(approvalValue)
                                                                                    .build());
    assertThat(jiraTaskNGResponse.getExecutionStatus()).isEqualTo(CommandExecutionStatus.SUCCESS);
    assertThat(jiraTaskNGResponse.getCurrentStatus()).isEqualTo(approvalValue);
  }

  private JiraTaskNGParametersBuilder createJiraTaskParametersBuilder() {
    JiraConnectorDTO jiraConnectorDTO =
        JiraConnectorDTO.builder()
            .jiraUrl("https://harness.atlassian.net/")
            .username("username")
            .passwordRef(SecretRefData.builder().decryptedValue(new char[] {'3', '4', 'f', '5', '1'}).build())
            .build();

    return JiraTaskNGParameters.builder().jiraConnectorDTO(jiraConnectorDTO);
  }
}
