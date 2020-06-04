package software.wings.service.impl.aws.manager;

import static io.harness.rule.OwnerRule.SATYAM;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static software.wings.utils.WingsTestConstants.APP_ID;

import com.google.common.collect.ImmutableMap;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.AwsConfig;
import software.wings.service.impl.aws.model.AwsIamListInstanceRolesResponse;
import software.wings.service.impl.aws.model.AwsIamListRolesResponse;
import software.wings.service.intfc.DelegateService;

import java.util.List;
import java.util.Map;

public class AwsIamHelperServiceManagerImplTest extends CategoryTest {
  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testListIamRoles() throws InterruptedException {
    AwsIamHelperServiceManagerImpl service = spy(AwsIamHelperServiceManagerImpl.class);
    DelegateService mockDelegateService = mock(DelegateService.class);
    on(service).set("delegateService", mockDelegateService);
    doReturn(AwsIamListRolesResponse.builder().roles(ImmutableMap.of("k1", "v1", "k2", "v2")).build())
        .when(mockDelegateService)
        .executeTask(any());
    Map<String, String> roles = service.listIamRoles(AwsConfig.builder().build(), emptyList(), APP_ID);
    assertThat(roles).isNotNull();
    assertThat(roles.size()).isEqualTo(2);
    assertThat(roles.containsKey("k1")).isTrue();
    assertThat(roles.get("k1")).isEqualTo("v1");
    assertThat(roles.containsKey("k2")).isTrue();
    assertThat(roles.get("k2")).isEqualTo("v2");
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testListIamInstanceRoles() throws InterruptedException {
    AwsIamHelperServiceManagerImpl service = spy(AwsIamHelperServiceManagerImpl.class);
    DelegateService mockDelegateService = mock(DelegateService.class);
    on(service).set("delegateService", mockDelegateService);
    doReturn(AwsIamListInstanceRolesResponse.builder().instanceRoles(asList("r-0", "r-1")).build())
        .when(mockDelegateService)
        .executeTask(any());
    List<String> roles = service.listIamInstanceRoles(AwsConfig.builder().build(), emptyList(), APP_ID);
    assertThat(roles).isNotNull();
    assertThat(roles.size()).isEqualTo(2);
    assertThat(roles.contains("r-0")).isTrue();
    assertThat(roles.contains("r-1")).isTrue();
  }
}