package software.wings.service.impl.template;

import static io.harness.network.Http.getUnsafeOkHttpClient;
import static software.wings.beans.Application.GLOBAL_APP_ID;

import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.harness.commandlibrary.api.dto.CommandDTO;
import io.harness.commandlibrary.api.dto.CommandVersionDTO;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.rest.RestResponse;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import software.wings.beans.template.ImportedTemplate;
import software.wings.beans.template.ImportedTemplate.ImportedTemplateKeys;
import software.wings.beans.template.Template;
import software.wings.beans.template.Template.TemplateKeys;
import software.wings.beans.template.TemplateGallery;
import software.wings.beans.template.TemplateGallery.GalleryKey;
import software.wings.beans.template.TemplateVersion;
import software.wings.beans.template.dto.HarnessImportedTemplateDetails;
import software.wings.beans.template.dto.ImportedCommand;
import software.wings.beans.template.dto.ImportedCommand.ImportedCommandBuilder;
import software.wings.beans.template.dto.ImportedCommandVersion;
import software.wings.beans.template.dto.ImportedCommandVersion.ImportedCommandVersionBuilder;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.template.ImportedTemplateService;
import software.wings.service.intfc.template.TemplateGalleryService;
import software.wings.service.intfc.template.TemplateService;
import software.wings.service.intfc.template.TemplateVersionService;

import java.io.IOException;
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
    if (templateVersions != null) {
      templateVersions.forEach(templateVersion -> {
        ImportedCommandVersionBuilder importedCommandVersionBuilder =
            ImportedCommandVersion.builder()
                .commandId(commandId)
                .commandStoreId(commandStoreId)
                .createdAt(String.valueOf(templateVersion.getCreatedAt()))
                .version(templateVersion.getImportedTemplateVersion())
                .versionDetails(templateVersion.getVersionDetails())
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
                                                        .commandStoreId(commandStoreId);
    if (template != null) {
      importedCommandBuilder.name(template.getName())
          .description(template.getDescription())
          .templateId(template.getUuid());
    }
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
      String token, String version, String commandId, String commandStoreId, String accountId) {
    String commandHost = getCommandDownloadHost(commandStoreId);
    String commandInformationUrl = commandHost + constructCommandDetailsUrl(commandId, commandStoreId, accountId);
    String commandVersionUrl =
        commandHost + constructCommandVersionDetailsUrl(commandId, commandStoreId, version, accountId);

    CommandDTO commandDTO = downloadAndGetCommandDTO(commandInformationUrl, token);
    ImportedTemplate importedTemplate = getImportedTemplate(commandDTO, accountId);
    Template template = null;
    if (importedTemplate == null) {
      template = saveNewCommand(token, version, commandVersionUrl, accountId, commandDTO);
    } else {
      template = downloadAndSaveNewCommandVersion(token, version, commandVersionUrl, accountId, importedTemplate);
    }

    return template;
  }

  private String getCommandDownloadHost(String commandStoreId) {
    // This is a stub. Will change later.
    return "https://localhost:9090";
  }

  private String constructCommandDetailsUrl(String commandId, String commandStoreId, String accountId) {
    return "/api/command-stores/" + commandStoreId + "/commands/" + commandId + "/?accountId=" + accountId;
  }

  private String constructCommandVersionDetailsUrl(
      String commandId, String commandStoreId, String version, String accountId) {
    return "/api/command-stores/" + commandStoreId + "/commands/" + commandId + "/versions/" + version
        + "/?accountId=" + accountId;
  }

  private String downloadFromUrl(String url, String token) {
    OkHttpClient httpClient = getUnsafeOkHttpClient(url);
    try {
      okhttp3.Response response = httpClient.newCall(getRequestForMakingCalls(url, token)).execute();
      if (response.isSuccessful()) {
        String bodyString = (null != response.body()) ? response.body().string() : "null";
        if (bodyString != null) {
          logger.info("Response successful for url {}. Response body: {}", url, bodyString);
          return bodyString;
        }
      }
    } catch (IOException e) {
      throw new InvalidRequestException("Cannot download command.", e, WingsException.USER);
    }
    return null;
  }

  private Request getRequestForMakingCalls(String url, String token) {
    return new Request.Builder()
        .method("GET", null)
        .url(url)
        .addHeader("Authorization", token)
        .addHeader("Content-Type", "application/json")
        .addHeader("Accept", "*/*")
        .addHeader("Cache-Control", "no-cache")
        .addHeader("cache-control", "no-cache")
        .addHeader("Connection", "close")
        .addHeader("Accept-Encoding", "identity")
        .build();
  }

  private ImportedTemplate getImportedTemplate(CommandDTO commandDTO, String accountId) {
    return get(commandDTO.getId(), commandDTO.getCommandStoreId(), accountId);
  }

  private Template saveNewCommand(
      String token, String version, String commandversionUrl, String accountId, CommandDTO commandDTO) {
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
        downloadAndSaveNewCommandVersion(token, version, commandversionUrl, accountId, importedTemplate);
    importedTemplate.setTemplateId(template.getUuid());
    wingsPersistence.save(importedTemplate);
    return template;
  }

  private Template downloadAndSaveNewCommandVersion(
      String token, String version, String commandVersionUrl, String accountId, ImportedTemplate importedTemplate) {
    Template template;
    if (importedTemplate.getTemplateId() == null) {
      CommandVersionDTO commandVersionDTO = downloadAndGetCommandVersionDTO(commandVersionUrl, token);
      template = createTemplateFromCommandVersionDTO(commandVersionDTO, importedTemplate);
      template = templateService.saveReferenceTemplate(template);
    } else {
      throwExceptionIfCommandVersionAlreadyDownloaded(version, importedTemplate);
      CommandVersionDTO commandVersionDTO = downloadAndGetCommandVersionDTO(commandVersionUrl, token);
      template = createTemplateFromCommandVersionDTO(commandVersionDTO, importedTemplate);
      template = templateService.updateReferenceTemplate(template);
    }
    return template;
  }

  @VisibleForTesting
  CommandVersionDTO downloadAndGetCommandVersionDTO(String commandVersionUrl, String token) {
    String payload = downloadFromUrl(commandVersionUrl, token);
    ObjectMapper objectMapper = new ObjectMapper();
    CommandVersionDTO commandVersionDTO = null;
    try {
      RestResponse<CommandVersionDTO> commandDTORestResponse = objectMapper.readValue(payload, RestResponse.class);
      commandVersionDTO = objectMapper.convertValue(commandDTORestResponse.getResource(), CommandVersionDTO.class);
    } catch (IOException e) {
      throw new InvalidRequestException("Cannot download command.", e, WingsException.USER);
    }
    return commandVersionDTO;
  }

  @VisibleForTesting
  CommandDTO downloadAndGetCommandDTO(String commandInformationUrl, String token) {
    ObjectMapper objectMapper = new ObjectMapper();
    String payload = downloadFromUrl(commandInformationUrl, token);
    CommandDTO commandDTO;
    try {
      RestResponse<CommandDTO> commandDTORestResponse = objectMapper.readValue(payload, RestResponse.class);
      commandDTO = objectMapper.convertValue(commandDTORestResponse.getResource(), CommandDTO.class);
    } catch (IOException e) {
      throw new InvalidRequestException("Cannot download command.", e, WingsException.USER);
    }
    return commandDTO;
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
  Template createTemplateFromCommandVersionDTO(CommandVersionDTO commandVersionDTO, ImportedTemplate importedTemplate) {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    Template template;
    try {
      template = objectMapper.readValue(commandVersionDTO.getYamlContent(), Template.class);
    } catch (IOException e) {
      throw new InvalidRequestException("Cannot map value correctly", e);
    }
    template.setAppId(GLOBAL_APP_ID);
    template.setAccountId(importedTemplate.getAccountId());
    template.setVersionDetails(commandVersionDTO.getDescription());
    TemplateGallery templateGallery = templateGalleryService.getByAccount(
        importedTemplate.getAccountId(), GalleryKey.HARNESS_COMMAND_LIBRARY_GALLERY);
    template.setGalleryId(templateGallery.getUuid());
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
