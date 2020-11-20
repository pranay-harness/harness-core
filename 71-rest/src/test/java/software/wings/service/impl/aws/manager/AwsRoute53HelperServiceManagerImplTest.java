package software.wings.service.impl.aws.manager;

import static io.harness.rule.OwnerRule.SATYAM;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static software.wings.utils.WingsTestConstants.APP_ID;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.AwsConfig;
import software.wings.service.impl.aws.model.AwsRoute53HostedZoneData;
import software.wings.service.impl.aws.model.AwsRoute53ListHostedZonesResponse;
import software.wings.service.intfc.DelegateService;

import java.util.List;

public class AwsRoute53HelperServiceManagerImplTest extends CategoryTest {
  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testListHostedZones() throws InterruptedException {
    AwsRoute53HelperServiceManagerImpl service = spy(AwsRoute53HelperServiceManagerImpl.class);
    DelegateService mockDelegateService = mock(DelegateService.class);
    on(service).set("delegateService", mockDelegateService);
    doReturn(AwsRoute53ListHostedZonesResponse.builder()
                 .hostedZones(singletonList(
                     AwsRoute53HostedZoneData.builder().hostedZoneId("h-id").hostedZoneName("h-name").build()))
                 .build())
        .when(mockDelegateService)
        .executeTask(any());
    AwsHelperServiceManager mockHelper = mock(AwsHelperServiceManager.class);
    on(service).set("helper", mockHelper);
    doNothing().when(mockHelper).validateDelegateSuccessForSyncTask(any());
    List<AwsRoute53HostedZoneData> data =
        service.listHostedZones(AwsConfig.builder().build(), emptyList(), "us-east-1", APP_ID);
    assertThat(data).isNotNull();
    assertThat(data.size()).isEqualTo(1);
    assertThat(data.get(0).getHostedZoneId()).isEqualTo("h-id");
    assertThat(data.get(0).getHostedZoneName()).isEqualTo("h-name");
  }
}
