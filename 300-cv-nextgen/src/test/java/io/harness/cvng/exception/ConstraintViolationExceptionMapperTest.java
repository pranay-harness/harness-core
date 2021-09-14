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
import io.harness.rule.Owner;

import java.util.HashSet;
import java.util.List;
import javax.validation.ConstraintViolationException;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ConstraintViolationExceptionMapperTest extends CategoryTest {
  private ConstraintViolationExceptionMapper constraintViolationExceptionMapper;

  @Before
  public void setup() {
    constraintViolationExceptionMapper = new ConstraintViolationExceptionMapper();
  }

  @Test
  @Owner(developers = NEMANJA)
  @Category(UnitTests.class)
  public void testToResponse() {
    ConstraintViolationException constraintViolationException = new ConstraintViolationException(new HashSet<>());
    Response response = constraintViolationExceptionMapper.toResponse(constraintViolationException);
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    assertThat(response.getEntity()).isInstanceOf(List.class);
  }
}
