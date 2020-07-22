package io.harness.ng.core.exceptionmappers;

import static io.harness.rule.OwnerRule.PHOENIKX;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.ng.core.Status;
import io.harness.ng.core.dto.ErrorDTO;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.ws.rs.core.Response;

public class GenericExceptionMapperV2Test {
  private GenericExceptionMapperV2 genericExceptionMapperV2;

  @Before
  public void setup() {
    genericExceptionMapperV2 = new GenericExceptionMapperV2();
  }

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testToResponse() {
    NullPointerException nullPointerException = new NullPointerException("Null");
    Response response = genericExceptionMapperV2.toResponse(nullPointerException);
    assertThat(response.getStatus()).isEqualTo(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode());
    assertThat(response.getEntity()).isInstanceOf(ErrorDTO.class);
    ErrorDTO errorDTO = (ErrorDTO) response.getEntity();
    assertThat(errorDTO.getCode()).isEqualTo(ErrorCode.UNKNOWN_ERROR);
    assertThat(errorDTO.getStatus()).isEqualTo(Status.ERROR);
  }
}
