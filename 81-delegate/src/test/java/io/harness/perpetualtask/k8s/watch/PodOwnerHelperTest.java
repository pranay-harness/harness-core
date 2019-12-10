package io.harness.perpetualtask.k8s.watch;

import static io.harness.rule.OwnerRule.VIKAS;
import static junit.framework.TestCase.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.Maps;

import io.fabric8.kubernetes.api.model.OwnerReference;
import io.fabric8.kubernetes.api.model.OwnerReferenceBuilder;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.api.model.PodBuilder;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.k8s.model.Kind;
import io.harness.perpetualtask.k8s.watch.functions.JobOwnerMappingFunction;
import io.harness.perpetualtask.k8s.watch.functions.PodOwnerMappingFunction;
import io.harness.rule.OwnerRule.Owner;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Map;
import java.util.UUID;

@RunWith(MockitoJUnitRunner.class)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PodOwnerHelperTest extends CategoryTest {
  KubernetesClient kubernetesClient;
  PodOwnerHelper podOwnerHelper;
  PodOwnerMappingFunction<OwnerReference, KubernetesClient, String, io.harness.perpetualtask.k8s.watch.Owner>
      podOwnerMappingFunction;
  Map<String,
      PodOwnerMappingFunction<OwnerReference, KubernetesClient, String, io.harness.perpetualtask.k8s.watch.Owner>>
      ownerKindToNameFunctionMap;
  public static final String POD_OWNER_ID = UUID.randomUUID().toString();
  public static final String POD_OWNER_NAME = "PepetualTaskJob-12432434";
  public static final String POD_PARENT_OWNER_NAME = "PepetualTaskJob";
  public static final String POD_PARENT_KIND_NAME = Kind.CronJob.name();
  public static final String POD_JOB_KIND = Kind.Job.name();
  public static final String POD_NAMESPACE = "harness-test";

  @Before
  public void setUp() {
    kubernetesClient = mock(KubernetesClient.class);
    podOwnerMappingFunction = mock(JobOwnerMappingFunction.class);
    ownerKindToNameFunctionMap = Maps.newConcurrentMap();
    ownerKindToNameFunctionMap.put(Kind.Job.name(), podOwnerMappingFunction);
    podOwnerHelper = new PodOwnerHelper(ownerKindToNameFunctionMap);
  }

  @Test
  @Owner(developers = VIKAS)
  @Category({UnitTests.class})
  public void testGetOwner() {
    Pod jobPod = getJobPod();
    when(podOwnerMappingFunction.apply(
             jobPod.getMetadata().getOwnerReferences().get(0), kubernetesClient, POD_NAMESPACE))
        .thenReturn(getOwner());
    io.harness.perpetualtask.k8s.watch.Owner owner = podOwnerHelper.getOwner(jobPod, kubernetesClient);
    assertEquals(POD_PARENT_OWNER_NAME, owner.getName());
    assertEquals(POD_PARENT_KIND_NAME, owner.getKind());
    verify(podOwnerMappingFunction, times(1))
        .apply(jobPod.getMetadata().getOwnerReferences().get(0), kubernetesClient, POD_NAMESPACE);

    owner = podOwnerHelper.getOwner(jobPod, kubernetesClient);
    assertEquals(POD_PARENT_OWNER_NAME, owner.getName());
    assertEquals(POD_PARENT_KIND_NAME, owner.getKind());
    verify(podOwnerMappingFunction, times(1))
        .apply(jobPod.getMetadata().getOwnerReferences().get(0), kubernetesClient, POD_NAMESPACE);
  }

  private Pod getJobPod() {
    return jobPodBuilder().build();
  }

  private PodBuilder jobPodBuilder() {
    return new PodBuilder()
        .withNewMetadata()
        .withNamespace(POD_NAMESPACE)
        .withOwnerReferences(
            new OwnerReferenceBuilder().withUid(POD_OWNER_ID).withKind(POD_JOB_KIND).withName(POD_OWNER_NAME).build())
        .endMetadata();
  }

  private io.harness.perpetualtask.k8s.watch.Owner getOwner() {
    return io.harness.perpetualtask.k8s.watch.Owner.newBuilder()
        .setUid(UUID.randomUUID().toString())
        .setKind(POD_PARENT_KIND_NAME)
        .setName(POD_PARENT_OWNER_NAME)
        .build();
  }
}