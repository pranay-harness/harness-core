package software.wings.sm.states.collaboration;

import static io.harness.rule.OwnerRule.AGORODETKI;
import static io.harness.rule.OwnerRule.POOJA;
import static io.harness.rule.OwnerRule.PRABU;

import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.JIRA_CONNECTOR_ID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.exception.GeneralException;
import io.harness.exception.HarnessJiraException;
import io.harness.exception.InvalidRequestException;
import io.harness.jira.JiraCreateMetaResponse;
import io.harness.jira.JiraCustomFieldValue;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.Activity;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.service.impl.JiraHelperService;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.ExecutionContextImpl;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

public class JiraCreateUpdateTest extends WingsBaseTest {
  @Mock private ExecutionContextImpl context;
  @Mock private JiraHelperService jiraHelperService;
  @Mock private SettingsService settingsService;
  @Mock private ActivityService activityService;
  @InjectMocks JiraCreateUpdate jiraCreateUpdateState = new JiraCreateUpdate("Jira");
  private static JiraCreateMetaResponse createMetaResponse;
  private static JSONArray projects;
  private static Object statuses;

  @BeforeClass
  public static void setup() throws IOException {
    JSONObject jsonObject =
        new ObjectMapper().readValue(new File("src/test/resources/mock_create_meta"), JSONObject.class);
    createMetaResponse = new JiraCreateMetaResponse(jsonObject);
    projects = new ObjectMapper().readValue(new File("src/test/resources/mock_projects"), JSONArray.class);
    statuses = new ObjectMapper().readValue(new File("src/test/resources/mock_statuses"), JSONArray.class);
  }

  @Before
  public void setUpMocks() {
    when(context.getAccountId()).thenReturn(ACCOUNT_ID);
    when(context.getAppId()).thenReturn(APP_ID);
    when(jiraHelperService.getProjects(anyString(), anyString(), anyString())).thenReturn(projects);
    when(jiraHelperService.getStatuses(anyString(), anyString(), anyString(), anyString())).thenReturn(statuses);
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testValidateFields() {
    Map<String, String> results = jiraCreateUpdateState.validateFields();
    assertThat(results).isNotEmpty();
    assertThat(results.values().iterator().next()).isEqualTo("Connector, Project and IssueType must be provided.");
    jiraCreateUpdateState.setJiraConnectorId("CONNECTOR_ID");
    results = jiraCreateUpdateState.validateFields();
    assertThat(results).isNotEmpty();
    assertThat(results.values().iterator().next()).isEqualTo("Connector, Project and IssueType must be provided.");

    jiraCreateUpdateState.setProject("TJI");
    jiraCreateUpdateState.setIssueType("Story");
    Map<String, JiraCustomFieldValue> customFieldValueMap = new HashMap<>();
    jiraCreateUpdateState.setCustomFields(customFieldValueMap);
    JiraCustomFieldValue date = new JiraCustomFieldValue("date", "2014-04-05");
    JiraCustomFieldValue datetime = new JiraCustomFieldValue("datetime", "1234567891011");
    customFieldValueMap.put("due date", date);
    customFieldValueMap.put("start date time", datetime);
    results = jiraCreateUpdateState.validateFields();
    assertThat(results).isEmpty();

    date = new JiraCustomFieldValue("date", "current() + 123456789");
    datetime = new JiraCustomFieldValue("datetime", "${test.var1}+123456789");
    customFieldValueMap.put("due date", date);
    customFieldValueMap.put("start date time", datetime);
    results = jiraCreateUpdateState.validateFields();
    assertThat(results).isEmpty();

    date = new JiraCustomFieldValue("date", "Current() + 123456789");
    datetime = new JiraCustomFieldValue("datetime", "${test.var1}+123456789");
    customFieldValueMap.put("due date", date);
    customFieldValueMap.put("start date time", datetime);
    results = jiraCreateUpdateState.validateFields();
    assertThat(results).isEmpty();

    date = new JiraCustomFieldValue("date", "CURRENT() + 123456789");
    datetime = new JiraCustomFieldValue("datetime", "${test.Var1}+123456789");
    customFieldValueMap.put("due date", date);
    customFieldValueMap.put("start date time", datetime);
    results = jiraCreateUpdateState.validateFields();
    assertThat(results).isEmpty();

    date = new JiraCustomFieldValue("date", "Current() + 123456789");
    datetime = new JiraCustomFieldValue("datetime", "${test.var 1}+123456789");
    customFieldValueMap.put("due date", date);
    customFieldValueMap.put("start date time", datetime);
    results = jiraCreateUpdateState.validateFields();
    assertThat(results).isNotEmpty();
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testParseDateTimeValue() {
    String fieldValue = "2014-03-03";
    String parsedVal = jiraCreateUpdateState.parseDateValue(fieldValue, context);
    assertThat(parsedVal).isEqualTo(fieldValue);

    fieldValue = "current() +172800000";
    parsedVal = jiraCreateUpdateState.parseDateValue(fieldValue, context);
    String expectedVal = new SimpleDateFormat("yyyy-MM-dd").format(new Date(System.currentTimeMillis() + 172800000));
    assertThat(parsedVal).isEqualTo(expectedVal);

    fieldValue = "Current() -172800000";
    parsedVal = jiraCreateUpdateState.parseDateValue(fieldValue, context);
    expectedVal = new SimpleDateFormat("yyyy-MM-dd").format(new Date(System.currentTimeMillis() - 172800000));
    assertThat(parsedVal).isEqualTo(expectedVal);

    fieldValue = "${workflow.variables.date}+172800000";
    when(context.renderExpression("${workflow.variables.date}")).thenReturn("2014-03-03");
    parsedVal = jiraCreateUpdateState.parseDateValue(fieldValue, context);
    assertThat(parsedVal).isEqualTo("2014-03-05");

    fieldValue = "${workflow.variables.date}-172800000";
    when(context.renderExpression("${workflow.variables.date}")).thenReturn("2014-03-03");
    parsedVal = jiraCreateUpdateState.parseDateValue(fieldValue, context);
    assertThat(parsedVal).isEqualTo("2014-03-01");

    fieldValue = "${workflow.variables.date}-172800000";
    when(context.renderExpression("${workflow.variables.date}")).thenReturn("1393785000000");
    parsedVal = jiraCreateUpdateState.parseDateValue(fieldValue, context);
    SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd");
    Date date = new Date(1393785000000L - 172800000L);
    assertThat(parsedVal).isEqualTo(f.format(date));

    fieldValue = "${workflow.variables.date} - ";
    when(context.renderExpression("${workflow.variables.date}")).thenReturn("2014-03-03");
    parsedVal = jiraCreateUpdateState.parseDateValue(fieldValue, context);
    assertThat(parsedVal).isEqualTo("2014-03-03");
  }

  @Test
  @Owner(developers = POOJA, intermittent = true)
  @Category(UnitTests.class)
  public void testParseDateValue() {
    String fieldValue = "1234567891011";
    String parsedVal = jiraCreateUpdateState.parseDateTimeValue(fieldValue, context);
    assertThat(parsedVal).isEqualTo(fieldValue);

    fieldValue = "current() +172800000";
    parsedVal = jiraCreateUpdateState.parseDateTimeValue(fieldValue, context);
    String expectedVal = String.valueOf(System.currentTimeMillis() + 172800000);
    assertThat(parsedVal).isEqualTo(expectedVal);

    fieldValue = "current() -172800000";
    parsedVal = jiraCreateUpdateState.parseDateTimeValue(fieldValue, context);
    expectedVal = String.valueOf(System.currentTimeMillis() - 172800000);
    assertThat(parsedVal).isEqualTo(expectedVal);

    fieldValue = "${workflow.variables.date}+172800000";
    when(context.renderExpression("${workflow.variables.date}")).thenReturn("1234567891911");
    parsedVal = jiraCreateUpdateState.parseDateTimeValue(fieldValue, context);
    assertThat(parsedVal).isEqualTo(String.valueOf(1234567891911L + 172800000L));

    fieldValue = "${workflow.variables.Date}-172800000";
    when(context.renderExpression("${workflow.variables.Date}")).thenReturn("1234567891911");
    parsedVal = jiraCreateUpdateState.parseDateTimeValue(fieldValue, context);
    assertThat(parsedVal).isEqualTo(String.valueOf(1234567891911L - 172800000L));

    fieldValue = "${workflow.variables.date} -";
    when(context.renderExpression("${workflow.variables.date}")).thenReturn("2010-01-29T12:20:00Z");
    parsedVal = jiraCreateUpdateState.parseDateTimeValue(fieldValue, context);
    assertThat(parsedVal).isEqualTo(String.valueOf(1264767600000L));
  }

  @Test
  @Owner(developers = POOJA)
  @Category(UnitTests.class)
  public void testParseDateTimeZoneValue() {
    String fieldValue = "${workflow.variables.date}";
    when(context.renderExpression("${workflow.variables.date}")).thenReturn("2010-01-29T 00:20:00-0800");
    String parsedVal = jiraCreateUpdateState.parseDateTimeValue(fieldValue, context);
    assertThat(parsedVal).isEqualTo(String.valueOf(1264753200000L));

    fieldValue = "${workflow.variables.date}";
    when(context.renderExpression("${workflow.variables.date}")).thenReturn("2010-01-29T00:20:00+0530");
    parsedVal = jiraCreateUpdateState.parseDateTimeValue(fieldValue, context);
    assertThat(parsedVal).isEqualTo(String.valueOf(1264704600000L));

    fieldValue = "${workflow.variables.date}";
    when(context.renderExpression("${workflow.variables.date}")).thenReturn("2010-01-29T00:20:00+0000");
    parsedVal = jiraCreateUpdateState.parseDateTimeValue(fieldValue, context);
    assertThat(parsedVal).isEqualTo(String.valueOf(1264724400000L));

    fieldValue = "${workflow.variables.date}";
    when(context.renderExpression("${workflow.variables.date}")).thenReturn("2010-01-29T00:20:00Z");
    parsedVal = jiraCreateUpdateState.parseDateTimeValue(fieldValue, context);
    assertThat(parsedVal).isEqualTo(String.valueOf(1264724400000L));
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnCustomFieldIdsToNamesMap() {
    jiraCreateUpdateState.setProject("PN");
    jiraCreateUpdateState.setIssueType("Issue Type");
    Map<String, String> map = new HashMap<>();
    map.put("multi", "Multiselect");
    map.put("customfield_option", "Option Name");
    map.put("customfield_option_2", "Option Name");
    map.put("customfield_option_number", "Number");

    assertThat(jiraCreateUpdateState.mapCustomFieldsIdsToNames(createMetaResponse)).isEqualTo(map);
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnCustomFieldsValuesToIdMap() {
    jiraCreateUpdateState.setProject("PN");
    jiraCreateUpdateState.setIssueType("Issue Type");
    Map<String, Map<String, String>> map = new HashMap<>();
    Map<String, String> multi = new HashMap<>();
    multi.put("multiselectname1", "multiselect_id_1");
    multi.put("multiselectname2", "multiselect_id_2");
    Map<String, String> option = new HashMap<>();
    option.put("optionvalue1", "option_id_1");
    option.put("optionvalue2", "option_id_2");
    Map<String, String> option2 = new HashMap<>();
    option2.put("optionvalue1", "option_id_1");
    option2.put("optionvalue2", "option_id_2");
    map.put("multi", multi);
    map.put("customfield_option", option);
    map.put("customfield_option_2", option2);

    assertThat(jiraCreateUpdateState.mapCustomFieldsValuesToId(createMetaResponse)).isEqualTo(map);
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldResolveCustomOptionFieldsVars() {
    jiraCreateUpdateState.setProject("PN");
    jiraCreateUpdateState.setIssueType("Issue Type");
    Map<String, JiraCustomFieldValue> customFields = new HashMap<>();
    JiraCustomFieldValue customFieldOptionValue = new JiraCustomFieldValue();
    customFieldOptionValue.setFieldType("option");
    customFieldOptionValue.setFieldValue("OptionValue2");
    customFields.put("customfield_option", customFieldOptionValue);
    jiraCreateUpdateState.setCustomFields(customFields);

    jiraCreateUpdateState.resolveCustomFieldsVars(jiraCreateUpdateState.mapCustomFieldsIdsToNames(createMetaResponse),
        jiraCreateUpdateState.mapCustomFieldsValuesToId(createMetaResponse));
    String resolvedExpression = jiraCreateUpdateState.getCustomFieldsMap().get("customfield_option").getFieldValue();

    assertThat(resolvedExpression).isEqualTo("option_id_2");
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldResolveCustomMultiFieldsVars() {
    jiraCreateUpdateState.setProject("PN");
    jiraCreateUpdateState.setIssueType("Issue Type");
    Map<String, JiraCustomFieldValue> customFields = new HashMap<>();
    JiraCustomFieldValue customFieldMultiValue = new JiraCustomFieldValue();
    customFieldMultiValue.setFieldType("multiselect");
    customFieldMultiValue.setFieldValue("MultiselectName1");
    customFields.put("multi", customFieldMultiValue);
    jiraCreateUpdateState.setCustomFields(customFields);

    jiraCreateUpdateState.resolveCustomFieldsVars(jiraCreateUpdateState.mapCustomFieldsIdsToNames(createMetaResponse),
        jiraCreateUpdateState.mapCustomFieldsValuesToId(createMetaResponse));
    String resolvedExpression = jiraCreateUpdateState.getCustomFieldsMap().get("multi").getFieldValue();

    assertThat(resolvedExpression).isEqualTo("multiselect_id_1");
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldResolveMultipleCustomMultiFieldsVars() {
    jiraCreateUpdateState.setProject("PN");
    jiraCreateUpdateState.setIssueType("Issue Type");
    Map<String, JiraCustomFieldValue> customFields = new HashMap<>();
    JiraCustomFieldValue multipleCustomFieldMultiValue = new JiraCustomFieldValue();
    multipleCustomFieldMultiValue.setFieldType("multiselect");
    multipleCustomFieldMultiValue.setFieldValue("MultiselectName1,  multiselectName2 ");
    customFields.put("multi", multipleCustomFieldMultiValue);
    jiraCreateUpdateState.setCustomFields(customFields);

    jiraCreateUpdateState.resolveCustomFieldsVars(jiraCreateUpdateState.mapCustomFieldsIdsToNames(createMetaResponse),
        jiraCreateUpdateState.mapCustomFieldsValuesToId(createMetaResponse));
    String resolvedExpression = jiraCreateUpdateState.getCustomFieldsMap().get("multi").getFieldValue();

    assertThat(resolvedExpression).isEqualTo("multiselect_id_1,multiselect_id_2");
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldIgnoreNumberFieldTypeWhenResolvingCustomFieldVars() {
    jiraCreateUpdateState.setProject("PN");
    jiraCreateUpdateState.setIssueType("Issue Type");
    Map<String, JiraCustomFieldValue> customFields = new HashMap<>();
    JiraCustomFieldValue numberTypeField = new JiraCustomFieldValue();
    numberTypeField.setFieldType("number");
    numberTypeField.setFieldValue("3");
    customFields.put("customfield_option_number", numberTypeField);
    jiraCreateUpdateState.setCustomFields(customFields);

    assertThatCode(()
                       -> jiraCreateUpdateState.resolveCustomFieldsVars(
                           jiraCreateUpdateState.mapCustomFieldsIdsToNames(createMetaResponse),
                           jiraCreateUpdateState.mapCustomFieldsValuesToId(createMetaResponse)))
        .doesNotThrowAnyException();
  }

  @Test(expected = HarnessJiraException.class)
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldThrowExceptionWhenProvidedValueIsNotAllowed() {
    jiraCreateUpdateState.setProject("PN");
    jiraCreateUpdateState.setIssueType("Issue Type");
    Map<String, JiraCustomFieldValue> customFields = new HashMap<>();
    JiraCustomFieldValue multipleCustomFieldMultiValue = new JiraCustomFieldValue();
    multipleCustomFieldMultiValue.setFieldType("multiselect");
    multipleCustomFieldMultiValue.setFieldValue("Not Allowed");
    customFields.put("multi", multipleCustomFieldMultiValue);
    jiraCreateUpdateState.setCustomFields(customFields);

    jiraCreateUpdateState.resolveCustomFieldsVars(jiraCreateUpdateState.mapCustomFieldsIdsToNames(createMetaResponse),
        jiraCreateUpdateState.mapCustomFieldsValuesToId(createMetaResponse));
  }

  @Test(expected = HarnessJiraException.class)
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldFailProjectValidation() {
    jiraCreateUpdateState.setProject("UNKNOWN");
    jiraCreateUpdateState.setStatus("To Do");
    jiraCreateUpdateState.setIssueType("Issue Type");
    jiraCreateUpdateState.setPriority("High");

    jiraCreateUpdateState.validateRequiredFields(createMetaResponse, context);
  }

  @Test(expected = HarnessJiraException.class)
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldFailStatusValidation() {
    jiraCreateUpdateState.setProject("PN");
    jiraCreateUpdateState.setStatus("UNKNOWN");
    jiraCreateUpdateState.setIssueType("Issue Type");
    jiraCreateUpdateState.setPriority("High");

    jiraCreateUpdateState.validateRequiredFields(createMetaResponse, context);
  }

  @Test(expected = HarnessJiraException.class)
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldFailIssueTypeValidation() {
    jiraCreateUpdateState.setProject("PN");
    jiraCreateUpdateState.setStatus("To Do");
    jiraCreateUpdateState.setIssueType("UNKNOWN");
    jiraCreateUpdateState.setPriority("High");

    jiraCreateUpdateState.validateRequiredFields(createMetaResponse, context);
  }

  @Test(expected = HarnessJiraException.class)
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldFailPriorityValidation() {
    jiraCreateUpdateState.setProject("PN");
    jiraCreateUpdateState.setStatus("To Do");
    jiraCreateUpdateState.setIssueType("Issue Type");
    jiraCreateUpdateState.setPriority("UNKNOWN");

    jiraCreateUpdateState.validateRequiredFields(createMetaResponse, context);
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldPassRequiredFieldsValidation() {
    jiraCreateUpdateState.setProject("PN");
    jiraCreateUpdateState.setStatus("To Do");
    jiraCreateUpdateState.setIssueType("Issue Type");
    jiraCreateUpdateState.setPriority("High");

    jiraCreateUpdateState.validateRequiredFields(createMetaResponse, context);
  }

  @Test(expected = HarnessJiraException.class)
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldFailCustomFieldsNameValidation() {
    jiraCreateUpdateState.setProject("PN");
    jiraCreateUpdateState.setIssueType("Issue Type");

    Map<String, JiraCustomFieldValue> customFields = new HashMap<>();
    JiraCustomFieldValue customFieldValue = new JiraCustomFieldValue();
    customFieldValue.setFieldValue("Some Value");
    customFields.put("UNKNOWN", customFieldValue);
    jiraCreateUpdateState.setCustomFields(customFields);

    jiraCreateUpdateState.inferCustomFieldsTypes(createMetaResponse);
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldInferCustomFieldType() {
    jiraCreateUpdateState.setProject("PN");
    jiraCreateUpdateState.setIssueType("Issue Type");

    Map<String, JiraCustomFieldValue> customFields = new HashMap<>();
    JiraCustomFieldValue customFieldValue = new JiraCustomFieldValue();
    customFieldValue.setFieldValue("MultiselectName1");
    customFields.put("Multiselect", customFieldValue);
    jiraCreateUpdateState.setCustomFields(customFields);

    jiraCreateUpdateState.inferCustomFieldsTypes(createMetaResponse);
    assertThat(jiraCreateUpdateState.getCustomFieldsMap().get("multi").getFieldType()).isEqualTo("multiselect");
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldInferTypeForTimetrackingField() {
    jiraCreateUpdateState.setProject("PN");
    jiraCreateUpdateState.setIssueType("Issue Type");

    Map<String, JiraCustomFieldValue> customFields = new HashMap<>();
    JiraCustomFieldValue customFieldValue = new JiraCustomFieldValue();
    customFieldValue.setFieldValue("1d 4h");
    customFields.put("TimeTracking:OriginalEstimate", customFieldValue);
    jiraCreateUpdateState.setCustomFields(customFields);

    jiraCreateUpdateState.inferCustomFieldsTypes(createMetaResponse);
    assertThat(jiraCreateUpdateState.getCustomFieldsMap().get("TimeTracking:OriginalEstimate").getFieldType())
        .isEqualTo("timetracking");
  }

  @Test(expected = HarnessJiraException.class)
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldFailProcessingIfProvidedCustomFieldHasDuplicatesByName() {
    jiraCreateUpdateState.setProject("PN");
    jiraCreateUpdateState.setIssueType("Issue Type");

    Map<String, JiraCustomFieldValue> customFields = new HashMap<>();
    JiraCustomFieldValue customFieldValue = new JiraCustomFieldValue();
    customFields.put("Option Name", customFieldValue);
    jiraCreateUpdateState.setCustomFields(customFields);

    jiraCreateUpdateState.inferCustomFieldsTypes(createMetaResponse);
  }

  @Test
  @Owner(developers = PRABU)
  @Category(UnitTests.class)
  public void shouldThrowExceptionIfConnectorIsOutOfAccountScope() {
    jiraCreateUpdateState.setJiraConnectorId(JIRA_CONNECTOR_ID);
    jiraCreateUpdateState.setIssueType("Issue Type");

    when(context.fetchRequiredApp()).thenReturn(Application.Builder.anApplication().name(APP_NAME).build());
    when(context.fetchRequiredEnvironment()).thenReturn(Environment.Builder.anEnvironment().build());
    when(settingsService.getByAccountAndId(ACCOUNT_ID, JIRA_CONNECTOR_ID)).thenReturn(null);
    when(activityService.save(any())).thenReturn(Activity.builder().uuid(ACTIVITY_ID).build());
    assertThatThrownBy(() -> jiraCreateUpdateState.execute(context))
        .isInstanceOf(GeneralException.class)
        .hasMessage("Jira connector doesn't exist");
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldThrowInvalidRequestExceptionWhenInvalidArgumentIsPassedForNumberFieldType() {
    String fieldValue = "wrongFormatForNumberType";
    String fieldName = "fieldName";
    when(context.renderExpression(fieldValue)).thenReturn(fieldValue);

    assertThatThrownBy(() -> jiraCreateUpdateState.parseNumberValue(fieldValue, context, fieldName))
        .isInstanceOf(InvalidRequestException.class)
        .hasMessage("Invalid value provided for field: fieldName. fieldName field is of type 'number'.");
  }

  @Test
  @Owner(developers = AGORODETKI)
  @Category(UnitTests.class)
  public void shouldReturnParsedNumericValueConvertedBackToString() {
    String fieldValue = "10";
    String fieldName = "fieldName";
    when(context.renderExpression(fieldValue)).thenReturn(fieldValue);
    String numericValue = jiraCreateUpdateState.parseNumberValue(fieldValue, context, fieldName);
    assertThat(Double.parseDouble(numericValue)).isEqualTo(Double.parseDouble(fieldValue));
  }
}
