package software.wings.graphql.datafetcher.ce.recommendation;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.ccm.cluster.dao.ClusterRecordDao;
import io.harness.ccm.cluster.entities.Cluster;
import io.harness.ccm.cluster.entities.ClusterRecord;

import software.wings.graphql.datafetcher.AbstractConnectionV2DataFetcher;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLContainerRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLK8SWorkloadRecommendationConnection;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLK8SWorkloadRecommendationConnection.QLK8SWorkloadRecommendationConnectionBuilder;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLK8sWorkloadFilter;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLK8sWorkloadRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLResourceEntry;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLResourceRequirement;
import software.wings.graphql.datafetcher.ce.recommendation.entity.ContainerRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.K8sWorkloadRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.K8sWorkloadRecommendation.K8sWorkloadRecommendationKeys;
import software.wings.graphql.datafetcher.ce.recommendation.entity.ResourceRequirement;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import graphql.schema.DataFetchingEnvironment;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.models.V1ResourceRequirements;
import io.kubernetes.client.openapi.models.V1ResourceRequirementsBuilder;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

@Slf4j
@Singleton
public class K8sWorkloadRecommendationsDataFetcher extends AbstractConnectionV2DataFetcher<QLK8sWorkloadFilter,
    QLNoOpSortCriteria, QLK8SWorkloadRecommendationConnection> {
  private final LoadingCache<String, String> clusterNameCache;

  @Inject
  public K8sWorkloadRecommendationsDataFetcher(final ClusterRecordDao clusterRecordDao) {
    clusterNameCache = Caffeine.newBuilder()
                           .expireAfterWrite(Duration.ofMinutes(5))
                           .maximumSize(1000)
                           .build(clusterId
                               -> Optional.ofNullable(clusterRecordDao.get(clusterId))
                                      .map(ClusterRecord::getCluster)
                                      .map(Cluster::getClusterName)
                                      .orElse(""));
  }

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLK8SWorkloadRecommendationConnection fetchConnection(List<QLK8sWorkloadFilter> filters,
      QLPageQueryParameters pageQueryParameters, List<QLNoOpSortCriteria> sortCriteria) {
    @SuppressWarnings("unchecked")
    Query<K8sWorkloadRecommendation> query =
        ((Query<K8sWorkloadRecommendation>) populateFilters(
             wingsPersistence, filters, K8sWorkloadRecommendation.class, true))
            .order(Sort.descending(K8sWorkloadRecommendationKeys.estimatedSavings))
            .field(K8sWorkloadRecommendationKeys.validRecommendation)
            .equal(Boolean.TRUE)
            .field(K8sWorkloadRecommendationKeys.lastDayCostAvailable)
            .equal(Boolean.TRUE)
            .field(K8sWorkloadRecommendationKeys.numDays)
            .greaterThanOrEq(1)
            .field(K8sWorkloadRecommendationKeys.lastReceivedUtilDataAt)
            .greaterThanOrEq(Instant.now().truncatedTo(ChronoUnit.DAYS).minus(Duration.ofDays(2)));
    QLK8SWorkloadRecommendationConnectionBuilder connectionBuilder = QLK8SWorkloadRecommendationConnection.builder();
    connectionBuilder.pageInfo(utils.populate(pageQueryParameters, query, k8sWorkloadRecommendation -> {
      Collection<? extends QLContainerRecommendation> containerRecommendations =
          entityToDtoCr(k8sWorkloadRecommendation.getContainerRecommendations());
      connectionBuilder.node(QLK8sWorkloadRecommendation.builder()
                                 .clusterId(k8sWorkloadRecommendation.getClusterId())
                                 .clusterName(clusterNameCache.get(k8sWorkloadRecommendation.getClusterId()))
                                 .containerRecommendations(containerRecommendations)
                                 .namespace(k8sWorkloadRecommendation.getNamespace())
                                 .workloadName(k8sWorkloadRecommendation.getWorkloadName())
                                 .workloadType(k8sWorkloadRecommendation.getWorkloadType())
                                 .estimatedSavings(k8sWorkloadRecommendation.getEstimatedSavings())
                                 .numDays(k8sWorkloadRecommendation.getNumDays())
                                 .build());
    }));
    return connectionBuilder.build();
  }

  private Collection<? extends QLContainerRecommendation> entityToDtoCr(
      Map<String, ContainerRecommendation> containerRecommendations) {
    return containerRecommendations.entrySet()
        .stream()
        .map(containerRecommendationEntry -> {
          String containerName = containerRecommendationEntry.getKey();
          ContainerRecommendation containerRecommendation = containerRecommendationEntry.getValue();
          return QLContainerRecommendation.builder()
              .containerName(containerName)
              .current(entityToDto(containerRecommendation.getCurrent()))
              .burstable(entityToDto(containerRecommendation.getBurstable()))
              .guaranteed(entityToDto(containerRecommendation.getGuaranteed()))
              .recommended(entityToDto(containerRecommendation.getRecommended()))
              .numDays(containerRecommendation.getNumDays())
              .totalSamplesCount(containerRecommendation.getTotalSamplesCount())
              .build();
        })
        .collect(Collectors.toList());
  }

  private QLResourceRequirement entityToDto(ResourceRequirement resourceRequirement) {
    if (resourceRequirement == null) {
      return QLResourceRequirement.builder().build();
    }
    return QLResourceRequirement.builder()
        .requests(entityToDto(resourceRequirement.getRequests()))
        .limits(entityToDto(resourceRequirement.getLimits()))
        .yaml(resourceRequirementToYaml(resourceRequirement))
        .build();
  }

  private String resourceRequirementToYaml(ResourceRequirement resourceRequirement) {
    V1ResourceRequirementsBuilder builder = new V1ResourceRequirementsBuilder();
    Optional.ofNullable(resourceRequirement.getRequests())
        .orElseGet(Collections::emptyMap)
        .forEach((k, v) -> builder.addToRequests(k, Quantity.fromString(v)));
    Optional.ofNullable(resourceRequirement.getLimits())
        .orElseGet(Collections::emptyMap)
        .forEach((k, v) -> builder.addToLimits(k, Quantity.fromString(v)));
    V1ResourceRequirements resourceRequirements = builder.build();
    return getYaml().dump(resourceRequirements);
  }

  private Yaml getYaml() {
    DumperOptions options = new DumperOptions();
    options.setIndent(2);
    options.setPrettyFlow(false);
    options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
    return new Yaml(new io.kubernetes.client.util.Yaml.CustomConstructor(),
        new io.kubernetes.client.util.Yaml.CustomRepresenter(), options);
  }

  private List<QLResourceEntry> entityToDto(Map<String, String> resourceEntryMap) {
    if (isEmpty(resourceEntryMap)) {
      return Collections.emptyList();
    }
    return resourceEntryMap.entrySet()
        .stream()
        .sorted(Map.Entry.comparingByKey())
        .map(e -> QLResourceEntry.of(e.getKey(), e.getValue()))
        .collect(Collectors.toList());
  }

  @Override
  protected void populateFilters(List<QLK8sWorkloadFilter> filters, Query query) {
    if (isEmpty(filters)) {
      return;
    }
    for (QLK8sWorkloadFilter filter : filters) {
      FieldEnd<? extends Query<K8sWorkloadRecommendation>> field;

      if (filter.getCluster() != null) {
        field = query.field(K8sWorkloadRecommendationKeys.clusterId);
        QLIdFilter clusterFilter = filter.getCluster();
        utils.setIdFilter(field, clusterFilter);
      }
      if (filter.getNamespace() != null) {
        field = query.field(K8sWorkloadRecommendationKeys.namespace);
        QLIdFilter namespaceFilter = filter.getNamespace();
        utils.setIdFilter(field, namespaceFilter);
      }
      if (filter.getWorkloadName() != null) {
        field = query.field(K8sWorkloadRecommendationKeys.workloadName);
        QLIdFilter workloadNameFilter = filter.getWorkloadName();
        utils.setIdFilter(field, workloadNameFilter);
      }
      if (filter.getWorkloadType() != null) {
        field = query.field(K8sWorkloadRecommendationKeys.workloadType);
        QLIdFilter workloadTypeFilter = filter.getWorkloadType();
        utils.setIdFilter(field, workloadTypeFilter);
      }
    }
  }

  @Override
  protected QLK8sWorkloadFilter generateFilter(DataFetchingEnvironment environment, String key, String value) {
    return null;
  }
}
