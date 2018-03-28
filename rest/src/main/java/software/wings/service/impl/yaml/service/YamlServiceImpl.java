package software.wings.service.impl.yaml.service;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static java.util.Arrays.asList;
import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;
import static software.wings.beans.yaml.YamlConstants.YAML_EXTENSION;
import static software.wings.beans.yaml.YamlType.ACCOUNT_DEFAULTS;
import static software.wings.beans.yaml.YamlType.APPLICATION;
import static software.wings.beans.yaml.YamlType.APPLICATION_DEFAULTS;
import static software.wings.beans.yaml.YamlType.ARTIFACT_SERVER;
import static software.wings.beans.yaml.YamlType.ARTIFACT_STREAM;
import static software.wings.beans.yaml.YamlType.CLOUD_PROVIDER;
import static software.wings.beans.yaml.YamlType.COLLABORATION_PROVIDER;
import static software.wings.beans.yaml.YamlType.COMMAND;
import static software.wings.beans.yaml.YamlType.CONFIG_FILE;
import static software.wings.beans.yaml.YamlType.CONFIG_FILE_CONTENT;
import static software.wings.beans.yaml.YamlType.CONFIG_FILE_OVERRIDE;
import static software.wings.beans.yaml.YamlType.CONFIG_FILE_OVERRIDE_CONTENT;
import static software.wings.beans.yaml.YamlType.DEPLOYMENT_SPECIFICATION;
import static software.wings.beans.yaml.YamlType.ENVIRONMENT;
import static software.wings.beans.yaml.YamlType.INFRA_MAPPING;
import static software.wings.beans.yaml.YamlType.LOADBALANCER_PROVIDER;
import static software.wings.beans.yaml.YamlType.NOTIFICATION_GROUP;
import static software.wings.beans.yaml.YamlType.PIPELINE;
import static software.wings.beans.yaml.YamlType.SERVICE;
import static software.wings.beans.yaml.YamlType.VERIFICATION_PROVIDER;
import static software.wings.beans.yaml.YamlType.WORKFLOW;
import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.esotericsoftware.yamlbeans.YamlReader;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.error.Mark;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.scanner.ScannerException;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Application;
import software.wings.beans.Base;
import software.wings.beans.ErrorCode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.ResponseMessage;
import software.wings.beans.ResponseMessage.Level;
import software.wings.beans.RestResponse;
import software.wings.beans.RestResponse.Builder;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.GitFileChange;
import software.wings.beans.yaml.YamlConstants;
import software.wings.beans.yaml.YamlType;
import software.wings.dl.PageRequest.PageRequestBuilder;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.exception.YamlProcessingException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.impl.yaml.handler.app.ApplicationYamlHandler;
import software.wings.service.intfc.AlertService;
import software.wings.service.intfc.yaml.YamlGitService;
import software.wings.service.intfc.yaml.sync.YamlService;
import software.wings.utils.Misc;
import software.wings.utils.Validator;
import software.wings.yaml.BaseYaml;
import software.wings.yaml.YamlPayload;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * @author rktummala on 10/16/17
 */
@Singleton
public class YamlServiceImpl<Y extends BaseYaml, B extends Base> implements YamlService<Y, B> {
  private static final Logger logger = LoggerFactory.getLogger(YamlServiceImpl.class);

  @Inject private YamlHandlerFactory yamlHandlerFactory;
  @Inject private YamlHelper yamlHelper;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private transient YamlGitService yamlGitService;
  @Inject private AlertService alertService;

  private final List<YamlType> yamlProcessingOrder = getEntityProcessingOrder();

  private List<YamlType> getEntityProcessingOrder() {
    return Lists.newArrayList(ACCOUNT_DEFAULTS, CLOUD_PROVIDER, ARTIFACT_SERVER, COLLABORATION_PROVIDER,
        LOADBALANCER_PROVIDER, VERIFICATION_PROVIDER, NOTIFICATION_GROUP, APPLICATION, APPLICATION_DEFAULTS, SERVICE,
        ARTIFACT_STREAM, COMMAND, DEPLOYMENT_SPECIFICATION, CONFIG_FILE_CONTENT, CONFIG_FILE, ENVIRONMENT,
        INFRA_MAPPING, CONFIG_FILE_OVERRIDE_CONTENT, CONFIG_FILE_OVERRIDE, WORKFLOW, PIPELINE);
  }

  @Override
  public List<ChangeContext> processChangeSet(List<Change> changeList) throws YamlProcessingException {
    // compute the order of processing
    computeProcessingOrder(changeList);
    // validate
    List<ChangeContext> changeContextList = validate(changeList);
    // process in the given order
    process(changeContextList);

    return changeContextList;
  }

  @Override
  public RestResponse<B> update(YamlPayload yamlPayload, String accountId) {
    GitFileChange change = GitFileChange.Builder.aGitFileChange()
                               .withChangeType(ChangeType.MODIFY)
                               .withFileContent(yamlPayload.getYaml())
                               .withFilePath(yamlPayload.getPath())
                               .withAccountId(accountId)
                               .build();
    RestResponse rr = new RestResponse<>();
    List<GitFileChange> gitFileChangeList = asList(change);

    try {
      List<ChangeContext> changeContextList = processChangeSet(asList(change));
      Validator.notNullCheck("Change Context List is null", changeContextList);
      boolean empty = isEmpty(changeContextList);
      if (!empty) {
        // We only sent one
        ChangeContext changeContext = changeContextList.get(0);
        Object base = changeContext.getYamlSyncHandler().get(
            changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
        rr.setResource(base);
        yamlGitService.removeGitSyncErrors(accountId, gitFileChangeList, false);

      } else {
        software.wings.yaml.YamlHelper.addResponseMessage(
            rr, ErrorCode.GENERAL_YAML_INFO, Level.ERROR, "Update yaml failed. Reason: " + yamlPayload.getName());
      }
    } catch (YamlProcessingException ex) {
      software.wings.yaml.YamlHelper.addResponseMessage(
          rr, ErrorCode.GENERAL_YAML_INFO, Level.ERROR, "Update failed. Reason:" + ex.getMessage());
    }

    return rr;
  }

  @Override
  public RestResponse processYamlFilesAsZip(String accountId, InputStream fileInputStream, String yamlPath)
      throws IOException {
    try {
      List changeList = getChangesForZipFile(accountId, fileInputStream, yamlPath);

      List<ChangeContext> changeSets = processChangeSet(changeList);
      Map<String, Object> metaDataMap = Maps.newHashMap();
      metaDataMap.put("yamlFilesProcessed", changeSets.size());
      return RestResponse.Builder.aRestResponse().withMetaData(metaDataMap).build();
    } catch (YamlProcessingException ex) {
      logger.warn("Unable to process zip upload for account {}. ", accountId, ex);
      // gitToHarness is false, as this is not initiated from git
      yamlGitService.processFailedChanges(accountId, ex.getFailedChangeErrorMsgMap(), false);
    }
    return Builder.aRestResponse()
        .withResponseMessages(Arrays.asList(
            new ResponseMessage[] {ResponseMessage.aResponseMessage().code(ErrorCode.DEFAULT_ERROR_CODE).build()}))
        .build();
  }

  protected List<GitFileChange> getChangesForZipFile(String accountId, InputStream fileInputStream, String yamlPath)
      throws IOException {
    List<GitFileChange> changeList = Lists.newArrayList();
    File tempFile = File.createTempFile(accountId + "_" + System.currentTimeMillis() + "_yaml", ".tmp");
    ZipFile zipFile = null;
    try {
      OutputStream outputStream = new FileOutputStream(tempFile);
      IOUtils.copy(fileInputStream, outputStream);
      outputStream.close();

      zipFile = new ZipFile(tempFile.getAbsoluteFile());

      Enumeration<? extends ZipEntry> entries = zipFile.entries();

      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        File currFile = new File(entry.getName());
        try {
          if (!currFile.isHidden() && !entry.isDirectory()
              && (entry.getName().endsWith(YAML_EXTENSION)
                     || entry.getName().contains(YamlConstants.CONFIG_FILES_FOLDER))) {
            InputStream stream = zipFile.getInputStream(entry);
            StringWriter writer = new StringWriter();
            IOUtils.copy(stream, writer, "UTF-8");
            GitFileChange change =
                GitFileChange.Builder.aGitFileChange()
                    .withAccountId(accountId)
                    .withChangeType(ChangeType.ADD)
                    .withFileContent(writer.toString())
                    .withFilePath((yamlPath != null ? yamlPath + File.separatorChar : "") + entry.getName())
                    .build();
            changeList.add(change);
          }
        } finally {
          if (currFile != null) {
            currFile.delete();
          }
        }
      }
    } finally {
      if (zipFile != null) {
        zipFile.close();
      }
      tempFile.delete();
    }
    return changeList;
  }

  @Override
  public RestResponse<Y> getYaml(String accountId, String yamlFilePath) {
    RestResponse rr = new RestResponse<>();

    try {
      YamlType yamlType = findYamlType(yamlFilePath);
      BaseYaml yaml = null;

      final Class beanClass = yamlType.getBeanClass();
      String entityName =
          yamlHelper.extractEntityNameFromYamlPath(yamlType.getPathExpression(), yamlFilePath, PATH_DELIMITER);
      PageRequestBuilder pageRequest = aPageRequest();
      String appId;
      String serviceId;
      Object entity;

      switch (yamlType) {
        case APPLICATION:
          pageRequest.addFilter("accountId", Operator.EQ, accountId).addFilter("name", Operator.EQ, entityName);
          entity = getResult(beanClass, pageRequest);
          appId = yamlHelper.getAppId(accountId, yamlFilePath);
          if (entity != null) {
            ApplicationYamlHandler yamlHandler = yamlHandlerFactory.getYamlHandler(yamlType);
            yaml = yamlHandler.toYaml((Application) entity, appId);
          }
          break;

        case SERVICE:
        case ENVIRONMENT:
        case PIPELINE:
          appId = yamlHelper.getAppId(accountId, yamlFilePath);
          pageRequest.addFilter("appId", Operator.EQ, appId).addFilter("name", Operator.EQ, entityName);
          entity = getResult(beanClass, pageRequest);
          if (entity != null) {
            yaml = yamlHandlerFactory.getYamlHandler(yamlType).toYaml(entity, appId);
          }
          break;

        case CONFIG_FILE:
          // TODO
          break;
        case CONFIG_FILE_OVERRIDE:
          // TODO
          break;
        case CLOUD_PROVIDER:
          // TODO
          break;
        case ARTIFACT_SERVER:
          // TODO
          break;
        case COLLABORATION_PROVIDER:
          // TODO
          break;
        case LOADBALANCER_PROVIDER:
          // TODO
          break;
        case VERIFICATION_PROVIDER:
          // TODO
          break;
        case NOTIFICATION_GROUP:
          // TODO
          break;
        case WORKFLOW:
          appId = yamlHelper.getAppId(accountId, yamlFilePath);
          pageRequest.addFilter("appId", Operator.EQ, appId).addFilter("name", Operator.EQ, entityName);
          entity = getResult(beanClass, pageRequest);
          if (entity != null) {
            Workflow workflow = (Workflow) entity;
            yaml =
                yamlHandlerFactory
                    .getYamlHandler(yamlType, workflow.getOrchestrationWorkflow().getOrchestrationWorkflowType().name())
                    .toYaml(workflow, appId);
          }
          break;

        case ARTIFACT_STREAM:
          appId = yamlHelper.getAppId(accountId, yamlFilePath);
          serviceId = yamlHelper.getServiceId(appId, yamlFilePath);
          pageRequest.addFilter("appId", Operator.EQ, appId)
              .addFilter("serviceId", Operator.EQ, serviceId)
              .addFilter("sourceName", Operator.EQ, entityName);
          entity = getResult(beanClass, pageRequest);
          if (entity != null) {
            ArtifactStream artifactStream = (ArtifactStream) entity;
            yaml = yamlHandlerFactory.getYamlHandler(yamlType, artifactStream.getArtifactStreamType())
                       .toYaml(artifactStream, appId);
          }
          break;

        case COMMAND:
          // TODO
          break;

        case INFRA_MAPPING:
          appId = yamlHelper.getAppId(accountId, yamlFilePath);
          String envId = yamlHelper.getEnvironmentId(appId, yamlFilePath);
          pageRequest.addFilter("appId", Operator.EQ, appId)
              .addFilter("envId", Operator.EQ, envId)
              .addFilter("name", Operator.EQ, entityName);
          entity = getResult(beanClass, pageRequest);
          if (entity != null) {
            InfrastructureMapping infraMapping = (InfrastructureMapping) entity;
            yaml = yamlHandlerFactory.getYamlHandler(yamlType, infraMapping.getInfraMappingType())
                       .toYaml(infraMapping, appId);
          }
          break;

        default:
      }

      if (yaml != null) {
        rr.setResource(yaml);
      } else {
        software.wings.yaml.YamlHelper.addResponseMessage(
            rr, ErrorCode.GENERAL_YAML_INFO, Level.ERROR, "Unable to update yaml for:" + yamlFilePath);
      }

    } catch (HarnessException e) {
      software.wings.yaml.YamlHelper.addResponseMessage(
          rr, ErrorCode.GENERAL_YAML_INFO, Level.ERROR, "Unable to update yaml for:" + yamlFilePath);
    }
    return rr;
  }

  private Object getResult(Class beanClass, PageRequestBuilder pageRequest) {
    PageResponse response = wingsPersistence.query(beanClass, pageRequest.build());
    if (response.getTotal() > 0) {
      return response.get(0);
    } else {
      return null;
    }
  }

  /**
   *
   * @param changeList
   * @throws WingsException
   */
  private void computeProcessingOrder(List<Change> changeList) throws YamlProcessingException {
    Collections.sort(changeList, new FilePathComparator());
  }

  private <T extends BaseYamlHandler> List<ChangeContext> validate(List<Change> changeList)
      throws YamlProcessingException {
    List<ChangeContext> changeContextList = Lists.newArrayList();
    Map<Change, String> failedChangeErrorMsgMap = Maps.newHashMap();

    for (Change change : changeList) {
      String yamlFilePath = change.getFilePath();

      try {
        if (yamlFilePath.endsWith(YAML_EXTENSION)) {
          validateYaml(change.getFileContent());
          YamlType yamlType = findYamlType(yamlFilePath);
          String yamlSubType = getYamlSubType(change.getFileContent());

          T yamlSyncHandler = yamlHandlerFactory.getYamlHandler(yamlType, yamlSubType);
          if (yamlSyncHandler != null) {
            Class yamlClass = yamlSyncHandler.getYamlClass();
            BaseYaml yaml = getYaml(change.getFileContent(), yamlClass, false);
            Validator.notNullCheck("Could not get yaml object for :" + yamlFilePath, yaml);

            ChangeContext.Builder changeContextBuilder = ChangeContext.Builder.aChangeContext()
                                                             .withChange(change)
                                                             .withYaml(yaml)
                                                             .withYamlType(yamlType)
                                                             .withYamlSyncHandler(yamlSyncHandler);
            ChangeContext changeContext = changeContextBuilder.build();
            changeContextList.add(changeContext);
          } else {
            failedChangeErrorMsgMap.put(change, "Unsupported type: " + yamlType);
          }
        } else if (yamlFilePath.contains(YamlConstants.CONFIG_FILES_FOLDER)) {
          // Special handling for config files
          YamlType yamlType = findYamlType(yamlFilePath);
          if (YamlType.CONFIG_FILE_CONTENT == yamlType || YamlType.CONFIG_FILE_OVERRIDE_CONTENT == yamlType) {
            ChangeContext.Builder changeContextBuilder =
                ChangeContext.Builder.aChangeContext().withChange(change).withYamlType(yamlType);
            changeContextList.add(changeContextBuilder.build());
          } else {
            failedChangeErrorMsgMap.put(change, "Unsupported type: " + yamlType);
          }
        }
      } catch (ScannerException ex) {
        String message;
        Mark contextMark = ex.getContextMark();
        if (contextMark != null) {
          String snippet = contextMark.get_snippet();
          if (snippet != null) {
            message = "Not a well-formed yaml. The field " + snippet + " in line " + contextMark.getLine()
                + " doesn't end with :";
          } else {
            message = ex.getMessage();
          }
        } else {
          message = ex.getMessage();
        }
        logger.error(message, ex);
        failedChangeErrorMsgMap.put(change, message);
      } catch (UnrecognizedPropertyException ex) {
        String propertyName = ex.getPropertyName();
        if (propertyName != null) {
          String error = "Unrecognized field: " + propertyName;
          logger.error(error, ex);
          failedChangeErrorMsgMap.put(change, error);
        } else {
          logger.error("Unable to load yaml from string for file: " + yamlFilePath, ex);
          failedChangeErrorMsgMap.put(change, ex.getMessage());
        }
      } catch (Exception ex) {
        logger.error("Unable to load yaml from string for file: " + yamlFilePath, ex);
        failedChangeErrorMsgMap.put(change, ex.getMessage());
      }
    }

    if (failedChangeErrorMsgMap.size() > 0) {
      throw new YamlProcessingException(
          "Error while processing some yaml files in the changeset", failedChangeErrorMsgMap);
    }

    return changeContextList;
  }

  /**
   * To find the yaml sub type, we need to look at the type field in the yaml payload
   * @param fileContent
   * @return
   */
  private String getYamlSubType(String fileContent) throws IOException {
    YamlReader reader = new YamlReader(fileContent);
    Object object = reader.read();
    Map map = (Map) object;
    return (String) map.get("type");
  }

  private void process(List<ChangeContext> changeContextList) throws YamlProcessingException {
    Map<Change, String> failedChangeErrorMsgMap = Maps.newHashMap();
    for (ChangeContext changeContext : changeContextList) {
      String yamlFilePath = changeContext.getChange().getFilePath();
      try {
        logger.info("Processing change [{}]", changeContext.getChange());
        processYamlChange(changeContext, changeContextList);
        yamlGitService.discardGitSyncError(changeContext.getChange().getAccountId(), yamlFilePath);
        logger.info("Processing done for change [{}]", changeContext.getChange());
      } catch (Exception ex) {
        logger.error("Exception while processing yaml file {}", yamlFilePath, ex);
        // We continue processing the yaml files we understand, the failures are reported at the end
        failedChangeErrorMsgMap.put(changeContext.getChange(), Misc.getMessage(ex));
      }
    }

    if (failedChangeErrorMsgMap.size() > 0) {
      throw new YamlProcessingException(
          "Error while processing some yaml files in the changeset", failedChangeErrorMsgMap);
    }
  }

  private void processYamlChange(ChangeContext changeContext, List<ChangeContext> changeContextList)
      throws HarnessException {
    Validator.notNullCheck("changeContext is null", changeContext);
    Change change = changeContext.getChange();
    Validator.notNullCheck("FileChange is null", change);
    Validator.notNullCheck("ChangeType is null for change:" + change.getFilePath(), change.getChangeType());

    // If its not a yaml file, we don't have a handler for that file
    if (!change.getFilePath().endsWith(YAML_EXTENSION)) {
      return;
    }

    BaseYamlHandler yamlSyncHandler = changeContext.getYamlSyncHandler();

    switch (change.getChangeType()) {
      case ADD:
      case MODIFY:
        yamlSyncHandler.upsertFromYaml(changeContext, changeContextList);
        break;
      case DELETE:
        yamlSyncHandler.delete(changeContext);
        break;
      case RENAME:
        // TODO
      default:
        // TODO
        break;
    }
  }

  private BaseYaml getYaml(String yamlString, Class<? extends BaseYaml> yamlClass, boolean ignoreUnknownFields)
      throws IOException {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    if (ignoreUnknownFields) {
      mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
    return mapper.readValue(yamlString, yamlClass);
  }

  private final class FilePathComparator implements Comparator<Change> {
    @Override
    public int compare(Change lhs, Change rhs) {
      return findOrdinal(lhs.getFilePath()) - findOrdinal(rhs.getFilePath());
    }
  }

  private int findOrdinal(String yamlFilePath) {
    final AtomicInteger count = new AtomicInteger();
    Optional<YamlType> first = yamlProcessingOrder.stream()
                                   .filter(yamlType -> {
                                     count.incrementAndGet();
                                     return Pattern.matches(yamlType.getPathExpression(), yamlFilePath);
                                   })
                                   .findFirst();

    if (first.isPresent()) {
      return count.get();
    } else {
      return -1;
    }
  }

  private YamlType findYamlType(String yamlFilePath) throws HarnessException {
    Optional<YamlType> first = yamlProcessingOrder.stream()
                                   .filter(yamlType -> Pattern.matches(yamlType.getPathExpression(), yamlFilePath))
                                   .findFirst();

    return first.orElseThrow(() -> new HarnessException("Unknown yaml type for path: " + yamlFilePath));
  }

  /**
   * Check if the yaml is valid
   * @param yamlString
   * @return
   */
  private void validateYaml(String yamlString) throws ScannerException {
    Yaml yamlObj = new Yaml();

    // We just load the yaml to see if its well formed.
    yamlObj.load(yamlString);
  }
}
