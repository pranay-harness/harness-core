package io.harness;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.fabric8.utils.Lists;
import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.InstanceType;
import software.wings.beans.infrastructure.instance.info.AutoScalingGroupInstanceInfo;
import software.wings.beans.infrastructure.instance.info.CodeDeployInstanceInfo;
import software.wings.beans.infrastructure.instance.info.EcsContainerInfo;
import software.wings.beans.infrastructure.instance.info.K8sContainerInfo;
import software.wings.beans.infrastructure.instance.info.K8sPodInfo;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.beans.infrastructure.instance.info.PcfInstanceInfo;
import software.wings.beans.infrastructure.instance.info.PhysicalHostInstanceInfo;
import software.wings.graphql.datafetcher.instance.InstanceController;
import software.wings.graphql.scalar.GraphQLDateTimeScalar;
import software.wings.graphql.schema.type.QLInstance;
import software.wings.graphql.schema.type.QLInstance.QLInstanceBuilder;
import software.wings.graphql.schema.type.instance.info.QLInstanceType;

public class InstanceInfoControllerTest extends GraphQLMockBaseTest {
  @Inject InstanceController instanceController;

  private static final String TEST_ACCOUNT_ID = "TESTACCOUNTID";
  private static final String TEST_APPLICATION_ID = "TESTAPPID";
  private static final String TESTHOSTID = "TESTHOSTID";
  private static final String TESTHOSTNAME = "TESTHOSTNAME";
  private static final String TESTDNS = "TESTDNS";
  private static final String TESTENVID = "TESTENVID";
  private final String TESTSERVICEID = "TESTSERVICEID";

  @Test
  @Category(UnitTests.class)
  public void testPhysicalHostInstance() {
    Instance instance = Instance.builder()
                            .accountId(TEST_ACCOUNT_ID)
                            .appId(TEST_APPLICATION_ID)
                            .instanceType(InstanceType.PHYSICAL_HOST_INSTANCE)
                            .envId(TESTENVID)
                            .serviceId(TESTSERVICEID)
                            .instanceInfo(PhysicalHostInstanceInfo.builder()
                                              .hostId(TESTHOSTID)
                                              .hostName(TESTHOSTNAME)
                                              .hostPublicDns(TESTDNS)
                                              .build())
                            .build();

    QLInstanceBuilder builder = QLInstance.builder();
    instanceController.populateInstance(instance, builder);

    QLInstance qlInstance = builder.build();
    assertThat(qlInstance.getType().equals(QLInstanceType.PHYSICAL_HOST_INSTANCE));
    assertThat(qlInstance.getPhysicalHostInstanceInfo()).isNotNull();
    assertThat(qlInstance.getPhysicalHostInstanceInfo().getHostId()).isEqualTo(TESTHOSTID);
    assertThat(qlInstance.getPhysicalHostInstanceInfo().getHostName()).isEqualTo(TESTHOSTNAME);
    assertThat(qlInstance.getPhysicalHostInstanceInfo().getHostPublicDns()).isEqualTo(TESTDNS);
    assertThat(qlInstance.getEnvId()).isEqualTo(TESTENVID);
    assertThat(qlInstance.getServiceId()).isEqualTo(TESTSERVICEID);
  }

  @Test
  @Category(UnitTests.class)
  public void testAutoScalingGroupInstanceInfo() {
    final String TESTAUTOSCALINGGROUP = "TESTAUTOSCALINGGROUP";
    Instance instance = Instance.builder()
                            .accountId(TEST_ACCOUNT_ID)
                            .appId(TEST_APPLICATION_ID)
                            .instanceType(InstanceType.EC2_CLOUD_INSTANCE)
                            .envId(TESTENVID)
                            .serviceId(TESTSERVICEID)
                            .instanceInfo(AutoScalingGroupInstanceInfo.builder()
                                              .hostId(TESTHOSTID)
                                              .hostName(TESTHOSTNAME)
                                              .hostPublicDns(TESTDNS)
                                              .autoScalingGroupName(TESTAUTOSCALINGGROUP)
                                              .build())
                            .build();

    QLInstanceBuilder builder = QLInstance.builder();
    instanceController.populateInstance(instance, builder);

    QLInstance qlInstance = builder.build();
    assertThat(qlInstance.getType().equals(QLInstanceType.EC2_CLOUD_INSTANCE));
    assertThat(qlInstance.getEc2InstanceInfo()).isNotNull();
    assertThat(qlInstance.getEc2InstanceInfo().getHostId()).isEqualTo(TESTHOSTID);
    assertThat(qlInstance.getEc2InstanceInfo().getHostName()).isEqualTo(TESTHOSTNAME);
    assertThat(qlInstance.getEc2InstanceInfo().getHostPublicDns()).isEqualTo(TESTDNS);
    assertThat(qlInstance.getEc2InstanceInfo().getAutoScalingGroupName()).isEqualTo(TESTAUTOSCALINGGROUP);
    assertThat(qlInstance.getEc2InstanceInfo().getDeploymentId()).isNull();
    assertThat(qlInstance.getEnvId()).isEqualTo(TESTENVID);
    assertThat(qlInstance.getServiceId()).isEqualTo(TESTSERVICEID);
  }

  @Test
  @Category(UnitTests.class)
  public void testCodeDeployInstanceInfo() {
    final String TESTDEPLOYMENTID = "TESTDEPLOYMENTID";
    Instance instance = Instance.builder()
                            .accountId(TEST_ACCOUNT_ID)
                            .appId(TEST_APPLICATION_ID)
                            .instanceType(InstanceType.EC2_CLOUD_INSTANCE)
                            .envId(TESTENVID)
                            .serviceId(TESTSERVICEID)
                            .instanceInfo(CodeDeployInstanceInfo.builder()
                                              .hostId(TESTHOSTID)
                                              .hostName(TESTHOSTNAME)
                                              .hostPublicDns(TESTDNS)
                                              .deploymentId(TESTDEPLOYMENTID)
                                              .build())
                            .build();

    QLInstanceBuilder builder = QLInstance.builder();
    instanceController.populateInstance(instance, builder);

    QLInstance qlInstance = builder.build();
    assertThat(qlInstance.getType().equals(QLInstanceType.EC2_CLOUD_INSTANCE));
    assertThat(qlInstance.getEc2InstanceInfo()).isNotNull();
    assertThat(qlInstance.getEc2InstanceInfo().getHostId()).isEqualTo(TESTHOSTID);
    assertThat(qlInstance.getEc2InstanceInfo().getHostName()).isEqualTo(TESTHOSTNAME);
    assertThat(qlInstance.getEc2InstanceInfo().getHostPublicDns()).isEqualTo(TESTDNS);
    assertThat(qlInstance.getEc2InstanceInfo().getAutoScalingGroupName()).isNull();
    assertThat(qlInstance.getEc2InstanceInfo().getDeploymentId()).isEqualTo(TESTDEPLOYMENTID);
    assertThat(qlInstance.getEnvId()).isEqualTo(TESTENVID);
    assertThat(qlInstance.getServiceId()).isEqualTo(TESTSERVICEID);
  }

  @Test
  @Category(UnitTests.class)
  public void testEcsContainerInfo() {
    Instance instance = Instance.builder()
                            .accountId(TEST_ACCOUNT_ID)
                            .appId(TEST_APPLICATION_ID)
                            .instanceType(InstanceType.ECS_CONTAINER_INSTANCE)
                            .envId(TESTENVID)
                            .serviceId(TESTSERVICEID)
                            .instanceInfo(EcsContainerInfo.Builder.anEcsContainerInfo()
                                              .withClusterName("TESTCLUSTER")
                                              .withServiceName("TESTSERVICE")
                                              .withStartedAt(100)
                                              .withStartedBy("TESTUSER")
                                              .withTaskArn("TESTTASK")
                                              .withTaskDefinitionArn("TESTTASKDEF")
                                              .build())
                            .build();

    QLInstanceBuilder builder = QLInstance.builder();
    instanceController.populateInstance(instance, builder);

    QLInstance qlInstance = builder.build();
    assertThat(qlInstance.getType().equals(QLInstanceType.ECS_CONTAINER_INSTANCE));
    assertThat(qlInstance.getEcsContainerInfo()).isNotNull();
    assertThat(qlInstance.getEcsContainerInfo().getClusterName()).isEqualTo("TESTCLUSTER");
    assertThat(qlInstance.getEcsContainerInfo().getServiceName()).isEqualTo("TESTSERVICE");
    assertThat(qlInstance.getEcsContainerInfo().getStartedAt())
        .isEqualTo(GraphQLDateTimeScalar.convert(Long.valueOf(100)));
    assertThat(qlInstance.getEcsContainerInfo().getStartedBy()).isEqualTo("TESTUSER");
    assertThat(qlInstance.getEcsContainerInfo().getTaskArn()).isEqualTo("TESTTASK");
    assertThat(qlInstance.getEcsContainerInfo().getTaskDefinitionArn()).isEqualTo("TESTTASKDEF");
    assertThat(qlInstance.getEnvId()).isEqualTo(TESTENVID);
    assertThat(qlInstance.getServiceId()).isEqualTo(TESTSERVICEID);
  }

  @Test
  @Category(UnitTests.class)
  public void testK8sPodInfo() {
    Instance instance = Instance.builder()
                            .accountId(TEST_ACCOUNT_ID)
                            .appId(TEST_APPLICATION_ID)
                            .instanceType(InstanceType.KUBERNETES_CONTAINER_INSTANCE)
                            .envId(TESTENVID)
                            .serviceId(TESTSERVICEID)
                            .instanceInfo(K8sPodInfo.builder()
                                              .ip("TESTIP")
                                              .namespace("TESTNAMESPACE")
                                              .podName("TESTPOD")
                                              .releaseName("TESTRELEASE")
                                              .clusterName("TESTCLUSTER")
                                              .containers(Lists.newArrayList(K8sContainerInfo.builder()
                                                                                 .containerId("TESTCONTAINER")
                                                                                 .image("TESTIMAGE")
                                                                                 .name("TESTNAME")
                                                                                 .build()))
                                              .build())
                            .build();

    QLInstanceBuilder builder = QLInstance.builder();
    instanceController.populateInstance(instance, builder);

    QLInstance qlInstance = builder.build();
    assertThat(qlInstance.getType().equals(QLInstanceType.KUBERNETES_CONTAINER_INSTANCE));
    assertThat(qlInstance.getK8sPodInfo()).isNotNull();
    assertThat(qlInstance.getK8sPodInfo().getClusterName()).isEqualTo("TESTCLUSTER");
    assertThat(qlInstance.getK8sPodInfo().getIp()).isEqualTo("TESTIP");
    assertThat(qlInstance.getK8sPodInfo().getNamespace()).isEqualTo("TESTNAMESPACE");
    assertThat(qlInstance.getK8sPodInfo().getPodName()).isEqualTo("TESTPOD");
    assertThat(qlInstance.getK8sPodInfo().getReleaseName()).isEqualTo("TESTRELEASE");
    assertThat(qlInstance.getK8sPodInfo().getClusterName()).isEqualTo("TESTCLUSTER");
    assertThat(qlInstance.getK8sPodInfo().getContainers().get(0).getContainerId()).isEqualTo("TESTCONTAINER");
    assertThat(qlInstance.getK8sPodInfo().getContainers().get(0).getImage()).isEqualTo("TESTIMAGE");
    assertThat(qlInstance.getK8sPodInfo().getContainers().get(0).getName()).isEqualTo("TESTNAME");
    assertThat(qlInstance.getEnvId()).isEqualTo(TESTENVID);
    assertThat(qlInstance.getServiceId()).isEqualTo(TESTSERVICEID);
  }

  @Test
  @Category(UnitTests.class)
  public void testKubernetesContainerInfo() {
    Instance instance = Instance.builder()
                            .accountId(TEST_ACCOUNT_ID)
                            .appId(TEST_APPLICATION_ID)
                            .instanceType(InstanceType.KUBERNETES_CONTAINER_INSTANCE)
                            .envId(TESTENVID)
                            .serviceId(TESTSERVICEID)
                            .instanceInfo(KubernetesContainerInfo.builder()
                                              .ip("TESTIP")
                                              .namespace("TESTNAMESPACE")
                                              .podName("TESTPOD")
                                              .clusterName("TESTCLUSTER")
                                              .build())
                            .build();

    QLInstanceBuilder builder = QLInstance.builder();
    instanceController.populateInstance(instance, builder);

    QLInstance qlInstance = builder.build();
    assertThat(qlInstance.getType().equals(QLInstanceType.KUBERNETES_CONTAINER_INSTANCE));
    assertThat(qlInstance.getK8sPodInfo()).isNotNull();
    assertThat(qlInstance.getK8sPodInfo().getClusterName()).isEqualTo("TESTCLUSTER");
    assertThat(qlInstance.getK8sPodInfo().getIp()).isEqualTo("TESTIP");
    assertThat(qlInstance.getK8sPodInfo().getNamespace()).isEqualTo("TESTNAMESPACE");
    assertThat(qlInstance.getK8sPodInfo().getPodName()).isEqualTo("TESTPOD");
    assertThat(qlInstance.getK8sPodInfo().getReleaseName()).isNull();
    assertThat(qlInstance.getK8sPodInfo().getContainers()).isNull();
    assertThat(qlInstance.getEnvId()).isEqualTo(TESTENVID);
    assertThat(qlInstance.getServiceId()).isEqualTo(TESTSERVICEID);
  }

  @Test
  @Category(UnitTests.class)
  public void testPcfInstanceInfo() {
    Instance instance = Instance.builder()
                            .accountId(TEST_ACCOUNT_ID)
                            .appId(TEST_APPLICATION_ID)
                            .instanceType(InstanceType.PCF_INSTANCE)
                            .envId(TESTENVID)
                            .serviceId(TESTSERVICEID)
                            .instanceInfo(PcfInstanceInfo.builder()
                                              .id("TESTID")
                                              .organization("TESTORG")
                                              .space("TESTSPACE")
                                              .pcfApplicationName("PCFAPPLICATIONNAME")
                                              .pcfApplicationGuid("PCFAPPLICATIONGUID")
                                              .instanceIndex("INSTANCEINDEX")
                                              .build())
                            .build();

    QLInstanceBuilder builder = QLInstance.builder();
    instanceController.populateInstance(instance, builder);

    QLInstance qlInstance = builder.build();
    assertThat(qlInstance.getType().equals(QLInstanceType.PCF_INSTANCE));
    assertThat(qlInstance.getPcfInstanceInfo()).isNotNull();
    assertThat(qlInstance.getPcfInstanceInfo().getId()).isEqualTo("TESTID");
    assertThat(qlInstance.getPcfInstanceInfo().getOrganization()).isEqualTo("TESTORG");
    assertThat(qlInstance.getPcfInstanceInfo().getSpace()).isEqualTo("TESTSPACE");
    assertThat(qlInstance.getPcfInstanceInfo().getPcfApplicationName()).isEqualTo("PCFAPPLICATIONNAME");
    assertThat(qlInstance.getPcfInstanceInfo().getPcfApplicationGuid()).isEqualTo("PCFAPPLICATIONGUID");
    assertThat(qlInstance.getPcfInstanceInfo().getInstanceIndex()).isEqualTo("INSTANCEINDEX");
    assertThat(qlInstance.getEnvId()).isEqualTo(TESTENVID);
    assertThat(qlInstance.getServiceId()).isEqualTo(TESTSERVICEID);
  }
}
