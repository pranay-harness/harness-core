/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.pms.helpers;

import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import io.harness.OrchestrationTestBase;
import io.harness.account.AccountClient;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.FeatureFlag;
import io.harness.beans.FeatureName;
import io.harness.category.element.UnitTests;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableList;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.ExecutionException;
import org.joor.Reflect;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.Mockito;
import retrofit2.Call;
import retrofit2.Response;

@OwnedBy(HarnessTeam.PIPELINE)
public class PmsFeatureFlagHelperTest extends OrchestrationTestBase {
  private static final String accountId = "accountId";
  @Mock private AccountClient accountClient;

  private PmsFeatureFlagHelper pmsFeatureFlagHelper;

  @Before
  public void setUp() {
    pmsFeatureFlagHelper = new PmsFeatureFlagHelper();
    Reflect.on(pmsFeatureFlagHelper).set("accountClient", accountClient);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void isEnabled() throws IOException {
    Call<RestResponse<Collection<FeatureFlag>>> callMock = Mockito.mock(Call.class);
    when(accountClient.listAllFeatureFlagsForAccount(anyString())).thenReturn(callMock);
    when(callMock.execute())
        .thenReturn(Response.success(new RestResponse<>(ImmutableList.of(
            FeatureFlag.builder().enabled(true).name(FeatureName.RESOURCE_CONSTRAINT_MAX_QUEUE.name()).build()))));

    assertThat(pmsFeatureFlagHelper.isEnabled(accountId, FeatureName.RESOURCE_CONSTRAINT_MAX_QUEUE.name())).isTrue();
    assertThat(pmsFeatureFlagHelper.isEnabled(accountId, "dd")).isFalse();
    assertThat(pmsFeatureFlagHelper.isEnabled(accountId, FeatureName.RESOURCE_CONSTRAINT_MAX_QUEUE)).isTrue();
    assertThat(pmsFeatureFlagHelper.isEnabled(accountId, FeatureName.ARGO_PHASE1)).isFalse();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void updateCache() throws IOException, ExecutionException {
    Call<RestResponse<Collection<FeatureFlag>>> callMock = Mockito.mock(Call.class);
    when(accountClient.listAllFeatureFlagsForAccount(anyString())).thenReturn(callMock);
    when(callMock.execute())
        .thenReturn(Response.success(new RestResponse<>(ImmutableList.of(
            FeatureFlag.builder().enabled(true).name(FeatureName.RESOURCE_CONSTRAINT_MAX_QUEUE.name()).build()))));

    assertThat(pmsFeatureFlagHelper.isEnabled(accountId, FeatureName.RESOURCE_CONSTRAINT_MAX_QUEUE.name())).isTrue();

    pmsFeatureFlagHelper.updateCache(accountId, false, FeatureName.RESOURCE_CONSTRAINT_MAX_QUEUE.name());
    assertThat(pmsFeatureFlagHelper.isEnabled(accountId, FeatureName.RESOURCE_CONSTRAINT_MAX_QUEUE.name())).isFalse();

    pmsFeatureFlagHelper.updateCache(accountId, true, FeatureName.RESOURCE_CONSTRAINT_MAX_QUEUE.name());
    assertThat(pmsFeatureFlagHelper.isEnabled(accountId, FeatureName.RESOURCE_CONSTRAINT_MAX_QUEUE.name())).isTrue();
  }
}
