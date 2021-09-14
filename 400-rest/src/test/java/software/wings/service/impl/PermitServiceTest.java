/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.service.impl;

import static io.harness.rule.OwnerRule.SRINIVAS;

import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ARTIFACT_STREAM_ID;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;
import software.wings.beans.Permit;
import software.wings.service.intfc.PermitService;

import com.google.inject.Inject;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class PermitServiceTest extends WingsBaseTest {
  @InjectMocks @Inject private PermitService permitService;

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldAcquirePermit() {
    assertThat(acquirePermit()).isNotNull();
  }

  private String acquirePermit() {
    int leaseDuration = (int) (TimeUnit.MINUTES.toMillis(1) * PermitServiceImpl.getBackoffMultiplier(0));
    return permitService.acquirePermit(Permit.builder()
                                           .appId(APP_ID)
                                           .group("ARTIFACT_STREAM_GROUP")
                                           .key(ARTIFACT_STREAM_ID)
                                           .expireAt(new Date(System.currentTimeMillis() + leaseDuration))
                                           .leaseDuration(leaseDuration)
                                           .build());
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldReleasePermitByKey() {
    String permitId = acquirePermit();
    assertThat(permitId).isNotNull();
    assertThat(permitService.releasePermitByKey(ARTIFACT_STREAM_ID)).isTrue();
  }
}
