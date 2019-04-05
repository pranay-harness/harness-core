package software.wings.graphql.datafetcher.artifact;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.graphql.datafetcher.QueryOperationsEnum.DEPLOYED_ARTIFACTS;
import static software.wings.graphql.utils.GraphQLConstants.APP_ID;
import static software.wings.graphql.utils.GraphQLConstants.ARTIFACT_TYPE;
import static software.wings.graphql.utils.GraphQLConstants.ENV_ID;
import static software.wings.graphql.utils.GraphQLConstants.MAX_PAGE_SIZE_STR;
import static software.wings.graphql.utils.GraphQLConstants.NO_RECORDS_FOUND_FOR_ENTITIES;
import static software.wings.graphql.utils.GraphQLConstants.SERVICE_ID;
import static software.wings.graphql.utils.GraphQLConstants.ZERO_OFFSET_STR;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import graphql.schema.DataFetcher;
import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.beans.SortOrder;
import io.harness.beans.SortOrder.OrderType;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.graphql.datafetcher.AbstractDataFetcher;
import software.wings.graphql.schema.type.ArtifactInfo;
import software.wings.graphql.utils.GraphQLConstants;
import software.wings.service.impl.security.auth.AuthHandler;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.instance.InstanceService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Singleton
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ArtifactDataFetcher extends AbstractDataFetcher {
  public static final String LAST_UPDATED_AT_KEY = "lastUpdatedAt";
  public static final String LAST_ARTIFACT_ID_KEY = "lastArtifactId";
  public static final String LAST_DEPLOYED_AT_KEY = "lastDeployedAt";
  public static final String LAST_WORKFLOW_EXECUTION_NAME = "lastWorkflowExecutionName";
  public static final String LAST_DEPLOYED_BY_NAME_KEY = "lastDeployedByName";

  InstanceService instanceService;

  ArtifactService artifactService;

  @Inject
  public ArtifactDataFetcher(
      InstanceService instanceService, ArtifactService artifactService, AuthHandler authHandler) {
    super(authHandler);
    this.instanceService = instanceService;
    this.artifactService = artifactService;
  }

  @Override
  public Map<String, DataFetcher<?>> getOperationToDataFetcherMap() {
    return ImmutableMap.<String, DataFetcher<?>>builder()
        .put(DEPLOYED_ARTIFACTS.getOperationName(), getSuccessFullyDeployedArtifacts())
        .build();
  }

  private DataFetcher<List<ArtifactInfo>> getSuccessFullyDeployedArtifacts() {
    return environment -> {
      List<ArtifactInfo> deployedArtifactList = Lists.newArrayList();

      String appId = environment.getArgument(GraphQLConstants.APP_ID);
      if (StringUtils.isBlank(appId)) {
        addInvalidInputDebugInfo(deployedArtifactList, GraphQLConstants.APP_ID);
        return deployedArtifactList;
      }

      String serviceId = environment.getArgument(GraphQLConstants.SERVICE_ID);
      if (StringUtils.isBlank(serviceId)) {
        addInvalidInputDebugInfo(deployedArtifactList, GraphQLConstants.SERVICE_ID);
        return deployedArtifactList;
      }

      String envId = environment.getArgument(GraphQLConstants.ENV_ID);
      if (StringUtils.isBlank(envId)) {
        addInvalidInputDebugInfo(deployedArtifactList, GraphQLConstants.ENV_ID);
        return deployedArtifactList;
      }

      // At present, we are not getting limit/offset from query inputs.
      int limit = getPageLimit(environment);
      int offset = getPageOffset(environment);

      PageResponse<Instance> instancePageResponse = getDeployedInstances(appId, envId, serviceId, limit, offset);

      Map<String, List<Instance>> artifactToInstanceMap = Maps.newHashMap();
      for (Instance instance : instancePageResponse.getResponse()) {
        artifactToInstanceMap.computeIfAbsent(instance.getLastArtifactId(), k -> new ArrayList<>()).add(instance);
      }

      if (!artifactToInstanceMap.isEmpty()) {
        PageResponse<Artifact> artifactPageResponse = getArtifacts(appId, artifactToInstanceMap.keySet().toArray());

        Map<String, Artifact> artifactMap =
            artifactPageResponse.stream().collect(Collectors.toMap(a -> a.getUuid(), a -> a));

        deployedArtifactList = instancePageResponse.stream()
                                   .map(i -> {
                                     Artifact artifact = artifactMap.get(i.getLastArtifactId());

                                     ArtifactInfo artifactInfo =
                                         ArtifactInfo.builder()
                                             .lastDeployedAt(i.getLastDeployedAt())
                                             .lastDeployedBy(i.getLastDeployedByName())
                                             .workflowExecutionName(i.getLastWorkflowExecutionName())
                                             .pipelineExecutionName(i.getLastPipelineExecutionName())
                                             .id(i.getLastArtifactId())
                                             .buildNo(artifact != null ? artifact.getBuildNo() : null)
                                             .displayName(artifact != null ? artifact.getDisplayName() : null)
                                             .sourceName(artifact != null ? artifact.getArtifactSourceName() : null)
                                             .build();

                                     return artifactInfo;
                                   })
                                   .collect(Collectors.toList());
      }

      if (deployedArtifactList.isEmpty()) {
        addNoRecordFoundDebugInfo(
            deployedArtifactList, NO_RECORDS_FOUND_FOR_ENTITIES, ARTIFACT_TYPE, appId, envId, serviceId);
      }

      return deployedArtifactList;
    };
  }

  private PageResponse<Artifact> getArtifacts(String appId, Object[] ids) {
    PageRequest<Artifact> aritifactPageRequest = aPageRequest()
                                                     .addFilter(APP_ID, Operator.EQ, appId)
                                                     .addFilter(ID_KEY, Operator.IN, ids)
                                                     .withLimit(MAX_PAGE_SIZE_STR)
                                                     .withOffset(ZERO_OFFSET_STR)
                                                     .build();
    return artifactService.list(aritifactPageRequest, false);
  }

  private PageResponse<Instance> getDeployedInstances(
      String appId, String envId, String serviceId, int limit, int offset) {
    PageRequest<Instance> instancePageRequest =
        aPageRequest()
            .addFilter(APP_ID, Operator.EQ, appId)
            .addFilter(ENV_ID, Operator.EQ, envId)
            .addFilter(SERVICE_ID, Operator.EQ, serviceId)
            .addFilter(LAST_ARTIFACT_ID_KEY, Operator.EXISTS)
            .withLimit(String.valueOf(limit))
            .withOffset(String.valueOf(offset))
            .addFieldsIncluded(
                LAST_ARTIFACT_ID_KEY, LAST_DEPLOYED_AT_KEY, LAST_WORKFLOW_EXECUTION_NAME, LAST_DEPLOYED_BY_NAME_KEY)
            .addOrder(SortOrder.Builder.aSortOrder().withField(LAST_UPDATED_AT_KEY, OrderType.DESC).build())
            .build();

    return this.instanceService.list(instancePageRequest);
  }

  private void addInvalidInputDebugInfo(List<ArtifactInfo> artifactInfoList, String entityName) {
    ArtifactInfo artifactInfo = ArtifactInfo.builder().build();
    artifactInfoList.add(artifactInfo);
    addInvalidInputInfo(artifactInfo, entityName);
  }

  private void addNoRecordFoundDebugInfo(List<ArtifactInfo> artifactInfoList, String messageString, Object... values) {
    ArtifactInfo artifactInfo = ArtifactInfo.builder().build();
    artifactInfoList.add(artifactInfo);
    addNoRecordFoundInfo(artifactInfo, messageString, values);
  }
}
