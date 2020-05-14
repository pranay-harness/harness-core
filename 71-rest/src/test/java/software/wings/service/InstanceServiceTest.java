package software.wings.service;

import static io.harness.rule.OwnerRule.RAMA;
import static junit.framework.TestCase.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static software.wings.beans.Account.GLOBAL_ACCOUNT_ID;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.utils.WingsTestConstants.INFRA_MAPPING_ID;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.beans.PageRequest.PageRequestBuilder;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.InstanceType;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.beans.infrastructure.instance.key.ContainerInstanceKey;
import software.wings.dl.WingsPersistence;
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

  @Inject private WingsPersistence wingsPersistence;
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
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void testSaveAndRead() {
    Instance instance = buildInstance();
    Instance savedInstance = instanceService.save(instance);
    compare(instance, savedInstance);

    Instance instanceFromGet = instanceService.get(instanceId, false);
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
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
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

    PageResponse<Instance> pageResponse = instanceService.list(
        PageRequestBuilder.aPageRequest().addFilter("accountId", Operator.EQ, GLOBAL_ACCOUNT_ID).build());
    assertThat(pageResponse).isNotNull();
    List<Instance> instanceList = pageResponse.getResponse();
    assertThat(instanceList).isNotNull();
    assertThat(instanceList).hasSize(2);

    instanceList = instanceService.getInstancesForAppAndInframapping(GLOBAL_APP_ID, INFRA_MAPPING_ID);
    assertThat(instanceList).hasSize(2);

    assertEquals(2, instanceService.getInstanceCount(GLOBAL_APP_ID, INFRA_MAPPING_ID));
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void testUpdateAndRead() {
    Instance instance = buildInstance();
    Instance savedInstance = instanceService.save(instance);
    compare(instance, savedInstance);

    Instance instanceFromGet = instanceService.get(instanceId, false);
    compare(instance, instanceFromGet);

    instanceFromGet.setInfraMappingId("inframappingId1");

    Instance updatedInstance = instanceService.update(instanceFromGet, savedInstance.getUuid());
    compare(instanceFromGet, updatedInstance);

    instanceFromGet = instanceService.get(instanceId, false);
    compare(updatedInstance, instanceFromGet);
  }

  @Test
  @Owner(developers = RAMA)
  @Category(UnitTests.class)
  public void testDelete() {
    Instance instance = buildInstance();
    instanceService.save(instance);

    instanceService.delete(Sets.newHashSet(instanceId));

    Instance instanceAfterDelete = instanceService.get(instanceId, false);
    assertThat(instanceAfterDelete).isNull();

    instanceAfterDelete = instanceService.get(instanceId, true);
    assertThat(instanceAfterDelete).isNotNull();
  }

  private void compare(Instance lhs, Instance rhs) {
    //    assertThat( rhs.getUuid()).isEqualTo(lhs.getUuid());
    assertThat(rhs.getContainerInstanceKey().getContainerId())
        .isEqualTo(lhs.getContainerInstanceKey().getContainerId());
    assertThat(rhs.getInfraMappingId()).isEqualTo(lhs.getInfraMappingId());
    assertThat(rhs.getAccountId()).isEqualTo(lhs.getAccountId());
    assertThat(rhs.getAppId()).isEqualTo(lhs.getAppId());
    assertThat(rhs.getInstanceType()).isEqualTo(lhs.getInstanceType());
    assertThat(rhs.getInstanceType()).isEqualTo(lhs.getInstanceType());
  }
}
