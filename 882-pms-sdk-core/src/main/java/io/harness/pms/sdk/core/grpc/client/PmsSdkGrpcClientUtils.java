package io.harness.pms.sdk.core.grpc.client;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.data.structure.EmptyPredicate;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.GeneralException;
import io.harness.exception.WingsException;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(HarnessTeam.PIPELINE)
@UtilityClass
@Slf4j
public class PmsSdkGrpcClientUtils {
  public WingsException processException(Exception ex) {
    if (ex instanceof WingsException) {
      return (WingsException) ex;
    } else if (ex instanceof StatusRuntimeException) {
      return processStatusRuntimeException((StatusRuntimeException) ex);
    } else {
      return new GeneralException(
          ex == null ? "Unknown error while communicating with pipeline service" : ExceptionUtils.getMessage(ex));
    }
  }

  private WingsException processStatusRuntimeException(StatusRuntimeException ex) {
    if (ex.getStatus().getCode() == Status.Code.INTERNAL) {
      return new GeneralException(EmptyPredicate.isEmpty(ex.getStatus().getDescription())
              ? "Unknown grpc error while communicating with pipeline service"
              : ex.getStatus().getDescription());
    } else if (ex.getStatus().getCode() == Status.Code.DEADLINE_EXCEEDED) {
      return new GeneralException("Request to pipeline service timed out");
    }

    log.error("Error connecting to pipeline service. Is it running?", ex);
    return new GeneralException("Error connecting to pipeline service");
  }
}
