package software.wings.service.impl.template;

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
  @Inject WingsPersistence wingsPersistence;
  @Inject TemplateService templateService;
  @Inject TemplateVersionService templateVersionService;
  @Inject TemplateGalleryService templateGalleryService;
  @Inject YamlHandlerFactory yamlHandlerFactory;
  @Inject TemplateGalleryHelper templateGalleryHelper;
  private CommandLibraryServiceHttpClient serviceHttpClient;

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
  public List<Template> getTemplatesByCommandIds(List<String> commandIds, String commandStoreId, String accountId) {
    List<String> templateIds = wingsPersistence.createQuery(ImportedTemplate.class)
                                   .filter(ImportedTemplateKeys.commandStoreId, commandStoreId)
                                   .field(ImportedTemplateKeys.commandId)
                                   .in(commandIds)
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
  public Template getTemplateByCommandId(String commandId, String commandStoreId, String accountId) {
    Template template = null;
    try {
      template = getTemplatesByCommandIds(Collections.singletonList(commandId), commandStoreId, accountId).get(0);
    } catch (Exception e) {
      logger.info(
          String.format("Template with command Id %s and command store id %s not found.", commandId, commandStoreId),
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
  public Map<String, Template> getCommandIdTemplateMap(
      List<String> commandIds, String commandStoreId, String accountId) {
    List<Template> templates = getTemplatesByCommandIds(commandIds, commandStoreId, accountId);
    if (templates == null) {
      return null;
    }
    return commandIds.stream().collect(Collectors.toMap(
        Function.identity(), commandId -> getTemplateByCommandId(commandId, commandStoreId, accountId)));
  }

  @Override
  public List<ImportedCommand> makeImportedCommandObjectWithLatestVersion(
      Map<String, TemplateVersion> templateUuidLatestTemplateVersionMap, List<String> commandIds, String commandStoreId,
      Map<String, Template> commandIdTemplateMap, String accountId) {
    if (commandIds == null) {
      return null;
    }
    return commandIds.stream()
        .map(commandId -> {
          Template template = commandIdTemplateMap.get(commandId);
          if (template == null) {
            return null;
          }
          String latestVersion =
              String.valueOf(templateUuidLatestTemplateVersionMap.get(template.getUuid()).getImportedTemplateVersion());
          ImportedTemplate importedTemplate = get(commandId, commandStoreId, accountId);
          String commandStoreName = null;
          if (importedTemplate != null) {
            commandStoreName = importedTemplate.getCommandStoreName();
          }
          return ImportedCommand.builder()
              .commandId(commandId)
              .commandStoreId(commandStoreId)
              .highestVersion(latestVersion)
              .templateId(template.getUuid())
              .description(template.getDescription())
              .name(template.getName())
              .commandStoreName(commandStoreName)
              .build();
        })
        .filter(Objects::nonNull)
        .collect(Collectors.toList());
  }

  @Override
  public ImportedCommand makeImportedCommandObject(String commandId, String commandStoreId,
      List<TemplateVersion> templateVersions, String accountId, Template template) {
    ImportedTemplate importedTemplate = get(commandId, commandStoreId, accountId);
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
                .commandId(commandId)
                .commandStoreId(commandStoreId)
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
                                                        .commandId(commandId)
                                                        .importedCommandVersionList(importedCommandVersionList)
                                                        .commandStoreId(commandStoreId)
                                                        .name(template.getName())
                                                        .description(template.getDescription())
                                                        .templateId(template.getUuid());

    if (importedTemplate != null) {
      importedCommandBuilder.commandStoreName(importedTemplate.getCommandStoreName());
    }
    return importedCommandBuilder.build();
  }

  @Override
  public ImportedTemplate get(String commandId, String commandStoreId, String accountId) {
    return wingsPersistence.createQuery(ImportedTemplate.class)
        .filter(ImportedTemplateKeys.commandId, commandId)
        .filter(ImportedTemplateKeys.accountId, accountId)
        .filter(ImportedTemplateKeys.commandStoreId, commandStoreId)
        .get();
  }

  @Override
  public Template getAndSaveImportedTemplate(
      String version, String commandId, String commandStoreId, String accountId) {
    CommandDTO commandDTO = downloadAndGetCommandDTO(commandStoreId, commandId);
    ImportedTemplate importedTemplate = getImportedTemplate(commandDTO, accountId);
    if (importedTemplate == null) {
      return saveNewCommand(version, commandId, commandStoreId, accountId, commandDTO);
    } else {
      return downloadAndSaveVersionOfExistingCommand(version, commandId, commandStoreId, accountId, importedTemplate);
    }
  }

  private ImportedTemplate getImportedTemplate(CommandDTO commandDTO, String accountId) {
    return get(commandDTO.getId(), commandDTO.getCommandStoreId(), accountId);
  }

  private Template saveNewCommand(
      String version, String commandId, String commandStoreId, String accountId, CommandDTO commandDTO) {
    ImportedTemplate importedTemplate = ImportedTemplate.builder()
                                            .commandStoreId(commandDTO.getCommandStoreId())
                                            .commandId(commandDTO.getId())
                                            .accountId(accountId)
                                            .appId(GLOBAL_APP_ID)
                                            .templateId(null)
                                            .description(commandDTO.getDescription())
                                            .name(commandDTO.getName())
                                            .imageUrl(commandDTO.getImageUrl())
                                            .build();
    Template template =
        downloadAndSaveNewCommandVersion(version, commandId, commandStoreId, accountId, importedTemplate);
    importedTemplate.setTemplateId(template.getUuid());
    wingsPersistence.save(importedTemplate);
    return template;
  }

  private Template downloadAndSaveNewCommandVersion(
      String version, String commandId, String commandStoreId, String accountId, ImportedTemplate importedTemplate) {
    EnrichedCommandVersionDTO commandVersionDTO = downloadAndGetCommandVersionDTO(commandStoreId, commandId, version);
    Template template = createTemplateFromCommandVersionDTO(commandVersionDTO, importedTemplate, accountId);
    return templateService.saveReferenceTemplate(template);
  }

  private Template downloadAndSaveVersionOfExistingCommand(
      String version, String commandId, String commandStoreId, String accountId, ImportedTemplate importedTemplate) {
    throwExceptionIfCommandVersionAlreadyDownloaded(version, importedTemplate);
    EnrichedCommandVersionDTO commandVersionDTO = downloadAndGetCommandVersionDTO(commandStoreId, commandId, version);
    Template template = createTemplateFromCommandVersionDTO(commandVersionDTO, importedTemplate, accountId);
    return templateService.updateReferenceTemplate(template);
  }

  @VisibleForTesting
  EnrichedCommandVersionDTO downloadAndGetCommandVersionDTO(String commandStoreId, String commandId, String version) {
    return CommandLibraryServiceClientUtils
        .executeHttpRequest(serviceHttpClient.getVersionDetails(commandStoreId, commandId, version))
        .getResource();
  }

  @VisibleForTesting
  CommandDTO downloadAndGetCommandDTO(String commandStoreId, String commandId) {
    return CommandLibraryServiceClientUtils
        .executeHttpRequest(serviceHttpClient.getCommandDetails(commandStoreId, commandId))
        .getResource();
  }

  private void throwExceptionIfCommandVersionAlreadyDownloaded(String version, ImportedTemplate importedTemplate) {
    ImportedCommand importedCommand = templateVersionService.listImportedTemplateVersions(
        importedTemplate.getCommandId(), importedTemplate.getCommandStoreId(), importedTemplate.getAccountId());
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
            .importedCommandVersion(commandVersionDTO.getVersion())
            .importedCommandStoreId(commandVersionDTO.getCommandStoreId())
            .importedCommandId(commandVersionDTO.getCommandId())
            .build();
    template.setImportedTemplateDetails(harnessImportedTemplateDetails);
    return template;
  }
}
