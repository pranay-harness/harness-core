/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ng.core.exceptionmappers;

import static io.harness.rule.OwnerRule.PHOENIKX;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.exception.UnauthorizedException;
import io.harness.exception.WingsException;
import io.harness.ng.core.Status;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.rule.Owner;

import com.google.common.collect.Sets;
import java.util.EnumSet;
import java.util.Set;
import javax.ws.rs.core.Response;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class WingsExceptionMapperV2Test extends CategoryTest {
  private WingsExceptionMapperV2 wingsExceptionMapperV2;

  @Before
  public void setup() {
    wingsExceptionMapperV2 = new WingsExceptionMapperV2();
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testToResponse() {
    Set<WingsException.ReportTarget> reportTargetSet = Sets.newHashSet(WingsException.ReportTarget.REST_API);
    EnumSet<WingsException.ReportTarget> enumSet = Sets.newEnumSet(reportTargetSet, WingsException.ReportTarget.class);
    UnauthorizedException unauthorizedException = new UnauthorizedException("Unauthorized", enumSet);
    Response response = wingsExceptionMapperV2.toResponse(unauthorizedException);
    assertThat(response.getStatus()).isEqualTo(Response.Status.UNAUTHORIZED.getStatusCode());
    assertThat(response.getEntity()).isInstanceOf(ErrorDTO.class);
    ErrorDTO errorDTO = (ErrorDTO) response.getEntity();
    assertThat(errorDTO.getStatus()).isEqualTo(Status.ERROR);
    assertThat(errorDTO.getCode()).isEqualTo(ErrorCode.INVALID_TOKEN);
  }
}
