package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static software.wings.beans.Base.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.data.structure.UUIDGenerator;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.InstanceType;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.beans.infrastructure.instance.key.ContainerInstanceKey;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.instance.InstanceService;

import java.util.List;

/**
 *
 * @author rktummala
 */
public class InstanceServiceTest extends WingsBaseTest {
  @Mock private AccountService accountService;
  @Mock private AppService appService;
  @Mock private Account account;

  @InjectMocks @Inject private InstanceService instanceService;

  private String instanceId = UUIDGenerator.generateUuid();
  private String containerId = "containerId";
  private String clusterName = "clusterName";
  private String controllerName = "controllerName";
  private String serviceName = "serviceName";
  private String podName = "podName";
  private String controllerType = "controllerType";

  /**
   * Sets mocks.
   */
  @Before
  public void setupMocks() {
    when(accountService.get(anyString())).thenReturn(account);
    when(appService.exist(anyString())).thenReturn(true);
  }

  /**
   * Should save and read.
   *
   */
  @Test
  public void testSaveAndRead() {
    Instance instance = buildInstance();
    Instance savedInstance = instanceService.save(instance);
    compare(instance, savedInstance);

    Instance instanceFromGet = instanceService.get(instanceId);
    compare(savedInstance, instanceFromGet);
  }

  private Instance buildInstance() {
    return Instance.builder()
        .uuid(instanceId)
        .instanceInfo(KubernetesContainerInfo.builder()
                          .clusterName(clusterName)
                          .controllerName(controllerName)
                          .controllerType(controllerType)
                          .podName(podName)
                          .serviceName(serviceName)
                          .build())
        .instanceType(InstanceType.KUBERNETES_CONTAINER_INSTANCE)
        .accountId(GLOBAL_ACCOUNT_ID)
        .appId(GLOBAL_APP_ID)
        .infraMappingId(INFRA_MAPPING_ID)
        .containerInstanceKey(ContainerInstanceKey.builder().containerId(containerId).build())
        .build();
  }

  @Test
  public void testList() {
    Instance instance1 = buildInstance();

    Instance savedInstance1 = instanceService.save(instance1);

    Instance instance2 = Instance.builder()
                             .uuid(UUIDGenerator.generateUuid())
                             .instanceInfo(KubernetesContainerInfo.builder()
                                               .clusterName(clusterName)
                                               .controllerName(controllerName)
                                               .controllerType(controllerType)
                                               .podName(podName)
                                               .serviceName(serviceName)
                                               .build())
                             .instanceType(InstanceType.KUBERNETES_CONTAINER_INSTANCE)
                             .accountId(GLOBAL_ACCOUNT_ID)
                             .appId(GLOBAL_APP_ID)
                             .infraMappingId(INFRA_MAPPING_ID)
                             .containerInstanceKey(ContainerInstanceKey.builder().containerId(containerId).build())
                             .build();
    Instance savedInstance2 = instanceService.save(instance2);

    PageResponse pageResponse = instanceService.list(
        PageRequestBuilder.aPageRequest().addFilter("accountId", Operator.EQ, GLOBAL_ACCOUNT_ID).build());
    assertNotNull(pageResponse);
    List<Instance> instanceList = pageResponse.getResponse();
    assertThat(instanceList).isNotNull();
    assertThat(instanceList).hasSize(2);
  }

  @Test
  public void testUpdateAndRead() {
    Instance instance = buildInstance();
    Instance savedInstance = instanceService.save(instance);
    compare(instance, savedInstance);

    Instance instanceFromGet = instanceService.get(instanceId);
    compare(instance, instanceFromGet);

    instanceFromGet.setInfraMappingId("inframappingId1");

    Instance updatedInstance = instanceService.saveOrUpdate(instanceFromGet);
    compare(instanceFromGet, updatedInstance);

    instanceFromGet = instanceService.get(instanceId);
    compare(updatedInstance, instanceFromGet);
  }

  @Test
  public void testDelete() {
    Instance instance = buildInstance();
    instanceService.save(instance);

    boolean delete = instanceService.delete(Sets.newHashSet(instanceId));
    assertTrue(delete);

    Instance instanceAfterDelete = instanceService.get(instanceId);
    assertNull(instanceAfterDelete);
  }

  private void compare(Instance lhs, Instance rhs) {
    assertEquals(lhs.getUuid(), rhs.getUuid());
    assertEquals(lhs.getContainerInstanceKey().getContainerId(), rhs.getContainerInstanceKey().getContainerId());
    assertEquals(lhs.getInfraMappingId(), rhs.getInfraMappingId());
    assertEquals(lhs.getAccountId(), rhs.getAccountId());
    assertEquals(lhs.getAppId(), rhs.getAppId());
    assertEquals(lhs.getInstanceType(), rhs.getInstanceType());
    assertEquals(lhs.getInstanceType(), rhs.getInstanceType());
  }
}
