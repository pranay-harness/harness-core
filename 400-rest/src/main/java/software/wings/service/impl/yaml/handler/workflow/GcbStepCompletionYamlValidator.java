package software.wings.service.impl.yaml.handler.workflow;

import static io.harness.validation.Validator.notNullCheck;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.beans.SweepingOutputInstance;
import io.harness.expression.ExpressionEvaluator;
import io.harness.serializer.JsonUtils;

import software.wings.beans.GitConfig;
import software.wings.beans.NameValuePair;
import software.wings.beans.SettingAttribute;
import software.wings.beans.yaml.ChangeContext;
import software.wings.exception.IncompleteStateException;
import software.wings.service.intfc.SettingsService;
import software.wings.sm.states.gcbconfigs.GcbOptions;
import software.wings.sm.states.gcbconfigs.GcbTriggerBuildSpec;
import software.wings.yaml.workflow.StepYaml;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class GcbStepCompletionYamlValidator implements StepCompletionYamlValidator {
  @Inject private SettingsService settingsService;

  private static final String SWEEPING_OUTPUT_SCOPE = "sweepingOutputScope";
  private static final String SWEEPING_OUTPUT_NAME = "sweepingOutputName";
  private static final String TEMPLATE_EXPRESSIONS = "templateExpressions";

  @Override
  public void validate(ChangeContext<StepYaml> changeContext) {
    StepYaml stepYaml = changeContext.getYaml();
    if (stepYaml.getProperties().get("gcbOptions") == null) {
      throw new IncompleteStateException("Google Cloud Build step is incomplete. Please, provide gcbOptions");
    } else {
      validateGcbOptions(changeContext,
          JsonUtils.asObject(JsonUtils.asJson(stepYaml.getProperties().get("gcbOptions"))
                                 .replace("gcpConfigName", "gcpConfigId")
                                 .replace("gitConfigName", "gitConfigId"),
              GcbOptions.class));
      validateTimeout(stepYaml);
      validateSweepingOutput(stepYaml);
    }
  }

  private void validateSweepingOutput(StepYaml stepYaml) {
    if (!isBlank((String) stepYaml.getProperties().get(SWEEPING_OUTPUT_NAME))
        && (isBlank((String) stepYaml.getProperties().get(SWEEPING_OUTPUT_SCOPE)))) {
      throw new IncompleteStateException(
          "\"sweepingOutputScope\" could not be null or empty. Please, provide value (PIPELINE, WORKFLOW, PHASE, STATE)");
    } else if (isBlank((String) stepYaml.getProperties().get(SWEEPING_OUTPUT_NAME))
        && (!isBlank((String) stepYaml.getProperties().get(SWEEPING_OUTPUT_SCOPE)))) {
      throw new IncompleteStateException("\"sweepingOutputName\" could not be null or empty. Please, provide value");
    } else if (!isBlank((String) stepYaml.getProperties().get(SWEEPING_OUTPUT_NAME))
        && (!isBlank((String) stepYaml.getProperties().get(SWEEPING_OUTPUT_SCOPE)))) {
      try {
        SweepingOutputInstance.Scope.valueOf((String) stepYaml.getProperties().get(SWEEPING_OUTPUT_SCOPE));
      } catch (IllegalArgumentException e) {
        log.error("Invalid sweepingOutputScope", e);
        throw new IncompleteStateException(
            "Invalid value for \"sweepingOutputScope\". Please, provide value (PIPELINE, WORKFLOW, PHASE, STATE)");
      }
    }
  }

  private void validateTimeout(StepYaml stepYaml) {
    if (!(stepYaml.getProperties().get("timeoutMillis") instanceof Integer)
        || ((Integer) stepYaml.getProperties().get("timeoutMillis")) < 1000) {
      throw new IncompleteStateException(
          "\"timeoutMillis\" could not be empty or less than 1000 . Please, provide the corresponding numeric value");
    }
  }

  private void validateGcbOptions(ChangeContext<StepYaml> changeContext, GcbOptions gcbOptions) {
    StepYaml stepYaml = changeContext.getYaml();

    List<String> templatizedFields = stepYaml.getProperties().get(TEMPLATE_EXPRESSIONS) == null
        ? Collections.emptyList()
        : ((List<Map<String, Object>>) stepYaml.getProperties().get(TEMPLATE_EXPRESSIONS))
              .stream()
              .map(map -> (String) map.get("fieldName"))
              .collect(Collectors.toList());
    if (isBlank(gcbOptions.getGcpConfigId()) && !templatizedFields.contains("gcpConfigId")) {
      throw new IncompleteStateException(
          "\"gcpConfigName\" could not be empty or null. Please, provide gcpConfigName or add templateExpression");
    }
    if (!templatizedFields.isEmpty()) {
      ((List<Map<String, Object>>) stepYaml.getProperties().get(TEMPLATE_EXPRESSIONS))
          .stream()
          .map(map -> (String) map.get("expression"))
          .forEach(expression -> {
            if (!ExpressionEvaluator.containsVariablePattern(expression)) {
              throw new IncompleteStateException("Invalid template expression.");
            }
          });
    }

    if (gcbOptions.getSpecSource() != null) {
      switch (gcbOptions.getSpecSource()) {
        case INLINE:
          validateInlineSpec(gcbOptions);
          break;
        case REMOTE:
          validateRemoteSpec(changeContext, templatizedFields, gcbOptions);
          break;
        case TRIGGER:
          validateTriggerSpec(gcbOptions);
          break;
        default:
          throw new UnsupportedOperationException("Gcb option " + gcbOptions.getSpecSource() + " not supported");
      }
    } else {
      throw new IncompleteStateException(
          "gcbOptions are incomplete. Please, provide specSource (INLINE, REMOTE, TRIGGER)");
    }
  }

  private void validateInlineSpec(GcbOptions gcbOptions) {
    if (isBlank(gcbOptions.getInlineSpec())) {
      throw new IncompleteStateException(
          "\"inlineSpec\" could not be empty or null within INLINE specSource, please provide value");
    }
  }

  private void validateRemoteSpec(
      ChangeContext<StepYaml> changeContext, List<String> templatizedFields, GcbOptions gcbOptions) {
    if (gcbOptions.getRepositorySpec() == null) {
      throw new IncompleteStateException(
          "\"repositorySpec\" could not be empty or null within REMOTE specSource, please provide value");
    } else {
      if (isBlank(gcbOptions.getRepositorySpec().getGitConfigId()) && !templatizedFields.contains("gitConfigId")) {
        throw new IncompleteStateException(
            "\"gitConfigName\" could not be empty or null. Please, provide gitConfigName or add templateExpression");
      }
      if (gcbOptions.getRepositorySpec().getFileSource() == null) {
        throw new IncompleteStateException("\"fileSource\" could not be empty or null. Please, provide value");
      }
      if (isBlank(gcbOptions.getRepositorySpec().getFilePath())) {
        throw new IncompleteStateException("\"filePath\" could not be empty or null. Please, provide value");
      } else if (gcbOptions.getRepositorySpec().getFilePath().startsWith("${")
          && !ExpressionEvaluator.containsVariablePattern(gcbOptions.getRepositorySpec().getFilePath())) {
        throw new IncompleteStateException(
            "Invalid expression for \"filePath\". Please, provide value or valid expression");
      }
      if (isBlank(gcbOptions.getRepositorySpec().getSourceId())) {
        throw new IncompleteStateException("\"sourceId\" could not be empty or null. Please, provide value");
      } else if (gcbOptions.getRepositorySpec().getSourceId().startsWith("${")
          && !ExpressionEvaluator.containsVariablePattern(gcbOptions.getRepositorySpec().getSourceId())) {
        throw new IncompleteStateException(
            "Invalid expression for \"sourceId\". Please, provide value or valid expression");
      }

      // Check repoName for non-templatized Git connector
      if (!templatizedFields.contains("gitConfigId")) {
        SettingAttribute settingAttribute = settingsService.getSettingAttributeByName(
            changeContext.getChange().getAccountId(), gcbOptions.getRepositorySpec().getGitConfigId());
        notNullCheck("Git connector was not found", settingAttribute);

        GitConfig gitConfig = (GitConfig) settingAttribute.getValue();
        notNullCheck("Git configuration was not found", gitConfig);

        if (GitConfig.UrlType.ACCOUNT == gitConfig.getUrlType()) {
          if (isBlank(gcbOptions.getRepositorySpec().getRepoName())) {
            throw new IncompleteStateException("\"repoName\" could not be empty or null. Please, provide value");
          } else if (gcbOptions.getRepositorySpec().getRepoName().contains("${")
              && !ExpressionEvaluator.containsVariablePattern(gcbOptions.getRepositorySpec().getRepoName())) {
            throw new IncompleteStateException(
                "Invalid expression for \"repoName\". Please, provide value or valid expression");
          }
        }
      }
      // Check repoName for templatized Git connector
      else {
        if (!isBlank(gcbOptions.getRepositorySpec().getRepoName())
            && gcbOptions.getRepositorySpec().getRepoName().contains("${")
            && !ExpressionEvaluator.containsVariablePattern(gcbOptions.getRepositorySpec().getRepoName())) {
          throw new IncompleteStateException(
              "Invalid expression for \"repoName\". Please, provide value or valid expression");
        }
      }
    }
  }

  private void validateTriggerSpec(GcbOptions gcbOptions) {
    if (gcbOptions.getTriggerSpec() == null) {
      throw new IncompleteStateException(
          "\"triggerSpec\" could not be empty or null within TRIGGER specSource, please provide value");
    } else {
      if (gcbOptions.getTriggerSpec().getSource() == null) {
        throw new IncompleteStateException("\"source\" could not be empty or null. Please, provide value");
      }
      validateSubstitutions(gcbOptions.getTriggerSpec());
      if (isBlank(gcbOptions.getTriggerSpec().getName())) {
        throw new IncompleteStateException("\"name\" could not be empty or null. Please, provide value");
      } else if (gcbOptions.getTriggerSpec().getName().startsWith("${")
          && !ExpressionEvaluator.containsVariablePattern(gcbOptions.getTriggerSpec().getName())) {
        throw new IncompleteStateException(
            "Invalid expression for \"name\". Please, provide value or valid expression");
      }
      if (isBlank(gcbOptions.getTriggerSpec().getSourceId())) {
        throw new IncompleteStateException("\"sourceId\" could not be empty or null. Please, provide value");
      } else if (gcbOptions.getTriggerSpec().getSourceId().startsWith("${")
          && !ExpressionEvaluator.containsVariablePattern(gcbOptions.getTriggerSpec().getSourceId())) {
        throw new IncompleteStateException(
            "Invalid expression for \"sourceId\". Please, provide value or valid expression");
      }
    }
  }

  private void validateSubstitutions(GcbTriggerBuildSpec triggerSpec) {
    if (triggerSpec.getSubstitutions() != null) {
      for (NameValuePair pair : triggerSpec.getSubstitutions()) {
        if (isBlank(pair.getValue())) {
          throw new IncompleteStateException("value of substitution could not be empty or null. Please, provide value");
        } else if (pair.getValue().startsWith("${") && !ExpressionEvaluator.containsVariablePattern(pair.getValue())) {
          throw new IncompleteStateException(
              "Invalid expression for substitution value. Please, provide value or valid expression");
        }
      }
    }
  }
}
