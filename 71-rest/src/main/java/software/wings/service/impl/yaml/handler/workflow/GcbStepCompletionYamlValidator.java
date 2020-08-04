package software.wings.service.impl.yaml.handler.workflow;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.serializer.JsonUtils;
import software.wings.exception.IncompleteStateException;
import software.wings.sm.states.gcbconfigs.GcbOptions;
import software.wings.yaml.workflow.StepYaml;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GcbStepCompletionYamlValidator implements StepCompletionYamlValidator {
  @Override
  public void validate(StepYaml stepYaml) {
    if (stepYaml.getProperties().get("gcbOptions") == null) {
      throw new IncompleteStateException("Google Cloud Build step is incomplete. Please, provide gcbOptions");
    } else {
      validateGcbOptions(stepYaml,
          JsonUtils.asObject(JsonUtils.asJson(stepYaml.getProperties().get("gcbOptions"))
                                 .replace("gcpConfigName", "gcpConfigId")
                                 .replace("gitConfigName", "gitConfigId"),
              GcbOptions.class));
    }
  }

  private void validateGcbOptions(StepYaml stepYaml, GcbOptions gcbOptions) {
    List<String> templatizedFields = ((List<Map<String, Object>>) stepYaml.getProperties().get("templateExpressions"))
                                         .stream()
                                         .map(map -> (String) map.get("fieldName"))
                                         .collect(Collectors.toList());
    if (isBlank(gcbOptions.getGcpConfigId()) && !templatizedFields.contains("gcpConfigId")) {
      throw new IncompleteStateException(
          "\"gcpConfigName\" could not be empty or null. Please, provide gcpConfigName or add templateExpression");
    }

    if (gcbOptions.getSpecSource() != null) {
      switch (gcbOptions.getSpecSource()) {
        case INLINE:
          validateInlineSpec(gcbOptions);
          break;
        case REMOTE:
          validateRemoteSpec(templatizedFields, gcbOptions);
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

  private void validateRemoteSpec(List<String> templatizedFields, GcbOptions gcbOptions) {
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
          && !isValidExpression(gcbOptions.getRepositorySpec().getFilePath())) {
        throw new IncompleteStateException(
            "Invalid expression for \"filePath\". Please, provide value or valid expression");
      }
      if (isBlank(gcbOptions.getRepositorySpec().getSourceId())) {
        throw new IncompleteStateException("\"sourceId\" could not be empty or null. Please, provide value");
      } else if (gcbOptions.getRepositorySpec().getSourceId().startsWith("${")
          && !isValidExpression(gcbOptions.getRepositorySpec().getSourceId())) {
        throw new IncompleteStateException(
            "Invalid expression for \"sourceId\". Please, provide value or valid expression");
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
      if (isBlank(gcbOptions.getTriggerSpec().getName())) {
        throw new IncompleteStateException("\"name\" could not be empty or null. Please, provide value");
      } else if (gcbOptions.getTriggerSpec().getName().startsWith("${")
          && !isValidExpression(gcbOptions.getTriggerSpec().getName())) {
        throw new IncompleteStateException(
            "Invalid expression for \"name\". Please, provide value or valid expression");
      }
      if (isBlank(gcbOptions.getTriggerSpec().getSourceId())) {
        throw new IncompleteStateException("\"sourceId\" could not be empty or null. Please, provide value");
      } else if (gcbOptions.getTriggerSpec().getSourceId().startsWith("${")
          && !isValidExpression(gcbOptions.getTriggerSpec().getSourceId())) {
        throw new IncompleteStateException(
            "Invalid expression for \"sourceId\". Please, provide value or valid expression");
      }
    }
  }

  private boolean isValidExpression(String expression) {
    return expression.matches("^\\$\\{\\w.+}");
  }
}
