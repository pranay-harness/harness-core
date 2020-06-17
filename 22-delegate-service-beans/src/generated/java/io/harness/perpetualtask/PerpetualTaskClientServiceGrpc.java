package io.harness.perpetualtask;

import static io.grpc.MethodDescriptor.generateFullMethodName;
import static io.grpc.stub.ClientCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ClientCalls.asyncClientStreamingCall;
import static io.grpc.stub.ClientCalls.asyncServerStreamingCall;
import static io.grpc.stub.ClientCalls.asyncUnaryCall;
import static io.grpc.stub.ClientCalls.blockingServerStreamingCall;
import static io.grpc.stub.ClientCalls.blockingUnaryCall;
import static io.grpc.stub.ClientCalls.futureUnaryCall;
import static io.grpc.stub.ServerCalls.asyncBidiStreamingCall;
import static io.grpc.stub.ServerCalls.asyncClientStreamingCall;
import static io.grpc.stub.ServerCalls.asyncServerStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnaryCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall;
import static io.grpc.stub.ServerCalls.asyncUnimplementedUnaryCall;

/**
 */
@javax.annotation.Generated(
    value = "by gRPC proto compiler", comments = "Source: io/harness/perpetualtask/perpetual_task_client_service.proto")
public final class PerpetualTaskClientServiceGrpc {
  private PerpetualTaskClientServiceGrpc() {}

  public static final String SERVICE_NAME = "io.harness.perpetualtask.PerpetualTaskClientService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.harness.perpetualtask.ObtainTaskCapabilitiesRequest,
      io.harness.perpetualtask.ObtainTaskCapabilitiesResponse> getObtainTaskCapabilitiesMethod;

  @io.grpc.stub.annotations
      .RpcMethod(fullMethodName = SERVICE_NAME + '/' + "ObtainTaskCapabilities",
          requestType = io.harness.perpetualtask.ObtainTaskCapabilitiesRequest.class,
          responseType = io.harness.perpetualtask.ObtainTaskCapabilitiesResponse.class,
          methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
      public static io.grpc.MethodDescriptor<io.harness.perpetualtask.ObtainTaskCapabilitiesRequest,
          io.harness.perpetualtask.ObtainTaskCapabilitiesResponse>
      getObtainTaskCapabilitiesMethod() {
    io.grpc.MethodDescriptor<io.harness.perpetualtask.ObtainTaskCapabilitiesRequest,
        io.harness.perpetualtask.ObtainTaskCapabilitiesResponse> getObtainTaskCapabilitiesMethod;
    if ((getObtainTaskCapabilitiesMethod = PerpetualTaskClientServiceGrpc.getObtainTaskCapabilitiesMethod) == null) {
      synchronized (PerpetualTaskClientServiceGrpc.class) {
        if ((getObtainTaskCapabilitiesMethod = PerpetualTaskClientServiceGrpc.getObtainTaskCapabilitiesMethod)
            == null) {
          PerpetualTaskClientServiceGrpc.getObtainTaskCapabilitiesMethod = getObtainTaskCapabilitiesMethod =
              io.grpc.MethodDescriptor
                  .<io.harness.perpetualtask.ObtainTaskCapabilitiesRequest,
                      io.harness.perpetualtask.ObtainTaskCapabilitiesResponse>newBuilder()
                  .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                  .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ObtainTaskCapabilities"))
                  .setSampledToLocalTracing(true)
                  .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.perpetualtask.ObtainTaskCapabilitiesRequest.getDefaultInstance()))
                  .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.perpetualtask.ObtainTaskCapabilitiesResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new PerpetualTaskClientServiceMethodDescriptorSupplier("ObtainTaskCapabilities"))
                  .build();
        }
      }
    }
    return getObtainTaskCapabilitiesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.harness.perpetualtask.ObtainTaskExecutionParamsRequest,
      io.harness.perpetualtask.ObtainTaskExecutionParamsResponse> getObtainTaskExecutionParamsMethod;

  @io.grpc.stub.annotations
      .RpcMethod(fullMethodName = SERVICE_NAME + '/' + "ObtainTaskExecutionParams",
          requestType = io.harness.perpetualtask.ObtainTaskExecutionParamsRequest.class,
          responseType = io.harness.perpetualtask.ObtainTaskExecutionParamsResponse.class,
          methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
      public static io.grpc.MethodDescriptor<io.harness.perpetualtask.ObtainTaskExecutionParamsRequest,
          io.harness.perpetualtask.ObtainTaskExecutionParamsResponse>
      getObtainTaskExecutionParamsMethod() {
    io.grpc.MethodDescriptor<io.harness.perpetualtask.ObtainTaskExecutionParamsRequest,
        io.harness.perpetualtask.ObtainTaskExecutionParamsResponse> getObtainTaskExecutionParamsMethod;
    if ((getObtainTaskExecutionParamsMethod = PerpetualTaskClientServiceGrpc.getObtainTaskExecutionParamsMethod)
        == null) {
      synchronized (PerpetualTaskClientServiceGrpc.class) {
        if ((getObtainTaskExecutionParamsMethod = PerpetualTaskClientServiceGrpc.getObtainTaskExecutionParamsMethod)
            == null) {
          PerpetualTaskClientServiceGrpc.getObtainTaskExecutionParamsMethod = getObtainTaskExecutionParamsMethod =
              io.grpc.MethodDescriptor
                  .<io.harness.perpetualtask.ObtainTaskExecutionParamsRequest,
                      io.harness.perpetualtask.ObtainTaskExecutionParamsResponse>newBuilder()
                  .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                  .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ObtainTaskExecutionParams"))
                  .setSampledToLocalTracing(true)
                  .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.perpetualtask.ObtainTaskExecutionParamsRequest.getDefaultInstance()))
                  .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.perpetualtask.ObtainTaskExecutionParamsResponse.getDefaultInstance()))
                  .setSchemaDescriptor(
                      new PerpetualTaskClientServiceMethodDescriptorSupplier("ObtainTaskExecutionParams"))
                  .build();
        }
      }
    }
    return getObtainTaskExecutionParamsMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static PerpetualTaskClientServiceStub newStub(io.grpc.Channel channel) {
    return new PerpetualTaskClientServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static PerpetualTaskClientServiceBlockingStub newBlockingStub(io.grpc.Channel channel) {
    return new PerpetualTaskClientServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static PerpetualTaskClientServiceFutureStub newFutureStub(io.grpc.Channel channel) {
    return new PerpetualTaskClientServiceFutureStub(channel);
  }

  /**
   */
  public static abstract class PerpetualTaskClientServiceImplBase implements io.grpc.BindableService {
    /**
     */
    public void obtainTaskCapabilities(io.harness.perpetualtask.ObtainTaskCapabilitiesRequest request,
        io.grpc.stub.StreamObserver<io.harness.perpetualtask.ObtainTaskCapabilitiesResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getObtainTaskCapabilitiesMethod(), responseObserver);
    }

    /**
     */
    public void obtainTaskExecutionParams(io.harness.perpetualtask.ObtainTaskExecutionParamsRequest request,
        io.grpc.stub.StreamObserver<io.harness.perpetualtask.ObtainTaskExecutionParamsResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getObtainTaskExecutionParamsMethod(), responseObserver);
    }

    @java.
    lang.Override
    public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(getObtainTaskCapabilitiesMethod(),
              asyncUnaryCall(new MethodHandlers<io.harness.perpetualtask.ObtainTaskCapabilitiesRequest,
                  io.harness.perpetualtask.ObtainTaskCapabilitiesResponse>(this, METHODID_OBTAIN_TASK_CAPABILITIES)))
          .addMethod(getObtainTaskExecutionParamsMethod(),
              asyncUnaryCall(new MethodHandlers<io.harness.perpetualtask.ObtainTaskExecutionParamsRequest,
                  io.harness.perpetualtask.ObtainTaskExecutionParamsResponse>(
                  this, METHODID_OBTAIN_TASK_EXECUTION_PARAMS)))
          .build();
    }
  }

  /**
   */
  public static final class PerpetualTaskClientServiceStub
      extends io.grpc.stub.AbstractStub<PerpetualTaskClientServiceStub> {
    private PerpetualTaskClientServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PerpetualTaskClientServiceStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PerpetualTaskClientServiceStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PerpetualTaskClientServiceStub(channel, callOptions);
    }

    /**
     */
    public void obtainTaskCapabilities(io.harness.perpetualtask.ObtainTaskCapabilitiesRequest request,
        io.grpc.stub.StreamObserver<io.harness.perpetualtask.ObtainTaskCapabilitiesResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getObtainTaskCapabilitiesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void obtainTaskExecutionParams(io.harness.perpetualtask.ObtainTaskExecutionParamsRequest request,
        io.grpc.stub.StreamObserver<io.harness.perpetualtask.ObtainTaskExecutionParamsResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getObtainTaskExecutionParamsMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class PerpetualTaskClientServiceBlockingStub
      extends io.grpc.stub.AbstractStub<PerpetualTaskClientServiceBlockingStub> {
    private PerpetualTaskClientServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PerpetualTaskClientServiceBlockingStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PerpetualTaskClientServiceBlockingStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PerpetualTaskClientServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public io.harness.perpetualtask.ObtainTaskCapabilitiesResponse obtainTaskCapabilities(
        io.harness.perpetualtask.ObtainTaskCapabilitiesRequest request) {
      return blockingUnaryCall(getChannel(), getObtainTaskCapabilitiesMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.harness.perpetualtask.ObtainTaskExecutionParamsResponse obtainTaskExecutionParams(
        io.harness.perpetualtask.ObtainTaskExecutionParamsRequest request) {
      return blockingUnaryCall(getChannel(), getObtainTaskExecutionParamsMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class PerpetualTaskClientServiceFutureStub
      extends io.grpc.stub.AbstractStub<PerpetualTaskClientServiceFutureStub> {
    private PerpetualTaskClientServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PerpetualTaskClientServiceFutureStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PerpetualTaskClientServiceFutureStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PerpetualTaskClientServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.harness.perpetualtask.ObtainTaskCapabilitiesResponse>
    obtainTaskCapabilities(io.harness.perpetualtask.ObtainTaskCapabilitiesRequest request) {
      return futureUnaryCall(getChannel().newCall(getObtainTaskCapabilitiesMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent
        .ListenableFuture<io.harness.perpetualtask.ObtainTaskExecutionParamsResponse>
        obtainTaskExecutionParams(io.harness.perpetualtask.ObtainTaskExecutionParamsRequest request) {
      return futureUnaryCall(getChannel().newCall(getObtainTaskExecutionParamsMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_OBTAIN_TASK_CAPABILITIES = 0;
  private static final int METHODID_OBTAIN_TASK_EXECUTION_PARAMS = 1;

  private static final class MethodHandlers<Req, Resp>
      implements io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
                 io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
                 io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
                 io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final PerpetualTaskClientServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(PerpetualTaskClientServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.
    lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_OBTAIN_TASK_CAPABILITIES:
          serviceImpl.obtainTaskCapabilities((io.harness.perpetualtask.ObtainTaskCapabilitiesRequest) request,
              (io.grpc.stub.StreamObserver<io.harness.perpetualtask.ObtainTaskCapabilitiesResponse>) responseObserver);
          break;
        case METHODID_OBTAIN_TASK_EXECUTION_PARAMS:
          serviceImpl.obtainTaskExecutionParams((io.harness.perpetualtask.ObtainTaskExecutionParamsRequest) request,
              (io.grpc.stub.StreamObserver<io.harness.perpetualtask.ObtainTaskExecutionParamsResponse>)
                  responseObserver);
          break;
        default:
          throw new AssertionError();
      }
    }

    @java.
    lang.Override
    @java.
    lang.SuppressWarnings("unchecked")
    public io.grpc.stub.StreamObserver<Req> invoke(io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        default:
          throw new AssertionError();
      }
    }
  }

  private static abstract class PerpetualTaskClientServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    PerpetualTaskClientServiceBaseDescriptorSupplier() {}

    @java.
    lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.harness.perpetualtask.PerpetualTaskClientServiceOuterClass.getDescriptor();
    }

    @java.
    lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("PerpetualTaskClientService");
    }
  }

  private static final class PerpetualTaskClientServiceFileDescriptorSupplier
      extends PerpetualTaskClientServiceBaseDescriptorSupplier {
    PerpetualTaskClientServiceFileDescriptorSupplier() {}
  }

  private static final class PerpetualTaskClientServiceMethodDescriptorSupplier
      extends PerpetualTaskClientServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    PerpetualTaskClientServiceMethodDescriptorSupplier(String methodName) {
      this.methodName = methodName;
    }

    @java.
    lang.Override
    public com.google.protobuf.Descriptors.MethodDescriptor getMethodDescriptor() {
      return getServiceDescriptor().findMethodByName(methodName);
    }
  }

  private static volatile io.grpc.ServiceDescriptor serviceDescriptor;

  public static io.grpc.ServiceDescriptor getServiceDescriptor() {
    io.grpc.ServiceDescriptor result = serviceDescriptor;
    if (result == null) {
      synchronized (PerpetualTaskClientServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
                                           .setSchemaDescriptor(new PerpetualTaskClientServiceFileDescriptorSupplier())
                                           .addMethod(getObtainTaskCapabilitiesMethod())
                                           .addMethod(getObtainTaskExecutionParamsMethod())
                                           .build();
        }
      }
    }
    return result;
  }
}
