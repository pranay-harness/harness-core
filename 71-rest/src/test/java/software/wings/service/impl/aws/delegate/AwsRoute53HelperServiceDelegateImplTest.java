package software.wings.service.impl.aws.delegate;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.amazonaws.services.route53.AmazonRoute53;
import com.amazonaws.services.route53.model.HostedZone;
import com.amazonaws.services.route53.model.ListHostedZonesResult;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.service.impl.aws.model.AwsRoute53HostedZoneData;
import software.wings.service.intfc.security.EncryptionService;

import java.util.List;

public class AwsRoute53HelperServiceDelegateImplTest extends WingsBaseTest {
  @Mock private EncryptionService mockEncryptionService;
  @Spy @InjectMocks private AwsRoute53HelperServiceDelegateImpl awsRoute53HelperServiceDelegate;

  @Test
  public void testListHostedZones() {
    AmazonRoute53 mockClient = mock(AmazonRoute53.class);
    doReturn(mockClient)
        .when(awsRoute53HelperServiceDelegate)
        .getAmazonRoute53Client(anyString(), anyString(), any(), anyBoolean());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    doReturn(new ListHostedZonesResult().withHostedZones(new HostedZone().withId("id").withName("name")))
        .when(mockClient)
        .listHostedZones();
    List<AwsRoute53HostedZoneData> zoneData =
        awsRoute53HelperServiceDelegate.listHostedZones(AwsConfig.builder().build(), emptyList(), "us-east-1");
    assertThat(zoneData).isNotNull();
    assertThat(zoneData.size()).isEqualTo(1);
    assertThat(zoneData.get(0).getHostedZoneId()).isEqualTo("id");
    assertThat(zoneData.get(0).getHostedZoneName()).isEqualTo("name");
  }

  @Test
  public void testUpsertRoute53ParentRecord() {
    AmazonRoute53 mockClient = mock(AmazonRoute53.class);
    doReturn(mockClient)
        .when(awsRoute53HelperServiceDelegate)
        .getAmazonRoute53Client(anyString(), anyString(), any(), anyBoolean());
    doReturn(null).when(mockEncryptionService).decrypt(any(), anyList());
    awsRoute53HelperServiceDelegate.upsertRoute53ParentRecord(
        AwsConfig.builder().build(), emptyList(), "us-east-1", "parent", "id", 100, "blue", 0, "green", 60);
    verify(mockClient).changeResourceRecordSets(any());
  }
}