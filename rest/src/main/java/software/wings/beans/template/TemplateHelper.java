package software.wings.beans.template;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.govern.Switch.unhandled;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.Base.ACCOUNT_ID_KEY;
import static software.wings.beans.Base.LINKED_TEMPLATE_UUIDS_KEY;
import static software.wings.beans.Base.TEMPATE_UUID_KEY;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.EntityType.WORKFLOW;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.template.Template.FOLDER_ID_KEY;
import static software.wings.beans.template.Template.NAME_KEY;
import static software.wings.beans.template.Template.TYPE_KEY;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.data.structure.ListUtils;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.query.CountOptions;
import org.mongodb.morphia.query.Query;
import software.wings.beans.CommandCategory;
import software.wings.beans.EntityType;
import software.wings.beans.NameValuePair;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.command.CommandUnitType;
import software.wings.beans.command.ServiceCommand;
import software.wings.common.TemplateConstants;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
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

  public Class<?> lookupEntityClass(TemplateType templateType) {
    switch (templateType) {
      case SSH:
        return ServiceCommand.class;
      case HTTP:
        return Workflow.class;
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
        return WORKFLOW;
      default:
        unhandled(templateType);
    }
    return null;
  }

  public boolean templatesLinked(TemplateType templateType, List<String> templateUuids) {
    if (isEmpty(templateUuids)) {
      return false;
    }
    long size = wingsPersistence.createQuery(lookupEntityClass(templateType))
                    .field(lookupLinkedTemplateField(templateType))
                    .in(templateUuids)
                    .count(new CountOptions().limit(1));
    return size != 0;
  }

  private String lookupLinkedTemplateField(TemplateType templateType) {
    switch (templateType) {
      case SSH:
        return TEMPATE_UUID_KEY;
      case HTTP:
        return LINKED_TEMPLATE_UUIDS_KEY;
      default:
        unhandled(templateType);
    }
    return null;
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
      entityTemplateVariables.forEach(variable
          -> templateVariables.add(aVariable().withName(variable.getName()).withValue(variable.getValue()).build()));
    }
    return templateVariables;
  }

  public static String obtainTemplateVersion(String templateUri) {
    String[] templateUris = templateUri.split(":");
    if (templateUris.length < 2) {
      throw new WingsException("Invalid TemplateUri [" + templateUri + "]", WingsException.USER);
    }
    if (templateUris.length == 2) {
      return TemplateConstants.LATEST_TAG;
    }
    return templateUris[2];
  }

  /**
   * Get Command categories of service and service command
   * @return List of Command Categories
   */
  public List<CommandCategory> getCommandCategories(@NotEmpty String accountId, @NotEmpty String templateId) {
    Template template = wingsPersistence.get(Template.class, templateId);

    Query<Template> templateQuery = wingsPersistence.createQuery(Template.class)
                                        .project("name", true)
                                        .filter(ACCOUNT_ID_KEY, accountId)
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
}