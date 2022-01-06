/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.exceptionmappers;

import static io.harness.rule.OwnerRule.ARCHIT;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.ng.core.Status;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.rule.Owner;

import javax.ws.rs.NotAllowedException;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class NotAllowedExceptionMapperTest extends CategoryTest {
  private NotAllowedExceptionMapper notAllowedExceptionMapper;

  @Before
  public void setup() {
    notAllowedExceptionMapper = new NotAllowedExceptionMapper();
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testToResponse() {
    Response response = notAllowedExceptionMapper.toResponse(new NotAllowedException("error"));
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    assertThat(response.getEntity()).isInstanceOf(FailureDTO.class);
    FailureDTO failureDTO = (FailureDTO) response.getEntity();
    assertThat(failureDTO.getStatus()).isEqualTo(Status.FAILURE);
    assertThat(failureDTO.getMessage()).isEqualTo("HTTP 405 Method Not Allowed");
    assertThat(failureDTO.getCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND_EXCEPTION);
  }
}
