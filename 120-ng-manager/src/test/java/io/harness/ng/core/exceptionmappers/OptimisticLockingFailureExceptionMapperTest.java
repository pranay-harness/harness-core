/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.exceptionmappers;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.rule.OwnerRule.KARAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.ng.core.Status;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.rule.Owner;

import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.springframework.dao.OptimisticLockingFailureException;

@OwnedBy(PL)
public class OptimisticLockingFailureExceptionMapperTest extends CategoryTest {
  private OptimisticLockingFailureExceptionMapper optimisticLockingFailureExceptionMapper;

  @Before
  public void setup() {
    optimisticLockingFailureExceptionMapper = new OptimisticLockingFailureExceptionMapper();
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testToResponse() {
    Response response =
        optimisticLockingFailureExceptionMapper.toResponse(new OptimisticLockingFailureException("error"));
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    assertThat(response.getEntity()).isInstanceOf(FailureDTO.class);
    FailureDTO failureDTO = (FailureDTO) response.getEntity();
    assertThat(failureDTO.getStatus()).isEqualTo(Status.FAILURE);
    assertThat(failureDTO.getCode()).isEqualTo(ErrorCode.OPTIMISTIC_LOCKING_EXCEPTION);
  }
}
