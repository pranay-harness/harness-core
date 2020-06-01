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
@javax.annotation.
Generated(value = "by gRPC proto compiler", comments = "Source: io/harness/perpetualtask/perpetual_task_service.proto")
public final class PerpetualTaskServiceGrpc {
  private PerpetualTaskServiceGrpc() {}

  public static final String SERVICE_NAME = "io.harness.perpetualtask.PerpetualTaskService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.harness.perpetualtask.PerpetualTaskListRequest,
      io.harness.perpetualtask.PerpetualTaskListResponse> getPerpetualTaskListMethod;

  @io.grpc.stub.annotations
      .RpcMethod(fullMethodName = SERVICE_NAME + '/' + "PerpetualTaskList",
          requestType = io.harness.perpetualtask.PerpetualTaskListRequest.class,
          responseType = io.harness.perpetualtask.PerpetualTaskListResponse.class,
          methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
      public static io.grpc.MethodDescriptor<io.harness.perpetualtask.PerpetualTaskListRequest,
          io.harness.perpetualtask.PerpetualTaskListResponse>
      getPerpetualTaskListMethod() {
    io.grpc.MethodDescriptor<io.harness.perpetualtask.PerpetualTaskListRequest,
        io.harness.perpetualtask.PerpetualTaskListResponse> getPerpetualTaskListMethod;
    if ((getPerpetualTaskListMethod = PerpetualTaskServiceGrpc.getPerpetualTaskListMethod) == null) {
      synchronized (PerpetualTaskServiceGrpc.class) {
        if ((getPerpetualTaskListMethod = PerpetualTaskServiceGrpc.getPerpetualTaskListMethod) == null) {
          PerpetualTaskServiceGrpc.getPerpetualTaskListMethod = getPerpetualTaskListMethod =
              io.grpc.MethodDescriptor
                  .<io.harness.perpetualtask.PerpetualTaskListRequest,
                      io.harness.perpetualtask.PerpetualTaskListResponse>newBuilder()
                  .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                  .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PerpetualTaskList"))
                  .setSampledToLocalTracing(true)
                  .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.perpetualtask.PerpetualTaskListRequest.getDefaultInstance()))
                  .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.perpetualtask.PerpetualTaskListResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new PerpetualTaskServiceMethodDescriptorSupplier("PerpetualTaskList"))
                  .build();
        }
      }
    }
    return getPerpetualTaskListMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.harness.perpetualtask.PerpetualTaskContextRequest,
      io.harness.perpetualtask.PerpetualTaskContextResponse> getPerpetualTaskContextMethod;

  @io.grpc.stub.annotations
      .RpcMethod(fullMethodName = SERVICE_NAME + '/' + "PerpetualTaskContext",
          requestType = io.harness.perpetualtask.PerpetualTaskContextRequest.class,
          responseType = io.harness.perpetualtask.PerpetualTaskContextResponse.class,
          methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
      public static io.grpc.MethodDescriptor<io.harness.perpetualtask.PerpetualTaskContextRequest,
          io.harness.perpetualtask.PerpetualTaskContextResponse>
      getPerpetualTaskContextMethod() {
    io.grpc.MethodDescriptor<io.harness.perpetualtask.PerpetualTaskContextRequest,
        io.harness.perpetualtask.PerpetualTaskContextResponse> getPerpetualTaskContextMethod;
    if ((getPerpetualTaskContextMethod = PerpetualTaskServiceGrpc.getPerpetualTaskContextMethod) == null) {
      synchronized (PerpetualTaskServiceGrpc.class) {
        if ((getPerpetualTaskContextMethod = PerpetualTaskServiceGrpc.getPerpetualTaskContextMethod) == null) {
          PerpetualTaskServiceGrpc.getPerpetualTaskContextMethod = getPerpetualTaskContextMethod =
              io.grpc.MethodDescriptor
                  .<io.harness.perpetualtask.PerpetualTaskContextRequest,
                      io.harness.perpetualtask.PerpetualTaskContextResponse>newBuilder()
                  .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                  .setFullMethodName(generateFullMethodName(SERVICE_NAME, "PerpetualTaskContext"))
                  .setSampledToLocalTracing(true)
                  .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.perpetualtask.PerpetualTaskContextRequest.getDefaultInstance()))
                  .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.perpetualtask.PerpetualTaskContextResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new PerpetualTaskServiceMethodDescriptorSupplier("PerpetualTaskContext"))
                  .build();
        }
      }
    }
    return getPerpetualTaskContextMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.harness.perpetualtask.HeartbeatRequest,
      io.harness.perpetualtask.HeartbeatResponse> getHeartbeatMethod;

  @io.grpc.stub.annotations
      .RpcMethod(fullMethodName = SERVICE_NAME + '/' + "Heartbeat",
          requestType = io.harness.perpetualtask.HeartbeatRequest.class,
          responseType = io.harness.perpetualtask.HeartbeatResponse.class,
          methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
      public static io.grpc
      .MethodDescriptor<io.harness.perpetualtask.HeartbeatRequest, io.harness.perpetualtask.HeartbeatResponse>
      getHeartbeatMethod() {
    io.grpc.MethodDescriptor<io.harness.perpetualtask.HeartbeatRequest, io.harness.perpetualtask.HeartbeatResponse>
        getHeartbeatMethod;
    if ((getHeartbeatMethod = PerpetualTaskServiceGrpc.getHeartbeatMethod) == null) {
      synchronized (PerpetualTaskServiceGrpc.class) {
        if ((getHeartbeatMethod = PerpetualTaskServiceGrpc.getHeartbeatMethod) == null) {
          PerpetualTaskServiceGrpc.getHeartbeatMethod = getHeartbeatMethod =
              io.grpc.MethodDescriptor
                  .<io.harness.perpetualtask.HeartbeatRequest, io.harness.perpetualtask.HeartbeatResponse>newBuilder()
                  .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                  .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Heartbeat"))
                  .setSampledToLocalTracing(true)
                  .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.perpetualtask.HeartbeatRequest.getDefaultInstance()))
                  .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.perpetualtask.HeartbeatResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new PerpetualTaskServiceMethodDescriptorSupplier("Heartbeat"))
                  .build();
        }
      }
    }
    return getHeartbeatMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static PerpetualTaskServiceStub newStub(io.grpc.Channel channel) {
    return new PerpetualTaskServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static PerpetualTaskServiceBlockingStub newBlockingStub(io.grpc.Channel channel) {
    return new PerpetualTaskServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static PerpetualTaskServiceFutureStub newFutureStub(io.grpc.Channel channel) {
    return new PerpetualTaskServiceFutureStub(channel);
  }

  /**
   */
  public static abstract class PerpetualTaskServiceImplBase implements io.grpc.BindableService {
    /**
     */
    public void perpetualTaskList(io.harness.perpetualtask.PerpetualTaskListRequest request,
        io.grpc.stub.StreamObserver<io.harness.perpetualtask.PerpetualTaskListResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getPerpetualTaskListMethod(), responseObserver);
    }

    /**
     */
    public void perpetualTaskContext(io.harness.perpetualtask.PerpetualTaskContextRequest request,
        io.grpc.stub.StreamObserver<io.harness.perpetualtask.PerpetualTaskContextResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getPerpetualTaskContextMethod(), responseObserver);
    }

    /**
     */
    public void heartbeat(io.harness.perpetualtask.HeartbeatRequest request,
        io.grpc.stub.StreamObserver<io.harness.perpetualtask.HeartbeatResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getHeartbeatMethod(), responseObserver);
    }

    @java.
    lang.Override
    public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(getPerpetualTaskListMethod(),
              asyncUnaryCall(new MethodHandlers<io.harness.perpetualtask.PerpetualTaskListRequest,
                  io.harness.perpetualtask.PerpetualTaskListResponse>(this, METHODID_PERPETUAL_TASK_LIST)))
          .addMethod(getPerpetualTaskContextMethod(),
              asyncUnaryCall(new MethodHandlers<io.harness.perpetualtask.PerpetualTaskContextRequest,
                  io.harness.perpetualtask.PerpetualTaskContextResponse>(this, METHODID_PERPETUAL_TASK_CONTEXT)))
          .addMethod(getHeartbeatMethod(),
              asyncUnaryCall(new MethodHandlers<io.harness.perpetualtask.HeartbeatRequest,
                  io.harness.perpetualtask.HeartbeatResponse>(this, METHODID_HEARTBEAT)))
          .build();
    }
  }

  /**
   */
  public static final class PerpetualTaskServiceStub extends io.grpc.stub.AbstractStub<PerpetualTaskServiceStub> {
    private PerpetualTaskServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PerpetualTaskServiceStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PerpetualTaskServiceStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PerpetualTaskServiceStub(channel, callOptions);
    }

    /**
     */
    public void perpetualTaskList(io.harness.perpetualtask.PerpetualTaskListRequest request,
        io.grpc.stub.StreamObserver<io.harness.perpetualtask.PerpetualTaskListResponse> responseObserver) {
      asyncUnaryCall(getChannel().newCall(getPerpetualTaskListMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void perpetualTaskContext(io.harness.perpetualtask.PerpetualTaskContextRequest request,
        io.grpc.stub.StreamObserver<io.harness.perpetualtask.PerpetualTaskContextResponse> responseObserver) {
      asyncUnaryCall(
          getChannel().newCall(getPerpetualTaskContextMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void heartbeat(io.harness.perpetualtask.HeartbeatRequest request,
        io.grpc.stub.StreamObserver<io.harness.perpetualtask.HeartbeatResponse> responseObserver) {
      asyncUnaryCall(getChannel().newCall(getHeartbeatMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class PerpetualTaskServiceBlockingStub
      extends io.grpc.stub.AbstractStub<PerpetualTaskServiceBlockingStub> {
    private PerpetualTaskServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PerpetualTaskServiceBlockingStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PerpetualTaskServiceBlockingStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PerpetualTaskServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public io.harness.perpetualtask.PerpetualTaskListResponse perpetualTaskList(
        io.harness.perpetualtask.PerpetualTaskListRequest request) {
      return blockingUnaryCall(getChannel(), getPerpetualTaskListMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.harness.perpetualtask.PerpetualTaskContextResponse perpetualTaskContext(
        io.harness.perpetualtask.PerpetualTaskContextRequest request) {
      return blockingUnaryCall(getChannel(), getPerpetualTaskContextMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.harness.perpetualtask.HeartbeatResponse heartbeat(io.harness.perpetualtask.HeartbeatRequest request) {
      return blockingUnaryCall(getChannel(), getHeartbeatMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class PerpetualTaskServiceFutureStub
      extends io.grpc.stub.AbstractStub<PerpetualTaskServiceFutureStub> {
    private PerpetualTaskServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PerpetualTaskServiceFutureStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PerpetualTaskServiceFutureStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PerpetualTaskServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.harness.perpetualtask.PerpetualTaskListResponse>
    perpetualTaskList(io.harness.perpetualtask.PerpetualTaskListRequest request) {
      return futureUnaryCall(getChannel().newCall(getPerpetualTaskListMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.harness.perpetualtask.PerpetualTaskContextResponse>
    perpetualTaskContext(io.harness.perpetualtask.PerpetualTaskContextRequest request) {
      return futureUnaryCall(getChannel().newCall(getPerpetualTaskContextMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.harness.perpetualtask.HeartbeatResponse> heartbeat(
        io.harness.perpetualtask.HeartbeatRequest request) {
      return futureUnaryCall(getChannel().newCall(getHeartbeatMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_PERPETUAL_TASK_LIST = 0;
  private static final int METHODID_PERPETUAL_TASK_CONTEXT = 1;
  private static final int METHODID_HEARTBEAT = 2;

  private static final class MethodHandlers<Req, Resp>
      implements io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
                 io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
                 io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
                 io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final PerpetualTaskServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(PerpetualTaskServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.
    lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_PERPETUAL_TASK_LIST:
          serviceImpl.perpetualTaskList((io.harness.perpetualtask.PerpetualTaskListRequest) request,
              (io.grpc.stub.StreamObserver<io.harness.perpetualtask.PerpetualTaskListResponse>) responseObserver);
          break;
        case METHODID_PERPETUAL_TASK_CONTEXT:
          serviceImpl.perpetualTaskContext((io.harness.perpetualtask.PerpetualTaskContextRequest) request,
              (io.grpc.stub.StreamObserver<io.harness.perpetualtask.PerpetualTaskContextResponse>) responseObserver);
          break;
        case METHODID_HEARTBEAT:
          serviceImpl.heartbeat((io.harness.perpetualtask.HeartbeatRequest) request,
              (io.grpc.stub.StreamObserver<io.harness.perpetualtask.HeartbeatResponse>) responseObserver);
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

  private static abstract class PerpetualTaskServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    PerpetualTaskServiceBaseDescriptorSupplier() {}

    @java.
    lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.harness.perpetualtask.PerpetualTaskServiceOuterClass.getDescriptor();
    }

    @java.
    lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("PerpetualTaskService");
    }
  }

  private static final class PerpetualTaskServiceFileDescriptorSupplier
      extends PerpetualTaskServiceBaseDescriptorSupplier {
    PerpetualTaskServiceFileDescriptorSupplier() {}
  }

  private static final class PerpetualTaskServiceMethodDescriptorSupplier
      extends PerpetualTaskServiceBaseDescriptorSupplier implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    PerpetualTaskServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (PerpetualTaskServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
                                           .setSchemaDescriptor(new PerpetualTaskServiceFileDescriptorSupplier())
                                           .addMethod(getPerpetualTaskListMethod())
                                           .addMethod(getPerpetualTaskContextMethod())
                                           .addMethod(getHeartbeatMethod())
                                           .build();
        }
      }
    }
    return result;
  }
}
