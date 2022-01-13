/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.ng.core.exceptionmappers;

import static io.harness.rule.OwnerRule.PHOENIKX;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnauthorizedException;
import io.harness.exception.WingsException;
import io.harness.exception.ngexception.beans.SampleErrorMetadataDTO;
import io.harness.ng.core.Status;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.rule.Owner;

import com.google.common.collect.ImmutableMap;
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
    assertThat(errorDTO.getMetadata()).isEqualTo(null);
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testInvalidRequestExceptionWithExtraMetadata() {
    InvalidRequestException invalidRequestException = new InvalidRequestException(
        "Invalid request", SampleErrorMetadataDTO.builder().sampleMap(ImmutableMap.of("a", "b")).build());
    Response response = wingsExceptionMapperV2.toResponse(invalidRequestException);
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    assertThat(response.getEntity()).isInstanceOf(ErrorDTO.class);
    ErrorDTO errorDTO = (ErrorDTO) response.getEntity();
    assertThat(errorDTO.getStatus()).isEqualTo(Status.ERROR);
    assertThat(errorDTO.getMetadata()).isInstanceOf(SampleErrorMetadataDTO.class);
    assertThat(errorDTO.getMetadata().getType()).isEqualTo("Sample");
    SampleErrorMetadataDTO sampleErrorMetadataDTO = (SampleErrorMetadataDTO) errorDTO.getMetadata();
    assertThat(sampleErrorMetadataDTO.getSampleMap()).hasSize(1);
  }
}
