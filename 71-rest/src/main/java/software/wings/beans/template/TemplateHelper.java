package software.wings.beans.template;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.govern.Switch.unhandled;
import static io.harness.persistence.HQuery.excludeValidate;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.Base.APP_ID_KEY;
import static software.wings.beans.EntityType.ARTIFACT_STREAM;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.EntityType.WORKFLOW;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.template.Template.FOLDER_ID_KEY;
import static software.wings.beans.template.Template.NAME_KEY;
import static software.wings.beans.template.Template.TYPE_KEY;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.data.structure.ListUtils;
import io.harness.exception.WingsException;
import io.harness.persistence.PersistentEntity;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.query.CountOptions;
import org.mongodb.morphia.query.Query;
import software.wings.beans.CommandCategory;
import software.wings.beans.EntityType;
import software.wings.beans.NameValuePair;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.command.CommandUnitType;
import software.wings.beans.command.ServiceCommand;
import software.wings.common.TemplateConstants;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.command.CommandHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Singleton
public class TemplateHelper {
  @Inject private WingsPersistence wingsPersistence;

  public Class<? extends PersistentEntity> lookupEntityClass(TemplateType templateType) {
    switch (templateType) {
      case SSH:
        return ServiceCommand.class;
      case HTTP:
      case SHELL_SCRIPT:
        return Workflow.class;
      case ARTIFACT_SOURCE:
        return ArtifactStream.class;
      default:
        unhandled(templateType);
    }
    return null;
  }

  public static EntityType mappedEntity(TemplateType templateType) {
    switch (templateType) {
      case SSH:
        return SERVICE;
      case HTTP:
      case SHELL_SCRIPT:
        return WORKFLOW;
      case ARTIFACT_SOURCE:
        return ARTIFACT_STREAM;
      default:
        unhandled(templateType);
    }
    return null;
  }

  public boolean templatesLinked(TemplateType templateType, List<String> templateUuids) {
    if (isEmpty(templateUuids)) {
      return false;
    }

    long templatesOfType = wingsPersistence.createQuery(Template.class)
                               .field(TYPE_KEY)
                               .equal(templateType.name())
                               .field(Template.ID_KEY)
                               .in(templateUuids)
                               .count(new CountOptions().limit(1));
    long linkedTemplates = wingsPersistence.createQuery(lookupEntityClass(templateType))
                               .field(lookupLinkedTemplateField(templateType))
                               .in(templateUuids)
                               .count(new CountOptions().limit(1));
    long templatesReferencedInOtherTemplates;
    if (templateType.equals(TemplateType.SSH)) {
      templatesReferencedInOtherTemplates =
          wingsPersistence.createQuery(VersionedTemplate.class, excludeValidate)
              .field("templateObject.referencedTemplateList.templateReference.templateUuid")
              .in(templateUuids)
              .count(new CountOptions().limit(1));
      return templatesOfType != 0 && (linkedTemplates != 0 || templatesReferencedInOtherTemplates != 0);
    }

    return templatesOfType != 0 && linkedTemplates != 0;
  }

  private String lookupLinkedTemplateField(TemplateType templateType) {
    switch (templateType) {
      case SSH:
        return ServiceCommand.TEMPLATE_UUID_KEY;
      case HTTP:
      case SHELL_SCRIPT:
        return Workflow.LINKED_TEMPLATE_UUIDS_KEY;
      case ARTIFACT_SOURCE:
        return ArtifactStream.TEMPLATE_UUID_KEY;
      default:
        unhandled(templateType);
    }
    return null;
  }

  public List<Variable> overrideVariables(List<Variable> templateVariables, List<Variable> existingVariables) {
    List<Variable> updatedVariables = new ArrayList<>();
    if (isNotEmpty(templateVariables)) {
      for (Variable variable : templateVariables) {
        updatedVariables.add(variable.cloneInternal());
      }
    }

    Map<String, Variable> oldVariablesMap = obtainVariableMap(existingVariables);
    for (Variable variable : updatedVariables) {
      if (oldVariablesMap.containsKey(variable.getName())) {
        // Do not override the value if it is from template
        Variable oldVariable = oldVariablesMap.get(variable.getName());

        if (isNotEmpty(oldVariable.getValue())) {
          variable.setValue(oldVariable.getValue());
        }
        if (isNotEmpty(oldVariable.getDescription())) {
          variable.setDescription(oldVariable.getDescription());
        }

        oldVariablesMap.remove(variable.getName());
      }
    }
    return updatedVariables;
  }

  public boolean variablesChanged(List<Variable> updatedVariables, List<Variable> existingVariables) {
    if (isEmpty(updatedVariables) && isEmpty(existingVariables)) {
      return false;
    } else if (isEmpty(updatedVariables) || isEmpty(existingVariables)) {
      return true;
    }
    Map<String, Variable> oldVariablesMap = obtainVariableMap(existingVariables);
    boolean variablesChanged = false;
    for (Variable variable : updatedVariables) {
      if (oldVariablesMap.containsKey(variable.getName())) {
        Variable oldVariable = oldVariablesMap.get(variable.getName());
        if (variable.getValue() != null) {
          if (!variable.getValue().equals(oldVariable.getValue())) {
            variablesChanged = true;
            break;
          }
        }
        if (variable.getDescription() != null) {
          if (!variable.getDescription().equals(oldVariable.getDescription())) {
            variablesChanged = true;
            break;
          }
        }
        oldVariablesMap.remove(variable.getName());
      } else {
        // New variable added check if any default value present
        variablesChanged = true;
        break;
      }
    }
    return variablesChanged || oldVariablesMap.size() != 0;
  }

  public boolean updateVariables(List<Variable> variables, List<Variable> oldVariables, boolean donotOverride) {
    if (isEmpty(variables) && isEmpty(oldVariables)) {
      return false;
    } else if (isEmpty(variables) || isEmpty(oldVariables)) {
      return true;
    }
    Map<String, Variable> oldVariablesMap = obtainVariableMap(oldVariables);
    boolean variablesChanged = false;
    for (Variable variable : variables) {
      if (oldVariablesMap.containsKey(variable.getName())) {
        // Do not override the value if it is from template
        Variable oldVariable = oldVariablesMap.get(variable.getName());
        if (donotOverride) {
          if (isNotEmpty(oldVariable.getValue())) {
            variable.setValue(oldVariable.getValue());
          }
          if (isNotEmpty(oldVariable.getDescription())) {
            variable.setDescription(oldVariable.getDescription());
          }
        }
        if (variable.getValue() != null) {
          if (!variable.getValue().equals(oldVariable.getValue())) {
            variablesChanged = true;
          }
        }
        oldVariablesMap.remove(variable.getName());
      } else {
        // New variable added check if any default value present
        variablesChanged = true;
      }
    }
    // Variable removed
    return variablesChanged || oldVariablesMap.size() != 0;
  }

  private Map<String, Variable> obtainVariableMap(List<Variable> variables) {
    if (isEmpty(variables)) {
      return new HashMap<>();
    }
    return variables.stream()
        .filter(variable -> variable.getName() != null && variable.getValue() != null)
        .collect(Collectors.toMap(Variable::getName, Function.identity()));
  }

  public static List<NameValuePair> convertToTemplateVariables(List<Variable> entityTemplateVariables) {
    List<NameValuePair> templateVariables = new ArrayList<>();
    if (isNotEmpty(entityTemplateVariables)) {
      entityTemplateVariables.forEach(variable
          -> templateVariables.add(
              NameValuePair.builder().name(variable.getName()).value(variable.getValue()).build()));
    }
    return templateVariables;
  }

  public static List<Variable> convertToEntityVariables(List<NameValuePair> entityTemplateVariables) {
    List<Variable> templateVariables = new ArrayList<>();
    if (isNotEmpty(entityTemplateVariables)) {
      entityTemplateVariables.forEach(
          variable -> templateVariables.add(aVariable().name(variable.getName()).value(variable.getValue()).build()));
    }
    return templateVariables;
  }

  public static String obtainTemplateVersion(String templateUri) {
    String[] templateUris = fetchTemplateUris(templateUri);
    if (templateUris.length == 1) {
      return TemplateConstants.LATEST_TAG;
    }
    return templateUris[1];
  }

  public static String obtainTemplateName(String templateUri) {
    if (templateUri.contains(":")) {
      String[] templateUris = fetchTemplateUris(templateUri);
      templateUri = templateUris[0];
    }
    return templateUri.substring(templateUri.lastIndexOf('/') + 1);
  }

  public static String obtainTemplateFolderPath(String templateUri) {
    String[] templateUris = fetchTemplateUris(templateUri);
    int endIndex = templateUri.lastIndexOf('/');
    return templateUris[0].substring(0, endIndex);
  }

  private static String[] fetchTemplateUris(String templateUri) {
    String[] templateUris = templateUri.split(":");
    if (templateUris.length < 1) {
      throw new WingsException("Invalid TemplateUri [" + templateUri + "]", WingsException.USER);
    }
    return templateUris;
  }

  /**
   * Get Command categories of service and service command
   * @return List of Command Categories
   */
  public List<CommandCategory> getCommandCategories(
      @NotEmpty String accountId, @NotEmpty String appId, @NotEmpty String templateId) {
    Template template = wingsPersistence.get(Template.class, templateId);

    Query<Template> templateQuery = wingsPersistence.createQuery(Template.class)
                                        .project("name", true)
                                        .filter(ACCOUNT_ID_KEY, accountId)
                                        .filter(APP_ID_KEY, appId)
                                        .filter(FOLDER_ID_KEY, template.getFolderId())
                                        .filter(TYPE_KEY, template.getType())
                                        .field(NAME_KEY)
                                        .notEqual(template.getName());

    List<Template> templates = templateQuery.asList();
    List<CommandCategory.CommandUnit> commands = templates.stream()
                                                     .map(commandTemplate
                                                         -> CommandCategory.CommandUnit.builder()
                                                                .uuid(commandTemplate.getUuid())
                                                                .name(commandTemplate.getName())
                                                                .type(CommandUnitType.COMMAND)
                                                                .build())
                                                     .collect(toList());

    return CommandHelper.getCommandCategories(commands);
  }

  public static List<String> addUserKeyWords(List<String> keywords, List<String> generatedKeywords) {
    List<String> userKeywords = ListUtils.trimStrings(keywords);
    if (isNotEmpty(userKeywords)) {
      generatedKeywords.addAll(userKeywords);
    }
    return ListUtils.trimStrings(generatedKeywords);
  }
  public static Map<String, Object> convertToVariableMap(List<Variable> variables) {
    if (isEmpty(variables)) {
      return null;
    }
    return variables.stream()
        .filter(variable -> variable.getName() != null && variable.getValue() != null)
        .collect(Collectors.toMap(Variable::getName, Variable::getValue));
  }
}