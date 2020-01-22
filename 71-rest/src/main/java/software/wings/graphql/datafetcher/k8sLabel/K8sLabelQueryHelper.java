package software.wings.graphql.datafetcher.k8sLabel;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.beans.SettingAttribute;
import software.wings.graphql.datafetcher.DataFetcherUtils;
import software.wings.graphql.schema.type.aggregation.QLIdFilter;
import software.wings.graphql.schema.type.aggregation.k8sLabel.QLK8sLabelFilter;

import java.util.List;

@Singleton
public class K8sLabelQueryHelper {
  @Inject protected DataFetcherUtils utils;

  public void setQuery(List<QLK8sLabelFilter> filters, Query query) {
    if (isEmpty(filters)) {
      return;
    }

    filters.forEach(filter -> {
      FieldEnd<? extends Query<SettingAttribute>> field;

      if (filter.getCluster() != null) {
        field = query.field("clusterId");
        QLIdFilter clusterFilter = filter.getCluster();
        utils.setIdFilter(field, clusterFilter);
      }

      if (filter.getNamespace() != null) {
        field = query.field("namespace");
        QLIdFilter namespaceFilter = filter.getNamespace();
        utils.setIdFilter(field, namespaceFilter);
      }

      if (filter.getWorkloadName() != null) {
        field = query.field("name");
        QLIdFilter workloadName = filter.getWorkloadName();
        utils.setIdFilter(field, workloadName);
      }
    });
  }
}
