package software.wings.service.impl;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import software.wings.WingsBaseTest;

/**
 * Created by anubhaw on 3/3/17.
 */
public class AwsHelperServiceTest extends WingsBaseTest {
  @Test
  public void shouldGetInstanceId() {
    AwsHelperService awsHelperService = new AwsHelperService();
    assertThat(awsHelperService.getHostnameFromDnsName("ip-172-31-18-241.ec2.internal")).isEqualTo("ip-172-31-18-241");
    assertThat(awsHelperService.getHostnameFromDnsName("ec2-184-73-35-194.compute-1.amazonaws.com"))
        .isEqualTo("ec2-184-73-35-194");
  }
}
