/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.SATYAM;

import static software.wings.beans.AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping;
import static software.wings.utils.WingsTestConstants.HOST_NAME;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.AwsInstanceFilter;
import software.wings.expression.ManagerExpressionEvaluator;

import com.amazonaws.services.ec2.model.Filter;
import com.google.inject.Inject;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@OwnedBy(CDP)
public class AwsUtilsTest extends WingsBaseTest {
  @Mock private ManagerExpressionEvaluator mockExpressionEvaluator;

  @InjectMocks @Inject private AwsUtils utils;

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testGetHostnameFromPrivateDnsName() {
    assertThat(utils.getHostnameFromPrivateDnsName("ip-172-31-18-241.ec2.internal")).isEqualTo("ip-172-31-18-241");
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testGetHostnameFromConvention() {
    doReturn(HOST_NAME).when(mockExpressionEvaluator).substitute(anyString(), any());
    utils.getHostnameFromConvention(Collections.emptyMap(), HOST_NAME);
    verify(mockExpressionEvaluator).substitute(anyString(), any());
  }

  @Test
  @Owner(developers = SATYAM)
  @Category(UnitTests.class)
  public void testGetAwsFilters() {
    AwsInfrastructureMapping awsInfrastructureMapping =
        anAwsInfrastructureMapping()
            .withAwsInstanceFilter(AwsInstanceFilter.builder().vpcIds(Collections.singletonList("vpc-id")).build())
            .build();
    List<Filter> filters = utils.getAwsFilters(awsInfrastructureMapping, null);
    assertThat(filters).isNotNull();
    assertThat(filters.size()).isEqualTo(2);
    verifyFilter(filters.get(0), "instance-state-name", Collections.singletonList("running"));
    verifyFilter(filters.get(1), "vpc-id", Collections.singletonList("vpc-id"));
  }

  private void verifyFilter(Filter filter, String name, List<String> values) {
    assertThat(filter).isNotNull();
    assertThat(filter.getName()).isEqualTo(name);
    assertThat(filter.getValues()).isEqualTo(values);
  }
}
