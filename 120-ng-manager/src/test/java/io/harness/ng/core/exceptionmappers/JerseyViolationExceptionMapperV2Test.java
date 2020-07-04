package io.harness.ng.core.exceptionmappers;

import static io.harness.rule.OwnerRule.PHOENIKX;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.dropwizard.jersey.validation.JerseyViolationException;
import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.ng.core.BaseTest;
import io.harness.ng.core.FailureDTO;
import io.harness.ng.core.Status;
import io.harness.rule.Owner;
import org.glassfish.jersey.server.model.Invocable;
import org.glassfish.jersey.server.model.ResourceMethodInvoker;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.HashSet;
import javax.ws.rs.core.Response;

public class JerseyViolationExceptionMapperV2Test extends BaseTest {
  @Inject private JerseyViolationExceptionMapperV2 jerseyViolationExceptionMapperV2;

  @Test
  @Owner(developers = PHOENIKX)
  @Category(UnitTests.class)
  public void testToResponse() {
    Invocable invocable = Invocable.create(ResourceMethodInvoker.class);
    JerseyViolationException jerseyViolationException = new JerseyViolationException(new HashSet<>(), invocable);
    Response response = jerseyViolationExceptionMapperV2.toResponse(jerseyViolationException);
    assertThat(response.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
    assertThat(response.getEntity()).isInstanceOf(FailureDTO.class);
    FailureDTO failureDTO = (FailureDTO) response.getEntity();
    assertThat(failureDTO.getStatus()).isEqualTo(Status.FAILURE);
    assertThat(failureDTO.getCode()).isEqualTo(ErrorCode.INVALID_REQUEST);
  }
}
