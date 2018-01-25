package software.wings.integration.migration.legacy;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.dl.PageRequest.UNLIMITED;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import org.assertj.core.util.Objects;
import org.junit.Ignore;
import org.junit.Test;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.infrastructure.instance.ContainerDeploymentInfo;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.InstanceType;
import software.wings.beans.infrastructure.instance.info.EcsContainerInfo;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author rktummala on 10/14/17
 */
@Integration
@Ignore
public class ContainerInstanceCleanupMigratorUtil extends WingsBaseTest {
  @Inject private WingsPersistence wingsPersistence;

  @Test
  public void cleanupInstances() {
    PageRequest<ContainerDeploymentInfo> pageRequest = aPageRequest().withLimit(UNLIMITED).build();
    System.out.println("Retrieving Container deployment info");
    PageResponse<ContainerDeploymentInfo> pageResponse =
        wingsPersistence.query(ContainerDeploymentInfo.class, pageRequest);

    if (pageResponse.isEmpty() || isEmpty(pageResponse.getResponse())) {
      System.out.println("No Container deployment info found");
      return;
    }

    Set<String> containerSvcNameSet = pageResponse.getResponse()
                                          .stream()
                                          .map(ContainerDeploymentInfo::getContainerSvcName)
                                          .collect(Collectors.toSet());

    findOrphanInstancesAndDelete(containerSvcNameSet);
  }

  private void findOrphanInstancesAndDelete(Set<String> containerSvcNameSet) {
    PageRequest<Instance> pageRequest =
        aPageRequest()
            .withLimit(UNLIMITED)
            .addFilter("instanceType", Operator.IN, "ECS_CONTAINER_INSTANCE", "KUBERNETES_CONTAINER_INSTANCE")
            .build();
    System.out.println("Retrieving Container instances");
    PageResponse<Instance> pageResponse = wingsPersistence.query(Instance.class, pageRequest);
    if (pageResponse.isEmpty() || isEmpty(pageResponse.getResponse())) {
      System.out.println("No Container instance found");
      return;
    }

    Set<String> ecsContainerSvcNamesToBeDeleted = Sets.newHashSet();
    Set<String> kubeContainerSvcNamesToBeDeleted = Sets.newHashSet();
    pageResponse.getResponse().stream().forEach(instance -> {
      if (InstanceType.KUBERNETES_CONTAINER_INSTANCE.equals(instance.getInstanceType())) {
        KubernetesContainerInfo kubernetesContainerInfo =
            Objects.castIfBelongsToType(instance.getInstanceInfo(), KubernetesContainerInfo.class);
        if (!containerSvcNameSet.contains(kubernetesContainerInfo.getControllerName())) {
          kubeContainerSvcNamesToBeDeleted.add(kubernetesContainerInfo.getControllerName());
        }
      } else if (InstanceType.ECS_CONTAINER_INSTANCE.equals(instance.getInstanceType())) {
        EcsContainerInfo ecsContainerInfo =
            Objects.castIfBelongsToType(instance.getInstanceInfo(), EcsContainerInfo.class);
        if (!containerSvcNameSet.contains(ecsContainerInfo.getServiceName())) {
          ecsContainerSvcNamesToBeDeleted.add(ecsContainerInfo.getServiceName());
        }
      }
    });

    deleteOrphanContainerInstances(kubeContainerSvcNamesToBeDeleted, InstanceType.KUBERNETES_CONTAINER_INSTANCE);
    deleteOrphanContainerInstances(ecsContainerSvcNamesToBeDeleted, InstanceType.ECS_CONTAINER_INSTANCE);
  }

  public void deleteOrphanContainerInstances(Set<String> containerSvcNameSetToBeDeleted, InstanceType instanceType) {
    String fieldName = null;
    if (InstanceType.KUBERNETES_CONTAINER_INSTANCE.equals(instanceType)) {
      fieldName = "instanceInfo.controllerName";
    } else if (InstanceType.ECS_CONTAINER_INSTANCE.equals(instanceType)) {
      fieldName = "instanceInfo.serviceName";
    }

    Query query = wingsPersistence.createAuthorizedQuery(Instance.class).disableValidation();
    query.field("instanceType").equal(instanceType);
    query.field(fieldName).in(containerSvcNameSetToBeDeleted);
    wingsPersistence.delete(query);
  }
}
