package io.harness.ccm.cluster;

import static io.harness.persistence.HQuery.excludeValidate;

import com.google.inject.Inject;

import io.harness.ccm.cluster.entities.K8sWorkload;
import io.harness.ccm.cluster.entities.K8sWorkload.K8sWorkloadKeys;
import io.harness.persistence.HPersistence;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Criteria;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public class K8sWorkloadDao {
  public static final String LABEL_FIELD = K8sWorkloadKeys.labels + ".";
  @Inject private HPersistence persistence;

  public void save(K8sWorkload k8sWorkload) {
    persistence.save(k8sWorkload);
  }

  // to get the workloads with at least one of the label(key:value) present
  public List<K8sWorkload> list(String accountId, Map<String, List<String>> labels) {
    if (labels == null) {
      return new ArrayList<>();
    }
    labels = labels.entrySet().stream().collect(Collectors.toMap(e -> encode(e.getKey()), Map.Entry::getValue));

    Query<K8sWorkload> query =
        persistence.createQuery(K8sWorkload.class, excludeValidate).field(K8sWorkloadKeys.accountId).equal(accountId);
    List<Criteria> criteriaList = new ArrayList<>();

    labels.forEach(
        (name, values) -> values.forEach(value -> criteriaList.add(query.criteria(LABEL_FIELD + name).equal(value))));

    query.or(criteriaList.toArray(new Criteria[0]));
    return query.asList(new FindOptions());
  }

  // to get the list of workloads having workload names in the given set and one of the label key equal to label name
  public List<K8sWorkload> list(String accountId, Set<String> workloadNames, String labelName) {
    labelName = encode(labelName);
    Query<K8sWorkload> query = persistence.createQuery(K8sWorkload.class, excludeValidate)
                                   .field(K8sWorkloadKeys.accountId)
                                   .equal(accountId)
                                   .field(K8sWorkloadKeys.name)
                                   .in(workloadNames);
    query.criteria(LABEL_FIELD + labelName).exists();
    return query.asList(new FindOptions());
  }

  public List<K8sWorkload> list(Query<K8sWorkload> query) {
    return query.asList(new FindOptions());
  }

  private String encode(String decoded) {
    return decoded.replace('.', '~');
  }
}
