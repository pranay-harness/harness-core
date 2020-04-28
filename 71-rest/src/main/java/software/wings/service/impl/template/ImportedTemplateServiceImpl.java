package software.wings.service.impl.template;

import static java.util.Collections.emptyMap;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import io.harness.commandlibrary.api.dto.CommandDTO;
import io.harness.commandlibrary.client.CommandLibraryServiceClientUtils;
import io.harness.commandlibrary.client.CommandLibraryServiceHttpClient;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import software.wings.api.commandlibrary.EnrichedCommandVersionDTO;
import software.wings.beans.template.ImportedTemplate;
import software.wings.beans.template.ImportedTemplate.ImportedTemplateKeys;
import software.wings.beans.template.Template;
import software.wings.beans.template.Template.TemplateKeys;
import software.wings.beans.template.TemplateGallery;
import software.wings.beans.template.TemplateGallery.GalleryKey;
import software.wings.beans.template.TemplateGalleryHelper;
import software.wings.beans.template.TemplateVersion;
import software.wings.beans.template.TemplateVersion.TemplateVersionKeys;
import software.wings.beans.template.dto.HarnessImportedTemplateDetails;
import software.wings.beans.template.dto.ImportedCommand;
import software.wings.beans.template.dto.ImportedCommand.ImportedCommandBuilder;
import software.wings.beans.template.dto.ImportedCommandVersion;
import software.wings.beans.template.dto.ImportedCommandVersion.ImportedCommandVersionBuilder;
import software.wings.beans.yaml.YamlType;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.intfc.template.ImportedTemplateService;
import software.wings.service.intfc.template.TemplateGalleryService;
import software.wings.service.intfc.template.TemplateService;
import software.wings.service.intfc.template.TemplateVersionService;
import software.wings.yaml.BaseYaml;
import software.wings.yaml.YamlHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class ImportedTemplateServiceImpl implements ImportedTemplateService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private TemplateService templateService;
  @Inject private TemplateVersionService templateVersionService;
  @Inject private TemplateGalleryService templateGalleryService;
  @Inject private YamlHandlerFactory yamlHandlerFactory;
  @Inject private TemplateGalleryHelper templateGalleryHelper;
  @Inject private CommandLibraryServiceHttpClient serviceHttpClient;

  @Override
  public boolean isImported(String templateId, String accountId) {
    // TODO: When copy is implemented we will have to query templateversion and check if template is imported.
    return wingsPersistence.createQuery(ImportedTemplate.class)
               .filter(ImportedTemplateKeys.templateId, templateId)
               .filter(ImportedTemplateKeys.accountId, accountId)
               .get()
        != null;
  }

  @Override
  public String getImportedTemplateVersionFromTemplateVersion(String templateId, String version, String accountId) {
    TemplateVersion templateVersion = wingsPersistence.createQuery(TemplateVersion.class)
                                          .filter(TemplateVersionKeys.templateUuid, templateId)
                                          .filter(TemplateVersionKeys.accountId, accountId)
                                          .filter(TemplateVersionKeys.version, Long.valueOf(version))
                                          .get();
    return templateVersion.getImportedTemplateVersion();
  }

  @Override
  public String getTemplateVersionFromImportedTemplateVersion(
      String templateId, String importedVersion, String accountId) {
    TemplateVersion templateVersion = wingsPersistence.createQuery(TemplateVersion.class)
                                          .filter(TemplateVersionKeys.templateUuid, templateId)
                                          .filter(TemplateVersionKeys.accountId, accountId)
                                          .filter(TemplateVersionKeys.importedTemplateVersion, importedVersion)
                                          .get();
    return String.valueOf(templateVersion.getVersion());
  }

  @Override
  public List<Template> getTemplatesByCommandNames(
      List<String> commandNames, String commandStoreName, String accountId) {
    List<String> templateIds = wingsPersistence.createQuery(ImportedTemplate.class)
                                   .filter(ImportedTemplateKeys.commandStoreName, commandStoreName)
                                   .field(ImportedTemplateKeys.commandName)
                                   .in(commandNames)
                                   .filter(ImportedTemplateKeys.accountId, accountId)
                                   .project(ImportedTemplateKeys.templateId, true)
                                   .asList()
                                   .stream()
                                   .map(ImportedTemplate::getTemplateId)
                                   .collect(Collectors.toList());

    return wingsPersistence.createQuery(Template.class)
        .field(TemplateKeys.uuid)
        .in(templateIds)
        .filter(TemplateKeys.accountId, accountId)
        .asList();
  }

  @Override
  public Template getTemplateByCommandName(String commandName, String commandStoreName, String accountId) {
    Template template = null;
    try {
      template = getTemplatesByCommandNames(Collections.singletonList(commandName), commandStoreName, accountId).get(0);
    } catch (Exception e) {
      logger.info(String.format(
                      "Template with command Id %s and command store id %s not found.", commandName, commandStoreName),
          e);
    }
    return template;
  }

  @Override
  public ImportedTemplate getCommandByTemplateId(String templateId, String accountId) {
    return wingsPersistence.createQuery(ImportedTemplate.class)
        .filter(ImportedTemplateKeys.templateId, templateId)
        .filter(ImportedTemplateKeys.accountId, accountId)
        .get();
  }

  @Override
  public Map<String, Template> getCommandNameTemplateMap(
      List<String> commandNames, String commandStoreName, String accountId) {
    List<Template> templates = getTemplatesByCommandNames(commandNames, commandStoreName, accountId);
    if (templates == null) {
      return null;
    }
    return commandNames.stream().collect(Collectors.toMap(
        Function.identity(), commandName -> getTemplateByCommandName(commandName, commandStoreName, accountId)));
  }

  @Override
  public List<ImportedCommand> makeImportedCommandObjectWithLatestVersion(
      Map<String, TemplateVersion> templateUuidLatestTemplateVersionMap, List<String> commandNames,
      final String commandStoreName, Map<String, Template> commandNameTemplateMap, String accountId) {
    if (commandNames == null) {
      return null;
    }
    return commandNames.stream()
        .map(commandName -> {
          Template template = commandNameTemplateMap.get(commandName);
          if (template == null) {
            return null;
          }
          String latestVersion =
              String.valueOf(templateUuidLatestTemplateVersionMap.get(template.getUuid()).getImportedTemplateVersion());
          ImportedTemplate importedTemplate = get(commandName, commandStoreName, accountId);
          String displayName = importedTemplate.getCommandDisplayName();
          return ImportedCommand.builder()
              .commandName(commandName)
              .commandStoreName(commandStoreName)
              .commandDisplayName(displayName)
              .highestVersion(latestVersion)
              .templateId(template.getUuid())
              .description(template.getDescription())
              .name(template.getName())
              .build();
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  @Override
  public ImportedCommand makeImportedCommandObject(String commandName, String commandStoreName,
      List<TemplateVersion> templateVersions, String accountId, Template template) {
    ImportedTemplate importedTemplate = get(commandName, commandStoreName, accountId);
    List<ImportedCommandVersion> importedCommandVersionList = new ArrayList<>();
    if (template == null) {
      return null;
    }
    BaseYamlHandler yamlHandler =
        yamlHandlerFactory.getYamlHandler(YamlType.GLOBAL_TEMPLATE_LIBRARY, template.getType());
    if (templateVersions != null) {
      templateVersions.forEach(templateVersion -> {
        Template templateWithDetails = templateService.get(template.getUuid());
        BaseYaml yaml = yamlHandler.toYaml(templateWithDetails, templateWithDetails.getAppId());
        ImportedCommandVersionBuilder importedCommandVersionBuilder =
            ImportedCommandVersion.builder()
                .commandName(commandName)
                .commandDisplayName(importedTemplate.getCommandDisplayName())
                .commandStoreName(commandStoreName)
                .createdAt(String.valueOf(templateVersion.getCreatedAt()))
                .version(templateVersion.getImportedTemplateVersion())
                .description(templateVersion.getVersionDetails())
                .templateObject(templateWithDetails.getTemplateObject())
                .variables(templateWithDetails.getVariables())
                .yamlContent(YamlHelper.toYamlString(yaml))
                .templateId(templateVersion.getTemplateUuid());
        if (templateVersion.getCreatedBy() != null) {
          importedCommandVersionBuilder.createdBy(templateVersion.getCreatedBy().getName());
        }
        importedCommandVersionList.add(importedCommandVersionBuilder.build());
      });
    }
    ImportedCommandBuilder importedCommandBuilder = ImportedCommand.builder()
                                                        .commandName(commandName)
                                                        .importedCommandVersionList(importedCommandVersionList)
                                                        .commandStoreName(commandStoreName)
                                                        .commandDisplayName(importedTemplate.getCommandDisplayName())
                                                        .name(template.getName())
                                                        .description(template.getDescription())
                                                        .templateId(template.getUuid());

    importedCommandBuilder.commandStoreName(importedTemplate.getCommandStoreName());
    return importedCommandBuilder.build();
  }

  @Override
  public ImportedTemplate get(String commandName, String commandStoreName, String accountId) {
    return wingsPersistence.createQuery(ImportedTemplate.class)
        .filter(ImportedTemplateKeys.commandName, commandName)
        .filter(ImportedTemplateKeys.accountId, accountId)
        .filter(ImportedTemplateKeys.commandStoreName, commandStoreName)
        .get();
  }

  @Override
  public Template getAndSaveImportedTemplate(
      String version, String commandName, String commandStoreName, String accountId) {
    CommandDTO commandDTO = downloadAndGetCommandDTO(commandStoreName, commandName);
    ImportedTemplate importedTemplate = getImportedTemplate(commandDTO, accountId);
    if (importedTemplate == null) {
      return saveNewCommand(version, commandName, commandStoreName, accountId, commandDTO);
    } else {
      return downloadAndSaveVersionOfExistingCommand(
          version, commandName, commandStoreName, accountId, importedTemplate);
    }
  }

  private ImportedTemplate getImportedTemplate(CommandDTO commandDTO, String accountId) {
    return get(commandDTO.getName(), commandDTO.getCommandStoreName(), accountId);
  }

  private Template saveNewCommand(
      String version, String commandName, String commandStoreName, String accountId, CommandDTO commandDTO) {
    ImportedTemplate importedTemplate = ImportedTemplate.builder()
                                            .commandStoreName(commandDTO.getCommandStoreName())
                                            .commandName(commandDTO.getName())
                                            .accountId(accountId)
                                            .appId(GLOBAL_APP_ID)
                                            .templateId(null)
                                            .description(commandDTO.getDescription())
                                            .name(commandDTO.getName())
                                            .imageUrl(commandDTO.getImageUrl())
                                            .build();
    Template template =
        downloadAndSaveNewCommandVersion(version, commandName, commandStoreName, accountId, importedTemplate);
    importedTemplate.setTemplateId(template.getUuid());
    wingsPersistence.save(importedTemplate);
    return template;
  }

  private Template downloadAndSaveNewCommandVersion(String version, String commandName, String commandStoreName,
      String accountId, ImportedTemplate importedTemplate) {
    EnrichedCommandVersionDTO commandVersionDTO =
        downloadAndGetCommandVersionDTO(commandStoreName, commandName, version);
    Template template = createTemplateFromCommandVersionDTO(commandVersionDTO, importedTemplate, accountId);
    return templateService.saveReferenceTemplate(template);
  }

  private Template downloadAndSaveVersionOfExistingCommand(String version, String commandName, String commandStoreName,
      String accountId, ImportedTemplate importedTemplate) {
    throwExceptionIfCommandVersionAlreadyDownloaded(version, importedTemplate);
    EnrichedCommandVersionDTO commandVersionDTO =
        downloadAndGetCommandVersionDTO(commandStoreName, commandName, version);
    Template template = createTemplateFromCommandVersionDTO(commandVersionDTO, importedTemplate, accountId);
    return templateService.updateReferenceTemplate(template);
  }

  @VisibleForTesting
  EnrichedCommandVersionDTO downloadAndGetCommandVersionDTO(
      String commandStoreName, String commandName, String version) {
    return CommandLibraryServiceClientUtils
        .executeHttpRequest(serviceHttpClient.getVersionDetails(commandStoreName, commandName, version, emptyMap()))
        .getResource();
  }

  @VisibleForTesting
  CommandDTO downloadAndGetCommandDTO(String commandStoreName, String commandName) {
    return CommandLibraryServiceClientUtils
        .executeHttpRequest(serviceHttpClient.getCommandDetails(commandStoreName, commandName, emptyMap()))
        .getResource();
  }

  private void throwExceptionIfCommandVersionAlreadyDownloaded(String version, ImportedTemplate importedTemplate) {
    ImportedCommand importedCommand = templateVersionService.listImportedTemplateVersions(
        importedTemplate.getCommandName(), importedTemplate.getCommandStoreName(), importedTemplate.getAccountId());
    boolean present = importedCommand.getImportedCommandVersionList()
                          .stream()
                          .filter(importedCommandVersion -> importedCommandVersion.getVersion().equals(version))
                          .findFirst()
                          .isPresent();
    if (present) {
      throw new InvalidRequestException(
          String.format("Version %s of command already exists.", version), WingsException.USER);
    }
  }

  @VisibleForTesting
  Template createTemplateFromCommandVersionDTO(
      EnrichedCommandVersionDTO commandVersionDTO, ImportedTemplate importedTemplate, String accountId) {
    Template template = Template.builder()
                            .templateObject(commandVersionDTO.getTemplateObject())
                            .variables(commandVersionDTO.getVariables())
                            .build();
    template.setAppId(GLOBAL_APP_ID);
    template.setAccountId(accountId);
    template.setVersionDetails(commandVersionDTO.getDescription());
    // TODO: Assuming only harness command gallery. May have to maintain a mapping for store ID to Gallery.
    TemplateGallery templateGallery =
        templateGalleryHelper.getGalleryByGalleryKey(GalleryKey.HARNESS_COMMAND_LIBRARY_GALLERY.name(), accountId);
    template.setGalleryId(templateGallery.getUuid());
    template.setName(importedTemplate.getName());
    template.setDescription(importedTemplate.getDescription());
    HarnessImportedTemplateDetails harnessImportedTemplateDetails =
        HarnessImportedTemplateDetails.builder()
            .commandVersion(commandVersionDTO.getVersion())
            .commandStoreName(commandVersionDTO.getCommandStoreName())
            .commandName(commandVersionDTO.getCommandName())
            .build();
    template.setImportedTemplateDetails(harnessImportedTemplateDetails);
    return template;
  }
}
