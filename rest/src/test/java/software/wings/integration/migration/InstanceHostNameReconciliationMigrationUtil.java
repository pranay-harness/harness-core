package software.wings.integration.migration;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;

import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Migration script to merge duplicate instance entries based on host name
 * @author brett on 10/12/17
 */
@Integration
@Ignore
public class InstanceHostNameReconciliationMigrationUtil extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;

  @Test
  public void mergeDuplicateHostNames() {
    System.out.println("Finding duplicate host names");

    List<Instance> instances = wingsPersistence.createQuery(Instance.class).asList();
    Multimap<String, Instance> hostNameInstances = ArrayListMultimap.create();
    for (Instance instance : instances) {
      if (instance.getInfraMappingType().equals(InfrastructureMappingType.AWS_SSH.name())) {
        String hostName = instance.getHostInstanceKey().getHostName();
        String key = hostName.split("\\.")[0] + instance.getHostInstanceKey().getInfraMappingId();
        hostNameInstances.put(key, instance);
      }
    }

    for (String key : hostNameInstances.keySet()) {
      List<Instance> matchingInstances = new ArrayList<>(hostNameInstances.get(key));
      matchingInstances.sort(Comparator.comparingInt(o -> o.getHostInstanceKey().getHostName().length()));
      Instance keepInstance = matchingInstances.get(0);
      List<Instance> deleteInstances =
          matchingInstances.stream().filter(instance -> instance != keepInstance).collect(Collectors.toList());
      if (isNotEmpty(deleteInstances)) {
        System.out.println("\nKeeping: " + keepInstance.getHostInstanceKey().getHostName());
        System.out.println("Deleting: "
            + deleteInstances.stream()
                  .map(instance -> instance.getHostInstanceKey().getHostName())
                  .collect(Collectors.toList()));
        deleteInstances.forEach(instance -> wingsPersistence.delete(instance));
      }
    }
    System.out.println("\nReconciling host names completed");
  }
}
