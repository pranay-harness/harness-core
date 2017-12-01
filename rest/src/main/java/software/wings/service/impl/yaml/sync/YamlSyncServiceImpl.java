package software.wings.service.impl.yaml.sync;

import static software.wings.beans.yaml.YamlConstants.PATH_DELIMITER;
import static software.wings.beans.yaml.YamlType.APPLICATION;
import static software.wings.beans.yaml.YamlType.ARTIFACT_SERVER;
import static software.wings.beans.yaml.YamlType.ARTIFACT_STREAM;
import static software.wings.beans.yaml.YamlType.CLOUD_PROVIDER;
import static software.wings.beans.yaml.YamlType.COLLABORATION_PROVIDER;
import static software.wings.beans.yaml.YamlType.COMMAND;
import static software.wings.beans.yaml.YamlType.CONFIG_FILE;
import static software.wings.beans.yaml.YamlType.CONFIG_FILE_OVERRIDE;
import static software.wings.beans.yaml.YamlType.DEPLOYMENT_SPECIFICATION;
import static software.wings.beans.yaml.YamlType.ENVIRONMENT;
import static software.wings.beans.yaml.YamlType.INFRA_MAPPING;
import static software.wings.beans.yaml.YamlType.LOADBALANCER_PROVIDER;
import static software.wings.beans.yaml.YamlType.PIPELINE;
import static software.wings.beans.yaml.YamlType.SERVICE;
import static software.wings.beans.yaml.YamlType.VERIFICATION_PROVIDER;
import static software.wings.beans.yaml.YamlType.WORKFLOW;
import static software.wings.utils.Util.isEmpty;

import com.google.api.client.util.Throwables;
import com.google.common.collect.Lists;

import com.esotericsoftware.yamlbeans.YamlReader;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.snakeyaml.Yaml;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Base;
import software.wings.beans.ErrorCode;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.ResponseMessage.ResponseTypeEnum;
import software.wings.beans.RestResponse;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.yaml.Change;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.ChangeContext;
import software.wings.beans.yaml.YamlType;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.HarnessException;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.handler.BaseYamlHandler;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.intfc.yaml.YamlResourceService;
import software.wings.service.intfc.yaml.sync.YamlSyncService;
import software.wings.utils.Validator;
import software.wings.yaml.BaseYaml;
import software.wings.yaml.YamlHelper;
import software.wings.yaml.YamlPayload;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Pattern;
import javax.inject.Inject;

/**
 * @author rktummala on 10/16/17
 */
public class YamlSyncServiceImpl<Y extends BaseYaml, B extends Base> implements YamlSyncService<Y, B> {
  private final Logger logger = LoggerFactory.getLogger(getClass());

  @Inject private YamlHandlerFactory yamlHandlerFactory;
  @Inject private YamlSyncHelper yamlSyncHelper;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private YamlResourceService yamlResourceService;

  private final List<YamlType> yamlProcessingOrder = getEntityProcessingOrder();

  private List<YamlType> getEntityProcessingOrder() {
    return Lists.newArrayList(CLOUD_PROVIDER, ARTIFACT_SERVER, COLLABORATION_PROVIDER, LOADBALANCER_PROVIDER,
        VERIFICATION_PROVIDER, APPLICATION, SERVICE, ARTIFACT_STREAM, COMMAND, DEPLOYMENT_SPECIFICATION, CONFIG_FILE,
        ENVIRONMENT, INFRA_MAPPING, CONFIG_FILE_OVERRIDE, WORKFLOW, PIPELINE);
  }

  @Override
  public List<ChangeContext> syncChangeSet(List<Change> changeList) throws HarnessException {
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
    Change change = Change.Builder.aFileChange()
                        .withChangeType(ChangeType.MODIFY)
                        .withFileContent(yamlPayload.getYaml())
                        .withFilePath(yamlPayload.getPath())
                        .withAccountId(accountId)
                        .build();
    RestResponse rr = new RestResponse<>();

    try {
      List<ChangeContext> changeContextList = syncChangeSet(Arrays.asList(change));
      Validator.notNullCheck("Change Context List is null", changeContextList);
      boolean empty = isEmpty(changeContextList);
      if (!empty) {
        // We only sent one
        ChangeContext changeContext = changeContextList.get(0);
        Object base = changeContext.getYamlSyncHandler().get(
            changeContext.getChange().getAccountId(), changeContext.getChange().getFilePath());
        rr.setResource(base);

      } else {
        YamlHelper.addResponseMessage(rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.ERROR,
            "Unable to update yaml for:" + yamlPayload.getName());
      }
    } catch (HarnessException e) {
      YamlHelper.addResponseMessage(
          rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.ERROR, "Unable to update yaml:" + e.getMessage());
    }

    return rr;
  }

  @Override
  public RestResponse<Y> getYaml(String accountId, String yamlFilePath) {
    RestResponse rr = new RestResponse<>();

    try {
      YamlType yamlType = findYamlType(yamlFilePath);
      BaseYaml yaml = null;

      final Class beanClass = yamlType.getBeanClass();
      String entityName =
          yamlSyncHelper.extractEntityNameFromYamlPath(yamlType.getPathExpression(), yamlFilePath, PATH_DELIMITER);
      PageRequest.Builder pageRequest = PageRequest.Builder.aPageRequest();
      String appId;
      String serviceId;
      Object entity;

      switch (yamlType) {
        case APPLICATION:
          pageRequest.addFilter("accountId", Operator.EQ, accountId).addFilter("name", Operator.EQ, entityName);
          entity = getResult(beanClass, pageRequest);
          appId = yamlSyncHelper.getAppId(accountId, yamlFilePath);
          if (entity != null) {
            BaseYamlHandler yamlHandler = yamlHandlerFactory.getYamlHandler(yamlType, null);
            yaml = yamlHandler.toYaml(entity, appId);
          }
          break;

        case SERVICE:
        case ENVIRONMENT:
        case PIPELINE:
          appId = yamlSyncHelper.getAppId(accountId, yamlFilePath);
          pageRequest.addFilter("appId", Operator.EQ, appId).addFilter("name", Operator.EQ, entityName);
          entity = getResult(beanClass, pageRequest);
          if (entity != null) {
            yaml = yamlHandlerFactory.getYamlHandler(yamlType, null).toYaml((Base) entity, appId);
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
        case WORKFLOW:
          appId = yamlSyncHelper.getAppId(accountId, yamlFilePath);
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
          appId = yamlSyncHelper.getAppId(accountId, yamlFilePath);
          serviceId = yamlSyncHelper.getServiceId(appId, yamlFilePath);
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
          appId = yamlSyncHelper.getAppId(accountId, yamlFilePath);
          String envId = yamlSyncHelper.getEnvironmentId(appId, yamlFilePath);
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
        YamlHelper.addResponseMessage(
            rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.ERROR, "Unable to update yaml for:" + yamlFilePath);
      }

    } catch (HarnessException e) {
      YamlHelper.addResponseMessage(
          rr, ErrorCode.GENERAL_YAML_INFO, ResponseTypeEnum.ERROR, "Unable to update yaml for:" + yamlFilePath);
    }
    return rr;
  }

  private Object getResult(Class beanClass, PageRequest.Builder pageRequest) {
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
  private void computeProcessingOrder(List<Change> changeList) throws HarnessException {
    Collections.sort(changeList, new FilePathComparator());
  }

  private List<ChangeContext> validate(List<Change> changeList) throws HarnessException {
    List<ChangeContext> changeContextList = Lists.newArrayList();

    try {
      changeList.forEach(change -> {
        try {
          validateYaml(change.getFileContent());
          YamlType yamlType = findYamlType(change.getFilePath());
          String yamlSubType = getYamlSubType(change.getFileContent());

          BaseYamlHandler yamlSyncHandler = yamlHandlerFactory.getYamlHandler(yamlType, yamlSubType);
          if (yamlSyncHandler != null) {
            Class yamlClass = yamlSyncHandler.getYamlClass();
            BaseYaml yaml = getYaml(change.getFileContent(), yamlClass, false);
            Validator.notNullCheck("Could not get yaml object for :" + change.getFilePath(), yaml);

            ChangeContext.Builder changeContextBuilder = ChangeContext.Builder.aChangeContext()
                                                             .withChange(change)
                                                             .withYaml(yaml)
                                                             .withYamlType(yamlType)
                                                             .withYamlSyncHandler(yamlSyncHandler);
            ChangeContext changeContext = changeContextBuilder.build();
            changeContextList.add(changeContext);
            yamlSyncHandler.validate(changeContext, changeContextList);
          } else {
            throw new HarnessException(
                "Can't find yaml handler for type: " + yamlType + " and subType: " + yamlSubType);
          }

        } catch (IOException | HarnessException ex) {
          logger.error("Unable to de-serialize yaml from string", ex);
          Throwables.propagate(ex);
        }

      });
    } catch (RuntimeException ex) {
      throw new HarnessException(ex);
    }

    return changeContextList;
  }

  /**
   * To find the yaml sub type, we need to look at the type field in the yaml payload
   * @param fileContent
   * @return
   * @throws IOException
   */
  private String getYamlSubType(String fileContent) throws IOException {
    YamlReader reader = new YamlReader(fileContent);
    Object object = reader.read();
    Map map = (Map) object;
    return (String) map.get("type");
  }

  private void process(List<ChangeContext> changeContextList) throws HarnessException {
    try {
      changeContextList.stream().forEachOrdered(changeContext -> {
        try {
          logger.info("Processing change [{}]", changeContext.getChange());
          processChange(changeContext, changeContextList);
          logger.info("Processing done for change [{}]", changeContext.getChange());
        } catch (HarnessException ex) {
          Throwables.propagate(ex);
        }
      });
    } catch (RuntimeException ex) {
      logger.error("Exception: ", ex);
      throw new HarnessException(ex);
    }
  }

  private void processChange(ChangeContext changeContext, List<ChangeContext> changeContextList)
      throws HarnessException {
    Validator.notNullCheck("changeContext is null", changeContext);
    Change change = changeContext.getChange();
    Validator.notNullCheck("FileChange is null", change);
    Validator.notNullCheck("ChangeType is null for change:" + change.getFilePath(), change.getChangeType());

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
    public int compare(Change o1, Change o2) {
      return (findOrdinal(o1.getFilePath()) - findOrdinal(o2.getFilePath()));
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
  private void validateYaml(String yamlString) throws HarnessException {
    Yaml yamlObj = new Yaml();

    try {
      // We just load the yaml to see if its well formed.
      yamlObj.load(yamlString);
    } catch (Exception e) {
      throw new HarnessException(e);
    }
  }
}
