/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.overviewdashboard.remote;

import static io.harness.overviewdashboard.bean.OverviewDashboardRequestType.GET_PROJECT_LIST;
import static io.harness.rule.OwnerRule.DEEPAK;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.dto.ResponseDTO;
import io.harness.overviewdashboard.bean.RestCallRequest;
import io.harness.overviewdashboard.bean.RestCallResponse;
import io.harness.rule.Owner;

import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import retrofit2.Call;
import retrofit2.Response;

@OwnedBy(HarnessTeam.PL)
public class ParallelRestCallExecutorTest extends CategoryTest {
  @InjectMocks ParallelRestCallExecutor parallelRestCallExecutor;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void test_executeRestCalls() throws Exception {
    List<RestCallRequest> restCallRequestList = new ArrayList<>();
    Call<ResponseDTO<Object>> request = mock(Call.class);
    when(request.execute()).thenReturn(Response.success(ResponseDTO.newResponse()));
    restCallRequestList.add(RestCallRequest.builder().requestType(GET_PROJECT_LIST).request(request).build());
    restCallRequestList.add(RestCallRequest.builder().requestType(GET_PROJECT_LIST).request(request).build());
    List<RestCallResponse> restCallResponses = parallelRestCallExecutor.executeRestCalls(restCallRequestList);
    assertThat(restCallResponses).isNotNull();
    assertThat(restCallResponses.size()).isEqualTo(2);
  }
}
