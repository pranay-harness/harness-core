/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cvng.exception;

import static io.harness.rule.OwnerRule.NEMANJA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.eraro.ResponseMessage;
import io.harness.rule.Owner;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class BadRequestExceptionMapperTest extends CategoryTest {
  private BadRequestExceptionMapper badRequestExceptionMapper;

  @Before
  public void setup() {
    badRequestExceptionMapper = new BadRequestExceptionMapper();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testToResponse() {
    BadRequestException exception = new BadRequestException();
    Response response = badRequestExceptionMapper.toResponse(exception);
    assertThat(response.getEntity()).isInstanceOf(ResponseMessage.class);
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }
}
