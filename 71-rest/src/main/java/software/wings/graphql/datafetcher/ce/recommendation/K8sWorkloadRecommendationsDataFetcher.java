package software.wings.graphql.datafetcher.ce.recommendation;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import graphql.schema.DataFetchingEnvironment;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.Sort;
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

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class K8sWorkloadRecommendationsDataFetcher extends AbstractConnectionV2DataFetcher<QLK8sWorkloadFilter,
    QLNoOpSortCriteria, QLK8SWorkloadRecommendationConnection> {
  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  protected QLK8SWorkloadRecommendationConnection fetchConnection(List<QLK8sWorkloadFilter> filters,
      QLPageQueryParameters pageQueryParameters, List<QLNoOpSortCriteria> sortCriteria) {
    Query<K8sWorkloadRecommendation> query =
        populateFilters(wingsPersistence, filters, K8sWorkloadRecommendation.class, true)
            .order(Sort.descending(K8sWorkloadRecommendationKeys.estimatedSavings));
    QLK8SWorkloadRecommendationConnectionBuilder connectionBuilder = QLK8SWorkloadRecommendationConnection.builder();
    connectionBuilder.pageInfo(utils.populate(pageQueryParameters, query,
        k8sWorkloadRecommendation
        -> connectionBuilder.node(
            QLK8sWorkloadRecommendation.builder()
                .containerRecommendations(entityToDto(k8sWorkloadRecommendation.getContainerRecommendations()))
                .namespace(k8sWorkloadRecommendation.getNamespace())
                .workloadName(k8sWorkloadRecommendation.getWorkloadName())
                .workloadType(k8sWorkloadRecommendation.getWorkloadType())
                .estimatedSavings(k8sWorkloadRecommendation.getEstimatedSavings())
                .build())));
    return connectionBuilder.build();
  }

  private Collection<? extends QLContainerRecommendation> entityToDto(
      List<ContainerRecommendation> containerRecommendations) {
    return containerRecommendations.stream()
        .map(containerRecommendation
            -> QLContainerRecommendation.builder()
                   .containerName(containerRecommendation.getContainerName())
                   .current(entityToDto(containerRecommendation.getCurrent()))
                   .burstable(entityToDto(containerRecommendation.getBurstable()))
                   .guaranteed(entityToDto(containerRecommendation.getGuaranteed()))
                   .build())
        .collect(Collectors.toList());
  }

  private QLResourceRequirement entityToDto(ResourceRequirement resourceRequirement) {
    return QLResourceRequirement.builder()
        .requests(entityToDto(resourceRequirement.getRequests()))
        .limits(entityToDto(resourceRequirement.getLimits()))
        .build();
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
