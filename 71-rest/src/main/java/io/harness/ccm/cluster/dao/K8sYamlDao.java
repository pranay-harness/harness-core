package io.harness.ccm.cluster.dao;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.cluster.entities.K8sYaml;
import io.harness.ccm.cluster.entities.K8sYaml.K8sYamlKeys;
import io.harness.persistence.HPersistence;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Sort;

import java.util.Optional;

@Singleton
public class K8sYamlDao {
  private final HPersistence hPersistence;

  @Inject
  public K8sYamlDao(HPersistence hPersistence) {
    this.hPersistence = hPersistence;
  }

  public Optional<K8sYaml> fetchLatestYaml(String clusterId, String uid) {
    return Optional.ofNullable(hPersistence.createQuery(K8sYaml.class)
                                   .field(K8sYamlKeys.clusterId)
                                   .equal(clusterId)
                                   .field(K8sYamlKeys.uid)
                                   .equal(uid)
                                   .order(Sort.descending(K8sYamlKeys.resourceVersion))
                                   .get(new FindOptions().limit(1)));
  }

  public String ensureYamlSaved(String clusterId, String uid, String resourceVersion, String yaml) {
    K8sYaml latest = fetchLatestYaml(clusterId, uid).orElse(null);
    if (latest != null && yaml.equals(latest.getYaml())) {
      return latest.getUuid();
    } else {
      return hPersistence.save(
          K8sYaml.builder().clusterId(clusterId).uid(uid).resourceVersion(resourceVersion).yaml(yaml).build());
    }
  }
}
