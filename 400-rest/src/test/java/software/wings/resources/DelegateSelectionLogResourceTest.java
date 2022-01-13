/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.resources;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.MARKO;
import static io.harness.rule.OwnerRule.VUK;

import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.DelegateSelectionLogParams;
import io.harness.delegate.beans.DelegateSelectionLogResponse;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;

import software.wings.exception.WingsExceptionMapper;
import software.wings.service.intfc.DelegateSelectionLogsService;
import software.wings.utils.ResourceTestRule;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.GenericType;
import org.glassfish.hk2.utilities.binding.AbstractBinder;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class DelegateSelectionLogResourceTest extends CategoryTest {
  private static HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
  private static DelegateSelectionLogsService delegateSelectionLogsService = mock(DelegateSelectionLogsService.class);

  @ClassRule
  public static final ResourceTestRule RESOURCES =

      ResourceTestRule.builder()
          .instance(new DelegateSelectionLogResource(delegateSelectionLogsService))
          .instance(new AbstractBinder() {
            @Override
            protected void configure() {
              bind(httpServletRequest).to(HttpServletRequest.class);
            }
          })
          .type(WingsExceptionMapper.class)
          .build();

  @Test
  @Owner(developers = VUK)
  @Category(UnitTests.class)
  public void getSelectionLogs() {
    String accountId = generateUuid();
    String taskId = generateUuid();
    RestResponse<List<DelegateSelectionLogParams>> restResponse =
        RESOURCES.client()
            .target("/selection-logs?accountId=" + accountId + "&taskId=" + taskId)
            .request()
            .get(new GenericType<RestResponse<List<DelegateSelectionLogParams>>>() {});

    verify(delegateSelectionLogsService, atLeastOnce()).fetchTaskSelectionLogs(accountId, taskId);
  }

  @Test
  @Owner(developers = MARKO)
  @Category(UnitTests.class)
  public void getSelectionLogsV2() {
    String accountId = generateUuid();
    String taskId = generateUuid();
    RestResponse<DelegateSelectionLogResponse> restResponse =
        RESOURCES.client()
            .target("/selection-logs/v2?accountId=" + accountId + "&taskId=" + taskId)
            .request()
            .get(new GenericType<RestResponse<DelegateSelectionLogResponse>>() {});

    verify(delegateSelectionLogsService, atLeastOnce()).fetchTaskSelectionLogsData(accountId, taskId);
  }
}
