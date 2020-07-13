package software.wings.graphql.datafetcher.ce.recommendation;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import graphql.schema.DataFetchingEnvironment;
import io.harness.ccm.cluster.entities.ClusterRecord;
import io.kubernetes.client.custom.Quantity;
import io.kubernetes.client.openapi.models.V1ResourceRequirements;
import io.kubernetes.client.openapi.models.V1ResourceRequirementsBuilder;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import software.wings.graphql.datafetcher.AbstractConnectionV2DataFetcher;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLContainerRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLK8SWorkloadRecommendationConnection;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLK8SWorkloadRecommendationConnection.QLK8SWorkloadRecommendationConnectionBuilder;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLK8sWorkloadFilter;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLK8sWorkloadRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLResourceEntry;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLResourceRequirement;
import software.wings.graphql.datafetcher.ce.recommendation.dto.QLWorkloadTypeFilter;
import software.wings.graphql.datafetcher.ce.recommendation.entity.ContainerRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.K8sWorkloadRecommendation;
import software.wings.graphql.datafetcher.ce.recommendation.entity.K8sWorkloadRecommendation.K8sWorkloadRecommendationKeys;
import software.wings.graphql.datafetcher.ce.recommendation.entity.ResourceRequirement;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.time.Duration;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class K8sWorkloadRecommendationsDataFetcher extends AbstractConnectionV2DataFetcher<QLK8sWorkloadFilter,
    QLNoOpSortCriteria, QLK8SWorkloadRecommendationConnection> {
  private final LoadingCache<String, String> clusterNameCache =
      Caffeine.newBuilder()
          .expireAfterWrite(Duration.ofMinutes(5))
          .maximumSize(1000)
          .build(clusterId
              -> Optional.ofNullable(wingsPersistence.get(ClusterRecord.class, clusterId))
                     .map(ClusterRecord::getCluster)
                     .map(s -> s.getClusterName())
                     .orElse(""));

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLK8SWorkloadRecommendationConnection fetchConnection(List<QLK8sWorkloadFilter> filters,
      QLPageQueryParameters pageQueryParameters, List<QLNoOpSortCriteria> sortCriteria) {
    Query<K8sWorkloadRecommendation> query =
        populateFilters(wingsPersistence, filters, K8sWorkloadRecommendation.class, true)
            .order(Sort.descending(K8sWorkloadRecommendationKeys.estimatedSavings));
    QLK8SWorkloadRecommendationConnectionBuilder connectionBuilder = QLK8SWorkloadRecommendationConnection.builder();
    connectionBuilder.pageInfo(utils.populate(pageQueryParameters, query, k8sWorkloadRecommendation -> {
      Collection<? extends QLContainerRecommendation> containerRecommendations =
          entityToDtoCr(k8sWorkloadRecommendation.getContainerRecommendations());
      int numDays = containerRecommendations.stream().mapToInt(QLContainerRecommendation::getNumDays).max().orElse(0);
      connectionBuilder.node(QLK8sWorkloadRecommendation.builder()
                                 .clusterId(k8sWorkloadRecommendation.getClusterId())
                                 .clusterName(clusterNameCache.get(k8sWorkloadRecommendation.getClusterId()))
                                 .containerRecommendations(containerRecommendations)
                                 .namespace(k8sWorkloadRecommendation.getNamespace())
                                 .workloadName(k8sWorkloadRecommendation.getWorkloadName())
                                 .workloadType(k8sWorkloadRecommendation.getWorkloadType())
                                 .estimatedSavings(k8sWorkloadRecommendation.getEstimatedSavings())
                                 .numDays(numDays)
                                 .build());
    }));
    return connectionBuilder.build();
  }

  private Collection<? extends QLContainerRecommendation> entityToDtoCr(
      Map<String, ContainerRecommendation> containerRecommendations) {
    return containerRecommendations.values()
        .stream()
        .map(containerRecommendation
            -> QLContainerRecommendation.builder()
                   .containerName(containerRecommendation.getContainerName())
                   .current(entityToDto(containerRecommendation.getCurrent()))
                   .burstable(entityToDto(containerRecommendation.getBurstable()))
                   .guaranteed(entityToDto(containerRecommendation.getGuaranteed()))
                   .numDays(containerRecommendation.getNumDays())
                   .build())
        .collect(Collectors.toList());
  }

  private QLResourceRequirement entityToDto(ResourceRequirement resourceRequirement) {
    return QLResourceRequirement.builder()
        .requests(entityToDto(resourceRequirement.getRequests()))
        .limits(entityToDto(resourceRequirement.getLimits()))
        .yaml(resourceRequirementToYaml(resourceRequirement))
        .build();
  }

  private String resourceRequirementToYaml(ResourceRequirement resourceRequirement) {
    V1ResourceRequirementsBuilder builder = new V1ResourceRequirementsBuilder();
    resourceRequirement.getRequests().forEach((k, v) -> builder.addToRequests(k, Quantity.fromString(v)));
    resourceRequirement.getLimits().forEach((k, v) -> builder.addToLimits(k, Quantity.fromString(v)));
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
    return resourceEntryMap.entrySet()
        .stream()
        .sorted(Comparator.comparing(e -> e.getKey()))
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
        QLWorkloadTypeFilter workloadTypeFilter = filter.getWorkloadType();
        utils.setEnumFilter(field, workloadTypeFilter);
      }
    }
  }

  @Override
  protected QLK8sWorkloadFilter generateFilter(DataFetchingEnvironment environment, String key, String value) {
    return null;
  }
}
