package io.harness.ng.core.exceptionmappers;

import static io.harness.rule.OwnerRule.PHOENIKX;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.eraro.ErrorCode;
import io.harness.exception.UnauthorizedException;
import io.harness.exception.WingsException;
import io.harness.ng.core.BaseTest;
import io.harness.ng.core.ErrorDTO;
import io.harness.ng.core.Status;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.EnumSet;
import java.util.Set;
import javax.ws.rs.core.Response;

public class WingsExceptionMapperV2Test extends BaseTest {
  @Inject private WingsExceptionMapperV2 wingsExceptionMapperV2;
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
