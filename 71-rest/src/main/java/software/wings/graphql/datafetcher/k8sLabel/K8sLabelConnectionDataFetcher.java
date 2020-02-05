package software.wings.graphql.datafetcher.k8sLabel;

import com.google.inject.Inject;

import graphql.schema.DataFetchingEnvironment;
import io.harness.ccm.cluster.entities.K8sWorkload;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import software.wings.graphql.datafetcher.AbstractConnectionV2DataFetcher;
import software.wings.graphql.schema.query.QLPageQueryParameters;
import software.wings.graphql.schema.type.QLK8sLabel;
import software.wings.graphql.schema.type.QLK8sLabelConnection;
import software.wings.graphql.schema.type.QLK8sLabelConnection.QLK8sLabelConnectionBuilder;
import software.wings.graphql.schema.type.aggregation.QLNoOpSortCriteria;
import software.wings.graphql.schema.type.aggregation.k8sLabel.QLK8sLabelFilter;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class K8sLabelConnectionDataFetcher
    extends AbstractConnectionV2DataFetcher<QLK8sLabelFilter, QLNoOpSortCriteria, QLK8sLabelConnection> {
  @Inject K8sLabelQueryHelper k8sLabelQueryHelper;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.LOGGED_IN)
  public QLK8sLabelConnection fetchConnection(List<QLK8sLabelFilter> filters, QLPageQueryParameters pageQueryParameters,
      List<QLNoOpSortCriteria> sortCriteria) {
    Query<K8sWorkload> query = populateFilters(wingsPersistence, filters, K8sWorkload.class, true);

    Map<String, Set<String>> labels = new HashMap<>();
    QLK8sLabelConnectionBuilder connectionBuilder = QLK8sLabelConnection.builder();
    connectionBuilder.pageInfo(utils.populate(pageQueryParameters, query, k8sWorkload -> {
      if (k8sWorkload.getLabels() != null) {
        k8sWorkload.getLabels().keySet().forEach(key -> {
          if (!labels.containsKey(key)) {
            labels.put(key, new HashSet<>());
          }
          labels.get(key).add(k8sWorkload.getLabels().get(key));
        });
      }
    }));

    labels.keySet().forEach(key -> {
      QLK8sLabel node = QLK8sLabel.builder().name(key).values(labels.get(key).toArray(new String[0])).build();
      connectionBuilder.node(node);
    });

    return connectionBuilder.build();
  }

  @Override
  protected void populateFilters(List<QLK8sLabelFilter> filters, Query query) {
    k8sLabelQueryHelper.setQuery(filters, query);
  }

  @Override
  protected QLK8sLabelFilter generateFilter(DataFetchingEnvironment environment, String key, String value) {
    return null;
  }
}