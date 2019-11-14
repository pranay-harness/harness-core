package io.harness.perpetualtask.k8s.watch;

import static io.harness.rule.OwnerRule.UNKNOWN;
import static org.assertj.core.api.Assertions.assertThat;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.ResourceRequirements;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.OwnerRule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.ArrayList;
import java.util.List;

public class K8sResourceUtilsTest extends CategoryTest {
  private ResourceRequirements resourceRequirements = new ResourceRequirements();
  private Container k8sContainer;
  private List<Container> k8sContainers = new ArrayList<>();

  @Before
  public void init() {
    k8sContainer = new io.fabric8.kubernetes.api.model.ContainerBuilder()
                       .withName("init-mydb")
                       .withImage("busybox")
                       .withResources(resourceRequirements)
                       .addToCommand("sh")
                       .addToCommand("-c")
                       .addToCommand("until nslookup mydb; do echo waiting for mydb; sleep 2; done;")
                       .build();

    k8sContainers.add(k8sContainer);
    k8sContainers.add(k8sContainer);
  }

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testGetResource() {
    Resource actualResource = K8sResourceUtils.getResource(k8sContainer);
    assertThat(actualResource).isNotNull();
  }

  @Test
  @Owner(emails = UNKNOWN)
  @Category(UnitTests.class)
  public void testGetTotalResourceRequest() {
    Resource actualResource = K8sResourceUtils.getTotalResourceRequest(k8sContainers);
    assertThat(actualResource).isNotNull();
  }
}
