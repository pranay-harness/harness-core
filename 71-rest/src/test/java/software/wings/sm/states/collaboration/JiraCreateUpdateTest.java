package software.wings.sm.states.collaboration;

import static io.harness.rule.OwnerRule.POOJA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.jira.JiraCustomFieldValue;
import software.wings.sm.ExecutionContextImpl;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class JiraCreateUpdateTest extends WingsBaseTest {
  @Mock private ExecutionContextImpl context;
  @InjectMocks JiraCreateUpdate jiraCreateUpdateState = new JiraCreateUpdate("Jira");

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
}