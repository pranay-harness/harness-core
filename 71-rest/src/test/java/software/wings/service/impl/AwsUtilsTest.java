package software.wings.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;
import static software.wings.utils.WingsTestConstants.HOST_NAME;

import com.google.inject.Inject;

import com.amazonaws.services.ec2.model.Filter;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.AwsInstanceFilter;
import software.wings.expression.ExpressionEvaluator;

import java.util.Collections;
import java.util.List;

public class AwsUtilsTest extends WingsBaseTest {
  @Mock private ExpressionEvaluator mockExpressionEvaluator;

  @InjectMocks @Inject private AwsUtils utils;

  @Test
  public void testGetHostnameFromPrivateDnsName() {
    assertThat(utils.getHostnameFromPrivateDnsName("ip-172-31-18-241.ec2.internal")).isEqualTo("ip-172-31-18-241");
  }

  @Test
  public void testGetHostnameFromConvention() {
    doReturn(HOST_NAME).when(mockExpressionEvaluator).substitute(anyString(), any());
    utils.getHostnameFromConvention(Collections.emptyMap(), HOST_NAME);
    verify(mockExpressionEvaluator).substitute(anyString(), any());
  }

  @Test
  public void testGetAwsFilters() {
    AwsInfrastructureMapping awsInfrastructureMapping =
        anAwsInfrastructureMapping()
            .withAwsInstanceFilter(AwsInstanceFilter.builder()
                                       .vpcIds(Collections.singletonList("vpc-id"))
                                       .securityGroupIds(Collections.singletonList("sg-id"))
                                       .build())
            .build();
    List<Filter> filters = utils.getAwsFilters(awsInfrastructureMapping);
    assertThat(filters).isNotNull();
    assertThat(filters.size()).isEqualTo(3);
    verifyFilter(filters.get(0), "instance-state-name", Collections.singletonList("running"));
    verifyFilter(filters.get(1), "vpc-id", Collections.singletonList("vpc-id"));
    verifyFilter(filters.get(2), "instance.group-id", Collections.singletonList("sg-id"));
  }

  private void verifyFilter(Filter filter, String name, List<String> values) {
    assertThat(filter).isNotNull();
    assertThat(filter.getName()).isEqualTo(name);
    assertThat(filter.getValues()).isEqualTo(values);
  }
}