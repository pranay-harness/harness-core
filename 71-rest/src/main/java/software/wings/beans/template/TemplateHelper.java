package software.wings.beans.template;

import static io.harness.data.structure.CollectionUtils.trimmedLowercaseSet;
import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.govern.Switch.unhandled;
import static io.harness.persistence.HPersistence.upToOne;
import static io.harness.persistence.HQuery.excludeValidate;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.EntityType.ARTIFACT_STREAM;
import static software.wings.beans.EntityType.SERVICE;
import static software.wings.beans.EntityType.WORKFLOW;
import static software.wings.beans.Variable.VariableBuilder.aVariable;
import static software.wings.beans.template.Template.FOLDER_ID_KEY;
import static software.wings.beans.template.Template.NAME_KEY;
import static software.wings.beans.template.Template.TYPE_KEY;
import static software.wings.common.TemplateConstants.DEFAULT_TAG;
import static software.wings.common.TemplateConstants.GALLERY_TOP_LEVEL_PATH_DELIMITER;
import static software.wings.common.TemplateConstants.PATH_DELIMITER;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.PersistentEntity;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.query.Query;
import software.wings.beans.Application.ApplicationKeys;
import software.wings.beans.CommandCategory;
import software.wings.beans.EntityType;
import software.wings.beans.NameValuePair;
import software.wings.beans.Variable;
import software.wings.beans.Workflow;
import software.wings.beans.Workflow.WorkflowKeys;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStream.ArtifactStreamKeys;
import software.wings.beans.command.CommandUnitType;
import software.wings.beans.command.ServiceCommand;
import software.wings.beans.template.Template.TemplateKeys;
import software.wings.beans.template.dto.HarnessImportedTemplateDetails;
import software.wings.beans.template.dto.ImportedTemplateDetails;
import software.wings.common.TemplateConstants;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.command.CommandHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
      case PCF_PLUGIN:
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
      case PCF_PLUGIN:
        return WORKFLOW;
      case ARTIFACT_SOURCE:
        return ARTIFACT_STREAM;
      default:
        throw new InvalidArgumentsException(String.format("TemplateType [%s] is not supported", templateType), null);
    }
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
                               .count(upToOne);
    long linkedTemplates = wingsPersistence.createQuery(lookupEntityClass(templateType))
                               .field(lookupLinkedTemplateField(templateType))
                               .in(templateUuids)
                               .count(upToOne);
    long templatesReferencedInOtherTemplates;
    if (templateType == TemplateType.SSH) {
      templatesReferencedInOtherTemplates =
          wingsPersistence.createQuery(VersionedTemplate.class, excludeValidate)
              .field("templateObject.referencedTemplateList.templateReference.templateUuid")
              .in(templateUuids)
              .count(upToOne);
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
      case PCF_PLUGIN:
        return WorkflowKeys.linkedTemplateUuids;
      case ARTIFACT_SOURCE:
        return ArtifactStreamKeys.templateUuid;
      default:
        unhandled(templateType);
    }
    return null;
  }

  public List<Variable> overrideVariables(List<Variable> templateVariables, List<Variable> existingVariables) {
    return overrideVariables(templateVariables, existingVariables, true);
  }

  public List<Variable> overrideVariables(
      List<Variable> templateVariables, List<Variable> existingVariables, boolean doNotOverride) {
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
        if (doNotOverride) {
          if (isNotEmpty(oldVariable.getValue())) {
            variable.setValue(oldVariable.getValue());
          }
          if (isNotEmpty(oldVariable.getDescription())) {
            variable.setDescription(oldVariable.getDescription());
          }
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
        if (variable.getType() != null) {
          if (variable.getType() != oldVariable.getType()) {
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

  private Map<String, Variable> obtainVariableMap(List<Variable> variables) {
    if (isEmpty(variables)) {
      return new HashMap<>();
    }
    Map<String, Variable> map = new HashMap<>();
    for (Variable variable : variables) {
      if (variable.getName() != null && variable.getValue() != null) {
        if (!map.containsKey(variable.getName())) {
          map.put(variable.getName(), variable);
        }
      }
    }
    return map;
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

  public static ImportedTemplateDetails getImportedTemplateDetails(Template template, String templateVersion) {
    if (template.getImportedTemplateDetails() instanceof HarnessImportedTemplateDetails) {
      HarnessImportedTemplateDetails importedTemplateDetails =
          (HarnessImportedTemplateDetails) template.getImportedTemplateDetails();
      if (DEFAULT_TAG.equals(templateVersion)) {
        importedTemplateDetails.setCommandVersion(DEFAULT_TAG);
      }
      return importedTemplateDetails;
    }
    return null;
  }

  // Don't directly use this method as it isn't imported template version aware.
  public static String obtainTemplateVersion(String templateUri) {
    String[] templateUris = fetchTemplateUris(templateUri);
    if (templateUris.length == 1) {
      return TemplateConstants.LATEST_TAG;
    }
    return templateUris[templateUris.length - 1];
  }

  public static String obtainTemplateName(String templateUri) {
    String cleanedTemplatePath = templateUri;
    if (templateUri.contains(":")) {
      String[] templateUris = fetchTemplateUris(templateUri);
      if (templateUris.length == 1) {
        cleanedTemplatePath = templateUris[0];
      } else {
        cleanedTemplatePath = templateUris[templateUris.length - 1 - 1];
      }
    }
    return cleanedTemplatePath.substring(cleanedTemplatePath.lastIndexOf(PATH_DELIMITER) + 1);
  }

  public static String obtainTemplateNameForImportedCommands(String templateUri) {
    return templateUri.split(String.valueOf(GALLERY_TOP_LEVEL_PATH_DELIMITER))[1];
  }

  public static String obtainTemplateFolderPath(String templateUri) {
    String[] templateUris = fetchTemplateUris(templateUri);
    String cleanedTemplateUri;
    if (templateUris.length == 1) {
      cleanedTemplateUri = templateUris[0];
    } else {
      cleanedTemplateUri = templateUris[templateUris.length - 1 - 1];
    }
    int endIndex = cleanedTemplateUri.lastIndexOf('/');
    return cleanedTemplateUri.substring(0, endIndex);
  }

  private static String[] fetchTemplateUris(String templateUri) {
    String[] templateUris = templateUri.split(":");
    if (templateUris.length < 1) {
      throw new InvalidRequestException("Invalid TemplateUri [" + templateUri + "]", WingsException.USER);
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
                                        .filter(ApplicationKeys.accountId, accountId)
                                        .filter(ApplicationKeys.appId, appId)
                                        .filter(FOLDER_ID_KEY, template.getFolderId())
                                        .filter(TYPE_KEY, template.getType())
                                        .filter(TemplateKeys.galleryId, template.getGalleryId())
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

    return CommandHelper.prepareCommandCategoriesFromCommands(commands);
  }

  public static Set<String> addUserKeyWords(Set<String> keywords, Set<String> generatedKeywords) {
    Set<String> userKeywords = trimmedLowercaseSet(keywords);
    if (isNotEmpty(userKeywords)) {
      generatedKeywords.addAll(userKeywords);
    }
    return trimmedLowercaseSet(generatedKeywords);
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
