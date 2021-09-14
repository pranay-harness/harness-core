/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl.aws.manager;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.rule.OwnerRule.SATYAM;

import static software.wings.utils.WingsTestConstants.APP_ID;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joor.Reflect.on;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.beans.AwsConfig;
import software.wings.service.impl.aws.model.AwsRoute53HostedZoneData;
import software.wings.service.impl.aws.model.AwsRoute53ListHostedZonesResponse;
import software.wings.service.intfc.DelegateService;

import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(CDP)
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
