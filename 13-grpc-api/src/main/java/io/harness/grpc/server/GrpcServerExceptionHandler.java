package io.harness.grpc.server;

import com.google.inject.Provider;

import io.grpc.ForwardingServerCall;
import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;
import io.grpc.Status;
import io.harness.exception.ExceptionUtils;
import io.harness.grpc.exception.GrpcExceptionMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.validation.constraints.NotNull;

@Slf4j
public class GrpcServerExceptionHandler implements ServerInterceptor {
  private final Map<Class, GrpcExceptionMapper> grpcExceptionMapperMap;

  public GrpcServerExceptionHandler(@NotNull Provider<Set<GrpcExceptionMapper>> grpcExceptionMappersProvider) {
    grpcExceptionMapperMap = new HashMap<>();
    Set<GrpcExceptionMapper> grpcExceptionMappers = grpcExceptionMappersProvider.get();
    if (grpcExceptionMappers != null) {
      grpcExceptionMappers.forEach(m -> { grpcExceptionMapperMap.put(m.getClazz(), m); });
    }
  }

  @Override
  public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
      ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
    ServerCall<ReqT, RespT> wrappedCall = new ForwardingServerCall.SimpleForwardingServerCall<ReqT, RespT>(call) {
      @Override
      public void close(Status status, Metadata trailers) {
        if (status.getCode() == Status.Code.UNKNOWN) {
          Throwable throwable = status.getCause();

          GrpcExceptionMapper exceptionMapper = grpcExceptionMapperMap.get(throwable.getClass());
          if (exceptionMapper != null) {
            status = exceptionMapper.toStatus(throwable);
          } else {
            logger.error("Exception occurred: " + ExceptionUtils.getMessage(throwable), throwable);
            status = Status.INTERNAL.withDescription(throwable.getMessage()).withCause(throwable);
          }
        }
        super.close(status, trailers);
      }
    };

    return next.startCall(wrappedCall, headers);
  }
}
