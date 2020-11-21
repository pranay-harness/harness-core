package io.harness.ng.core.exceptionmappers;

import static io.harness.rule.OwnerRule.KARAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.CategoryTest;
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
