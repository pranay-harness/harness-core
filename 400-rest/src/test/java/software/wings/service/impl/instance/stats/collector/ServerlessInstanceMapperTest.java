/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.instance.stats.collector;

import static io.harness.rule.OwnerRule.ROHIT_KUMAR;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.beans.infrastructure.instance.InvocationCount;
import software.wings.beans.infrastructure.instance.InvocationCount.InvocationCountKey;
import software.wings.beans.infrastructure.instance.ServerlessInstance;
import software.wings.beans.infrastructure.instance.info.AwsLambdaInstanceInfo;
import software.wings.beans.infrastructure.instance.stats.ServerlessInstanceStats;
import software.wings.beans.infrastructure.instance.stats.ServerlessInstanceStats.AggregateInvocationCount;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ServerlessInstanceMapperTest extends CategoryTest {
  @Before
  public void setUp() throws Exception {}

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void test_map() {
    final ServerlessInstanceMapper serverlessInstanceMapper = new ServerlessInstanceMapper(Instant.now(), "accountid");
    final ServerlessInstanceStats serverlessInstanceStats = serverlessInstanceMapper.map(getServerlessInstanceList());
    assertThat(serverlessInstanceStats.getAggregateCounts().size()).isEqualTo(3);
    for (AggregateInvocationCount aggregateCount : serverlessInstanceStats.getAggregateCounts()) {
      if (aggregateCount.getId().equals("appdid1")
          && aggregateCount.getInvocationCountKey() == InvocationCountKey.LAST_30_DAYS) {
        assertThat(aggregateCount.getInvocationCount()).isEqualTo(80);
      } else if (aggregateCount.getId().equals("appdid1")
          && aggregateCount.getInvocationCountKey() == InvocationCountKey.SINCE_LAST_DEPLOYED) {
        assertThat(aggregateCount.getInvocationCount()).isEqualTo(90);
      } else if (aggregateCount.getId().equals("appdid2")) {
        assertThat(aggregateCount.getInvocationCount()).isEqualTo(300);
      }
    }
  }

  private List<ServerlessInstance> getServerlessInstanceList() {
    final AwsLambdaInstanceInfo awsLambdaInstanceInfo =
        AwsLambdaInstanceInfo.builder()
            .invocationCountList(Collections.singletonList(
                InvocationCount.builder().key(InvocationCountKey.LAST_30_DAYS).count(10).build()))
            .build();
    final AwsLambdaInstanceInfo awsLambdaInstanceInfo1 =
        AwsLambdaInstanceInfo.builder()
            .invocationCountList(Collections.singletonList(
                InvocationCount.builder().key(InvocationCountKey.SINCE_LAST_DEPLOYED).count(90).build()))
            .build();
    final AwsLambdaInstanceInfo awsLambdaInstanceInfo2 =
        AwsLambdaInstanceInfo.builder()
            .invocationCountList(Collections.singletonList(
                InvocationCount.builder().key(InvocationCountKey.LAST_30_DAYS).count(300).build()))
            .build();
    final AwsLambdaInstanceInfo awsLambdaInstanceInfo3 =
        AwsLambdaInstanceInfo.builder()
            .invocationCountList(Collections.singletonList(
                InvocationCount.builder().key(InvocationCountKey.LAST_30_DAYS).count(70).build()))
            .build();
    final ServerlessInstance serverlessInstance =
        ServerlessInstance.builder().appId("appdid1").instanceInfo(awsLambdaInstanceInfo).build();
    final ServerlessInstance serverlessInstance1 =
        ServerlessInstance.builder().appId("appid1").instanceInfo(awsLambdaInstanceInfo1).build();
    final ServerlessInstance serverlessInstance2 =
        ServerlessInstance.builder().appId("appdid2").instanceInfo(awsLambdaInstanceInfo2).build();
    final ServerlessInstance serverlessInstance3 =
        ServerlessInstance.builder().appId("appdid1").instanceInfo(awsLambdaInstanceInfo3).build();

    return Arrays.asList(serverlessInstance, serverlessInstance1, serverlessInstance2, serverlessInstance3);
  }
}
