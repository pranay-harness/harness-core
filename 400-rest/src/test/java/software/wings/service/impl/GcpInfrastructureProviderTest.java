package software.wings.service.impl;

import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.rule.Owner;
import io.harness.rule.OwnerRule;

import software.wings.beans.GcpConfig;
import software.wings.beans.SettingAttribute;
import software.wings.beans.SyncTaskContext;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.service.intfc.ContainerService;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class GcpInfrastructureProviderTest extends CategoryTest {
  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Mock private ContainerService containerService;
  @InjectMocks private GcpInfrastructureProvider gcpInfrastructureProvider;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    doReturn(containerService)
        .when(delegateProxyFactory)
        .get(eq(ContainerService.class), Matchers.any(SyncTaskContext.class));
  }

  @Test
  @Owner(developers = OwnerRule.YOGESH)
  @Category(UnitTests.class)
  public void listClusterNames() {
    SettingAttribute settingAttribute =
        aSettingAttribute().withValue(GcpConfig.builder().useDelegate(false).build()).build();

    SettingAttribute settingAttributeDelegateBased =
        aSettingAttribute().withValue(GcpConfig.builder().useDelegate(true).delegateSelector("abc").build()).build();

    gcpInfrastructureProvider.listClusterNames(settingAttribute, null);
    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> gcpInfrastructureProvider.listClusterNames(settingAttributeDelegateBased, null))
        .withMessageContaining(
            "Infrastructure Definition Using a GCP Cloud Provider Inheriting from Delegate is not yet supported");
  }
}