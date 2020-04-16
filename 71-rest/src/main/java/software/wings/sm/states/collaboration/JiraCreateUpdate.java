package software.wings.sm.states.collaboration;

import static io.harness.beans.ExecutionStatus.FAILED;
import static io.harness.beans.OrchestrationWorkflowType.BUILD;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.validation.Validator.notNullCheck;
import static software.wings.beans.Environment.EnvironmentType.ALL;
import static software.wings.beans.Environment.GLOBAL_ENV_ID;
import static software.wings.beans.TaskType.JIRA;

import com.google.inject.Inject;

import io.harness.beans.DelegateTask;
import io.harness.beans.ExecutionStatus;
import io.harness.beans.SweepingOutputInstance;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.ResponseData;
import io.harness.delegate.beans.TaskData;
import io.harness.exception.InvalidRequestException;
import io.harness.expression.ExpressionEvaluator;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.jira.JiraExecutionData;
import software.wings.beans.Activity;
import software.wings.beans.Activity.ActivityBuilder;
import software.wings.beans.Activity.Type;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.JiraConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.jira.JiraCustomFieldValue;
import software.wings.beans.jira.JiraTaskParameters;
import software.wings.delegatetasks.jira.JiraAction;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.DelegateServiceImpl;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.LogService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.service.intfc.sweepingoutput.SweepingOutputService;
import software.wings.sm.ExecutionContext;
import software.wings.sm.ExecutionContextImpl;
import software.wings.sm.ExecutionResponse;
import software.wings.sm.State;
import software.wings.sm.StateType;
import software.wings.sm.states.mixin.SweepingOutputStateMixin;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;

@Slf4j
@FieldNameConstants(innerTypeName = "JiraCreateUpdateKeys")
public class JiraCreateUpdate extends State implements SweepingOutputStateMixin {
  public static final String DATE_ISO_FORMAT = "yyyy-MM-dd";
  private static final long JIRA_TASK_TIMEOUT_MILLIS = 60 * 1000;
  private static final String JIRA_ISSUE_ID = "issueId";
  private static final String JIRA_ISSUE_KEY = "issueKey";
  private static final String JIRA_ISSUE = "issue";
  public static final String DATETIME_ISO_FORMAT = "yyyy-MM-dd'T'HH:mm:ssXX";

  @Inject private transient ActivityService activityService;
  @Inject @Transient private LogService logService;

  @Inject @Transient private transient WingsPersistence wingsPersistence;
  @Inject @Transient private DelegateServiceImpl delegateService;
  @Inject @Transient private transient SecretManager secretManager;
  @Inject @Transient private SweepingOutputService sweepingOutputService;

  @Getter @Setter @NotNull private JiraAction jiraAction;
  @Getter @Setter @NotNull String jiraConnectorId;
  @Getter @Setter @NotNull private String project;
  @Getter @Setter private String issueType;
  @Getter @Setter private String priority;
  @Getter @Setter private List<String> labels;
  @Getter @Setter private String summary;
  @Getter @Setter private String description;
  @Getter @Setter private String status;
  @Getter @Setter private String comment;
  @Getter @Setter private String issueId;
  private Map<String, JiraCustomFieldValue> customFields;
  private static final Pattern currentPattern =
      Pattern.compile("(current\\(\\))(\\s*([+-])\\s*(\\d{0,13}))*", Pattern.CASE_INSENSITIVE);
  private static final Pattern varPattern =
      Pattern.compile("^(\\$\\{[a-z\\d._]*})(\\s*([+-])\\s*(\\d{0,13}))?", Pattern.CASE_INSENSITIVE);
  private static final Pattern fixedDatePattern = Pattern.compile("\\d{4}-\\d{2}-\\d{2}");
  private static final Pattern fixedDatetimePattern = Pattern.compile("(\\d{13})");

  public Map<String, JiraCustomFieldValue> fetchCustomFields() {
    return customFields;
  }

  public void setCustomFields(Map<String, JiraCustomFieldValue> customFields) {
    this.customFields = customFields;
  }

  @Getter @Setter private SweepingOutputInstance.Scope sweepingOutputScope;
  @Getter @Setter private String sweepingOutputName;

  public JiraCreateUpdate(String name) {
    super(name, StateType.JIRA_CREATE_UPDATE.name());
  }

  @Override
  public ExecutionResponse execute(ExecutionContext context) {
    String activityId = createActivity(context);
    return executeInternal(context, activityId);
  }

  private ExecutionResponse executeInternal(ExecutionContext context, String activityId) {
    ExecutionContextImpl executionContext = (ExecutionContextImpl) context;

    JiraConfig jiraConfig = getJiraConfig(jiraConnectorId);
    if (isNotEmpty(customFields)) {
      for (Entry<String, JiraCustomFieldValue> customField : customFields.entrySet()) {
        JiraCustomFieldValue value = customField.getValue();
        if (value.getFieldType().equals("date")) {
          String parsedDateValue = parseDateValue(value.getFieldValue(), context);
          value.setFieldValue(parsedDateValue);
        }
        if (value.getFieldType().equals("datetime")) {
          String parsedDateTimeValue = parseDateTimeValue(value.getFieldValue(), context);
          value.setFieldValue(parsedDateTimeValue);
        }
      }
    }
    renderExpressions(context);

    if (ExpressionEvaluator.containsVariablePattern(issueId)) {
      return ExecutionResponse.builder()
          .executionStatus(FAILED)
          .errorMessage("Expression not rendered for issue Id: " + issueId)
          .stateExecutionData(JiraExecutionData.builder().activityId(activityId).build())
          .build();
    }

    JiraTaskParameters parameters = JiraTaskParameters.builder()
                                        .jiraConfig(jiraConfig)
                                        .jiraAction(jiraAction)
                                        .issueId(issueId)
                                        .project(project)
                                        .issueType(issueType)
                                        .summary(summary)
                                        .status(status)
                                        .description(description)
                                        .labels(labels)
                                        .customFields(customFields)
                                        .comment(comment)
                                        .priority(priority)
                                        .encryptionDetails(secretManager.getEncryptionDetails(jiraConfig,
                                            executionContext.getAppId(), executionContext.getWorkflowExecutionId()))
                                        .accountId(((ExecutionContextImpl) context).getApp().getAccountId())
                                        .activityId(activityId)
                                        .appId(context.getAppId())
                                        .build();

    if (jiraAction == JiraAction.UPDATE_TICKET) {
      List<String> issueIds = parseExpression(issueId);
      if (EmptyPredicate.isEmpty(issueIds)) {
        return ExecutionResponse.builder()
            .executionStatus(FAILED)
            .errorMessage("No valid issueId after parsing: " + issueId)
            .stateExecutionData(JiraExecutionData.builder().activityId(activityId).build())
            .build();
      }
      parameters.setUpdateIssueIds(issueIds);
    }

    DelegateTask delegateTask = DelegateTask.builder()
                                    .accountId(executionContext.getApp().getAccountId())
                                    .waitId(activityId)
                                    .appId(((ExecutionContextImpl) context).getApp().getAppId())
                                    .data(TaskData.builder()
                                              .async(true)
                                              .taskType(JIRA.name())
                                              .parameters(new Object[] {parameters})
                                              .timeout(JIRA_TASK_TIMEOUT_MILLIS)
                                              .build())
                                    .build();
    String delegateTaskId = delegateService.queueTask(delegateTask);

    return ExecutionResponse.builder()
        .async(true)
        .correlationIds(Collections.singletonList(activityId))
        .delegateTaskId(delegateTaskId)
        .stateExecutionData(JiraExecutionData.builder().activityId(activityId).build())
        .build();
  }

  String parseDateTimeValue(String fieldValue, ExecutionContext context) {
    Matcher matcher = fixedDatetimePattern.matcher(fieldValue);
    if (matcher.matches()) {
      return fieldValue;
    }
    long val1;
    matcher = currentPattern.matcher(fieldValue);
    if (matcher.matches()) {
      val1 = System.currentTimeMillis();
      return getDateTime(matcher, val1);
    }
    matcher = varPattern.matcher(fieldValue);
    if (matcher.matches()) {
      String renderedVal = context.renderExpression(matcher.group(1));
      renderedVal = renderedVal.replaceAll("\\s+", "");
      try {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATETIME_ISO_FORMAT);
        val1 = dateFormat.parse(renderedVal).getTime();
      } catch (ParseException e) {
        try {
          val1 = Long.parseLong(renderedVal);
        } catch (NumberFormatException ne) {
          throw new InvalidRequestException("Cannot parse date time value from " + renderedVal, ne, USER);
        }
      }
      return getDateTime(matcher, val1);
    } else {
      throw new InvalidRequestException("Cannot parse date time value from " + fieldValue, USER);
    }
  }

  private String getDateTime(Matcher matcher, Long val1) {
    long val2;
    String group4 = matcher.group(4);
    if (isNotEmpty(group4)) {
      val2 = Long.parseLong(group4);
      if (matcher.group(3).equals("+")) {
        return String.valueOf(val1 + val2);
      } else {
        return String.valueOf(val1 - val2);
      }
    } else {
      return String.valueOf(val1);
    }
  }

  String parseDateValue(String fieldValue, ExecutionContext context) {
    Matcher matcher = fixedDatePattern.matcher(fieldValue);
    if (matcher.matches()) {
      return fieldValue;
    }
    long val1;
    matcher = currentPattern.matcher(fieldValue);
    if (matcher.matches()) {
      val1 = System.currentTimeMillis();
      return getDateValue(matcher, val1);
    }
    matcher = varPattern.matcher(fieldValue);
    if (matcher.matches()) {
      String renderedVal = context.renderExpression(matcher.group(1));
      renderedVal = renderedVal.replaceAll("\\s+", "");
      try {
        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_ISO_FORMAT);
        val1 = dateFormat.parse(renderedVal).getTime();
        return getDateValue(matcher, val1);
      } catch (ParseException e) {
        try {
          val1 = Long.parseLong(renderedVal);
          return getDateValue(matcher, val1);
        } catch (NumberFormatException ne) {
          throw new InvalidRequestException("Cannot parse date value from " + renderedVal, ne, USER);
        }
      }
    }
    throw new InvalidRequestException("Cannot parse date value from " + fieldValue, USER);
  }

  private String getDateValue(Matcher matcher, Long val1) {
    long val2;
    String group4 = matcher.group(4);
    if (isNotEmpty(group4)) {
      val2 = Long.parseLong(group4);
      if (matcher.group(3).equals("+")) {
        return new SimpleDateFormat(DATE_ISO_FORMAT).format(new Date(val1 + val2));
      } else {
        return new SimpleDateFormat(DATE_ISO_FORMAT).format(new Date(val1 - val2));
      }
    } else {
      return new SimpleDateFormat(DATE_ISO_FORMAT).format(new Date(val1));
    }
  }

  private List<String> parseExpression(String issueId) {
    List<String> issueIds;
    issueId = issueId.replaceAll("[\\[\\](){}?:!.,;]+", " ").trim();
    issueIds = Arrays.asList(issueId.split(" "));
    return issueIds.stream().filter(EmptyPredicate::isNotEmpty).map(String::trim).collect(Collectors.toList());
  }

  private void renderExpressions(ExecutionContext context) {
    issueId = context.renderExpression(issueId);
    labels = context.renderExpressionList(labels);
    summary = context.renderExpression(summary);
    description = context.renderExpression(description);
    comment = context.renderExpression(comment);
    if (EmptyPredicate.isNotEmpty(customFields)) {
      Map<String, JiraCustomFieldValue> renderedCustomFields = new HashMap<>();
      customFields.forEach((key, value) -> {
        JiraCustomFieldValue rendered = new JiraCustomFieldValue();
        rendered.setFieldType(value.getFieldType());
        rendered.setFieldValue(context.renderExpression(value.getFieldValue()));
        renderedCustomFields.put(key, rendered);
      });
      customFields = renderedCustomFields;
    }
  }

  private JiraConfig getJiraConfig(String jiraConnectorId) {
    SettingAttribute jiraSettingAttribute = wingsPersistence.get(SettingAttribute.class, jiraConnectorId);
    notNullCheck("jiraSettingAttribute", jiraSettingAttribute);

    if (!(jiraSettingAttribute.getValue() instanceof JiraConfig)) {
      throw new InvalidRequestException("Type of Setting Attribute Value is not JiraConfig");
    }

    return (JiraConfig) jiraSettingAttribute.getValue();
  }

  private String createActivity(ExecutionContext executionContext) {
    Application app = ((ExecutionContextImpl) executionContext).fetchRequiredApp();

    ActivityBuilder activityBuilder = Activity.builder()
                                          .applicationName(app.getName())
                                          .commandName(getName())
                                          .type(Type.Command)
                                          .workflowType(executionContext.getWorkflowType())
                                          .workflowExecutionName(executionContext.getWorkflowExecutionName())
                                          .stateExecutionInstanceId(executionContext.getStateExecutionInstanceId())
                                          .stateExecutionInstanceName(executionContext.getStateExecutionInstanceName())
                                          .commandType(getStateType())
                                          .workflowExecutionId(executionContext.getWorkflowExecutionId())
                                          .workflowId(executionContext.getWorkflowId())
                                          .commandUnits(Arrays.asList())
                                          .status(ExecutionStatus.RUNNING);

    if (executionContext.getOrchestrationWorkflowType() != null
        && executionContext.getOrchestrationWorkflowType() == BUILD) {
      activityBuilder.environmentId(GLOBAL_ENV_ID).environmentName(GLOBAL_ENV_ID).environmentType(ALL);
    } else {
      Environment env = ((ExecutionContextImpl) executionContext).fetchRequiredEnvironment();
      activityBuilder.environmentId(env.getUuid())
          .environmentName(env.getName())
          .environmentType(env.getEnvironmentType());
    }

    Activity activity = activityBuilder.build();
    activity.setAppId(app.getUuid());

    return activityService.save(activity).getUuid();
  }

  @Override
  public ExecutionResponse handleAsyncResponse(ExecutionContext context, Map<String, ResponseData> response) {
    Entry<String, ResponseData> responseEntry = response.entrySet().iterator().next();
    String activityId = responseEntry.getKey();

    JiraExecutionData jiraExecutionData = (JiraExecutionData) responseEntry.getValue();
    jiraExecutionData.setActivityId(activityId);

    if (jiraExecutionData.getExecutionStatus() == ExecutionStatus.SUCCESS) {
      Map<String, Object> sweepingOutputMap = new HashMap<>();
      sweepingOutputMap.put(JIRA_ISSUE_ID, jiraExecutionData.getIssueId());
      sweepingOutputMap.put(JIRA_ISSUE_KEY, jiraExecutionData.getIssueKey());
      sweepingOutputMap.put(JIRA_ISSUE, jiraExecutionData.getJiraIssueData());
      handleSweepingOutput(sweepingOutputService, context, sweepingOutputMap);
    }

    return ExecutionResponse.builder()
        .stateExecutionData(jiraExecutionData)
        .executionStatus(jiraExecutionData.getExecutionStatus())
        .errorMessage(jiraExecutionData.getErrorMessage())
        .build();
  }

  @Override
  public void handleAbortEvent(ExecutionContext context) {}

  @Override
  public Map<String, String> validateFields() {
    Map<String, String> results = new HashMap<>();
    if (isEmpty(jiraConnectorId) || isEmpty(project) || isEmpty(issueType)) {
      logger.info("Connector Id not present in Jira State");
      results.put("Required Fields missing", "Connector, Project and IssueType must be provided.");
      return results;
    }
    if (isNotEmpty(customFields)) {
      for (Entry<String, JiraCustomFieldValue> customField : customFields.entrySet()) {
        if (customField.getValue() == null) {
          logger.info("Field value null for a custom field selected: " + customField.getKey());
          results.put("Field value missing", "Value must be provided for " + customField.getKey());
          continue;
        }
        JiraCustomFieldValue value = customField.getValue();
        if (value.getFieldType() == null) {
          logger.info("Field Type null for a custom field selected: " + customField.getKey());
          results.put("Field Type missing", "Type must be provided for " + customField.getKey());
          continue;
        }
        if (value.getFieldType().equals("datetime") || value.getFieldType().equals("date")) {
          String fieldValue = value.getFieldValue();
          if (!validateDateFieldValue(fieldValue, value.getFieldType())) {
            logger.info("Field value not valid for a custom field selected: {} {} ", customField.getKey(),
                customField.getValue().getFieldValue());
            results.put("Invalid field value", "Value provided for " + customField.getKey() + " is not valid");
          }
        }
      }
    }

    logger.info("Jira State Validated");
    return results;
  }

  private boolean validateDateFieldValue(String fieldValue, String fieldType) {
    Matcher matcher = null;
    if (fieldType.equals("date")) {
      matcher = fixedDatePattern.matcher(fieldValue);
    } else {
      matcher = fixedDatetimePattern.matcher(fieldValue);
    }

    if (matcher.matches()) {
      return true;
    }
    matcher = currentPattern.matcher(fieldValue);
    if (matcher.matches()) {
      return true;
    }
    matcher = varPattern.matcher(fieldValue);
    return matcher.matches();
  }
}
