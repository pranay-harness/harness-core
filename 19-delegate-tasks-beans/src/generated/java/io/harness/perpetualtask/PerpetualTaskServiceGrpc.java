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
    value = "by gRPC proto compiler (version 1.21.0)", comments = "Source: perpetualtask/perpetual_task_service.proto")
public final class PerpetualTaskServiceGrpc {
  private PerpetualTaskServiceGrpc() {}

  public static final String SERVICE_NAME = "io.harness.perpetualtask.PerpetualTaskService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.harness.perpetualtask.DelegateId,
      io.harness.perpetualtask.PerpetualTaskIdList> getListTaskIdsMethod;

  @io.grpc.stub.annotations
      .RpcMethod(fullMethodName = SERVICE_NAME + '/' + "ListTaskIds",
          requestType = io.harness.perpetualtask.DelegateId.class,
          responseType = io.harness.perpetualtask.PerpetualTaskIdList.class,
          methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
      public static io.grpc
      .MethodDescriptor<io.harness.perpetualtask.DelegateId, io.harness.perpetualtask.PerpetualTaskIdList>
      getListTaskIdsMethod() {
    io.grpc.MethodDescriptor<io.harness.perpetualtask.DelegateId, io.harness.perpetualtask.PerpetualTaskIdList>
        getListTaskIdsMethod;
    if ((getListTaskIdsMethod = PerpetualTaskServiceGrpc.getListTaskIdsMethod) == null) {
      synchronized (PerpetualTaskServiceGrpc.class) {
        if ((getListTaskIdsMethod = PerpetualTaskServiceGrpc.getListTaskIdsMethod) == null) {
          PerpetualTaskServiceGrpc.getListTaskIdsMethod = getListTaskIdsMethod =
              io.grpc.MethodDescriptor
                  .<io.harness.perpetualtask.DelegateId, io.harness.perpetualtask.PerpetualTaskIdList>newBuilder()
                  .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                  .setFullMethodName(
                      generateFullMethodName("io.harness.perpetualtask.PerpetualTaskService", "ListTaskIds"))
                  .setSampledToLocalTracing(true)
                  .setRequestMarshaller(
                      io.grpc.protobuf.ProtoUtils.marshaller(io.harness.perpetualtask.DelegateId.getDefaultInstance()))
                  .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.perpetualtask.PerpetualTaskIdList.getDefaultInstance()))
                  .setSchemaDescriptor(new PerpetualTaskServiceMethodDescriptorSupplier("ListTaskIds"))
                  .build();
        }
      }
    }
    return getListTaskIdsMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.harness.perpetualtask.PerpetualTaskId,
      io.harness.perpetualtask.PerpetualTaskContext> getGetTaskContextMethod;

  @io.grpc.stub.annotations
      .RpcMethod(fullMethodName = SERVICE_NAME + '/' + "GetTaskContext",
          requestType = io.harness.perpetualtask.PerpetualTaskId.class,
          responseType = io.harness.perpetualtask.PerpetualTaskContext.class,
          methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
      public static io.grpc
      .MethodDescriptor<io.harness.perpetualtask.PerpetualTaskId, io.harness.perpetualtask.PerpetualTaskContext>
      getGetTaskContextMethod() {
    io.grpc.MethodDescriptor<io.harness.perpetualtask.PerpetualTaskId, io.harness.perpetualtask.PerpetualTaskContext>
        getGetTaskContextMethod;
    if ((getGetTaskContextMethod = PerpetualTaskServiceGrpc.getGetTaskContextMethod) == null) {
      synchronized (PerpetualTaskServiceGrpc.class) {
        if ((getGetTaskContextMethod = PerpetualTaskServiceGrpc.getGetTaskContextMethod) == null) {
          PerpetualTaskServiceGrpc.getGetTaskContextMethod = getGetTaskContextMethod =
              io.grpc.MethodDescriptor
                  .<io.harness.perpetualtask.PerpetualTaskId, io.harness.perpetualtask.PerpetualTaskContext>newBuilder()
                  .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                  .setFullMethodName(
                      generateFullMethodName("io.harness.perpetualtask.PerpetualTaskService", "GetTaskContext"))
                  .setSampledToLocalTracing(true)
                  .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.perpetualtask.PerpetualTaskId.getDefaultInstance()))
                  .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.perpetualtask.PerpetualTaskContext.getDefaultInstance()))
                  .setSchemaDescriptor(new PerpetualTaskServiceMethodDescriptorSupplier("GetTaskContext"))
                  .build();
        }
      }
    }
    return getGetTaskContextMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.harness.perpetualtask.HeartbeatRequest,
      io.harness.perpetualtask.HeartbeatResponse> getPublishHeartbeatMethod;

  @io.grpc.stub.annotations
      .RpcMethod(fullMethodName = SERVICE_NAME + '/' + "PublishHeartbeat",
          requestType = io.harness.perpetualtask.HeartbeatRequest.class,
          responseType = io.harness.perpetualtask.HeartbeatResponse.class,
          methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
      public static io.grpc
      .MethodDescriptor<io.harness.perpetualtask.HeartbeatRequest, io.harness.perpetualtask.HeartbeatResponse>
      getPublishHeartbeatMethod() {
    io.grpc.MethodDescriptor<io.harness.perpetualtask.HeartbeatRequest, io.harness.perpetualtask.HeartbeatResponse>
        getPublishHeartbeatMethod;
    if ((getPublishHeartbeatMethod = PerpetualTaskServiceGrpc.getPublishHeartbeatMethod) == null) {
      synchronized (PerpetualTaskServiceGrpc.class) {
        if ((getPublishHeartbeatMethod = PerpetualTaskServiceGrpc.getPublishHeartbeatMethod) == null) {
          PerpetualTaskServiceGrpc.getPublishHeartbeatMethod = getPublishHeartbeatMethod =
              io.grpc.MethodDescriptor
                  .<io.harness.perpetualtask.HeartbeatRequest, io.harness.perpetualtask.HeartbeatResponse>newBuilder()
                  .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                  .setFullMethodName(
                      generateFullMethodName("io.harness.perpetualtask.PerpetualTaskService", "PublishHeartbeat"))
                  .setSampledToLocalTracing(true)
                  .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.perpetualtask.HeartbeatRequest.getDefaultInstance()))
                  .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.perpetualtask.HeartbeatResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new PerpetualTaskServiceMethodDescriptorSupplier("PublishHeartbeat"))
                  .build();
        }
      }
    }
    return getPublishHeartbeatMethod;
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
    public void listTaskIds(io.harness.perpetualtask.DelegateId request,
        io.grpc.stub.StreamObserver<io.harness.perpetualtask.PerpetualTaskIdList> responseObserver) {
      asyncUnimplementedUnaryCall(getListTaskIdsMethod(), responseObserver);
    }

    /**
     */
    public void getTaskContext(io.harness.perpetualtask.PerpetualTaskId request,
        io.grpc.stub.StreamObserver<io.harness.perpetualtask.PerpetualTaskContext> responseObserver) {
      asyncUnimplementedUnaryCall(getGetTaskContextMethod(), responseObserver);
    }

    /**
     */
    public void publishHeartbeat(io.harness.perpetualtask.HeartbeatRequest request,
        io.grpc.stub.StreamObserver<io.harness.perpetualtask.HeartbeatResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getPublishHeartbeatMethod(), responseObserver);
    }

    @java.
    lang.Override
    public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(getListTaskIdsMethod(),
              asyncUnaryCall(
                  new MethodHandlers<io.harness.perpetualtask.DelegateId, io.harness.perpetualtask.PerpetualTaskIdList>(
                      this, METHODID_LIST_TASK_IDS)))
          .addMethod(getGetTaskContextMethod(),
              asyncUnaryCall(new MethodHandlers<io.harness.perpetualtask.PerpetualTaskId,
                  io.harness.perpetualtask.PerpetualTaskContext>(this, METHODID_GET_TASK_CONTEXT)))
          .addMethod(getPublishHeartbeatMethod(),
              asyncUnaryCall(new MethodHandlers<io.harness.perpetualtask.HeartbeatRequest,
                  io.harness.perpetualtask.HeartbeatResponse>(this, METHODID_PUBLISH_HEARTBEAT)))
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
    public void listTaskIds(io.harness.perpetualtask.DelegateId request,
        io.grpc.stub.StreamObserver<io.harness.perpetualtask.PerpetualTaskIdList> responseObserver) {
      asyncUnaryCall(getChannel().newCall(getListTaskIdsMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void getTaskContext(io.harness.perpetualtask.PerpetualTaskId request,
        io.grpc.stub.StreamObserver<io.harness.perpetualtask.PerpetualTaskContext> responseObserver) {
      asyncUnaryCall(getChannel().newCall(getGetTaskContextMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void publishHeartbeat(io.harness.perpetualtask.HeartbeatRequest request,
        io.grpc.stub.StreamObserver<io.harness.perpetualtask.HeartbeatResponse> responseObserver) {
      asyncUnaryCall(getChannel().newCall(getPublishHeartbeatMethod(), getCallOptions()), request, responseObserver);
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
    public io.harness.perpetualtask.PerpetualTaskIdList listTaskIds(io.harness.perpetualtask.DelegateId request) {
      return blockingUnaryCall(getChannel(), getListTaskIdsMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.harness.perpetualtask.PerpetualTaskContext getTaskContext(
        io.harness.perpetualtask.PerpetualTaskId request) {
      return blockingUnaryCall(getChannel(), getGetTaskContextMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.harness.perpetualtask.HeartbeatResponse publishHeartbeat(
        io.harness.perpetualtask.HeartbeatRequest request) {
      return blockingUnaryCall(getChannel(), getPublishHeartbeatMethod(), getCallOptions(), request);
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
    public com.google.common.util.concurrent.ListenableFuture<io.harness.perpetualtask.PerpetualTaskIdList> listTaskIds(
        io.harness.perpetualtask.DelegateId request) {
      return futureUnaryCall(getChannel().newCall(getListTaskIdsMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.harness.perpetualtask.PerpetualTaskContext>
    getTaskContext(io.harness.perpetualtask.PerpetualTaskId request) {
      return futureUnaryCall(getChannel().newCall(getGetTaskContextMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.harness.perpetualtask.HeartbeatResponse>
    publishHeartbeat(io.harness.perpetualtask.HeartbeatRequest request) {
      return futureUnaryCall(getChannel().newCall(getPublishHeartbeatMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_LIST_TASK_IDS = 0;
  private static final int METHODID_GET_TASK_CONTEXT = 1;
  private static final int METHODID_PUBLISH_HEARTBEAT = 2;

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
        case METHODID_LIST_TASK_IDS:
          serviceImpl.listTaskIds((io.harness.perpetualtask.DelegateId) request,
              (io.grpc.stub.StreamObserver<io.harness.perpetualtask.PerpetualTaskIdList>) responseObserver);
          break;
        case METHODID_GET_TASK_CONTEXT:
          serviceImpl.getTaskContext((io.harness.perpetualtask.PerpetualTaskId) request,
              (io.grpc.stub.StreamObserver<io.harness.perpetualtask.PerpetualTaskContext>) responseObserver);
          break;
        case METHODID_PUBLISH_HEARTBEAT:
          serviceImpl.publishHeartbeat((io.harness.perpetualtask.HeartbeatRequest) request,
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
                                           .addMethod(getListTaskIdsMethod())
                                           .addMethod(getGetTaskContextMethod())
                                           .addMethod(getPublishHeartbeatMethod())
                                           .build();
        }
      }
    }
    return result;
  }
}
