/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.exceptionmappers;

import static io.harness.rule.OwnerRule.KARAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.ng.core.Status;
import io.harness.ng.core.dto.FailureDTO;
import io.harness.rule.Owner;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class NotFoundExceptionMapperTest extends CategoryTest {
  private NotFoundExceptionMapper notFoundExceptionMapper;

  @Before
  public void setup() {
    notFoundExceptionMapper = new NotFoundExceptionMapper();
  }

  @Test
  @Owner(developers = KARAN)
  @Category(UnitTests.class)
  public void testToResponse() {
    Response response = notFoundExceptionMapper.toResponse(new NotFoundException("error"));
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    assertThat(response.getEntity()).isInstanceOf(FailureDTO.class);
    FailureDTO failureDTO = (FailureDTO) response.getEntity();
    assertThat(failureDTO.getStatus()).isEqualTo(Status.FAILURE);
    assertThat(failureDTO.getCode()).isEqualTo(ErrorCode.RESOURCE_NOT_FOUND_EXCEPTION);
  }
}
