package io.harness.delegate.progress;

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
Generated(value = "by gRPC proto compiler (version 1.21.0)", comments = "Source: delegate_service.proto")
public final class DelegateServiceGrpc {
  private DelegateServiceGrpc() {}

  public static final String SERVICE_NAME = "io.harness.delegate.progress.DelegateService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.harness.delegate.progress.SubmitTaskRequest,
      io.harness.delegate.progress.SubmitTaskResponse> getSubmitTaskMethod;

  @io.grpc.stub.annotations
      .RpcMethod(fullMethodName = SERVICE_NAME + '/' + "SubmitTask",
          requestType = io.harness.delegate.progress.SubmitTaskRequest.class,
          responseType = io.harness.delegate.progress.SubmitTaskResponse.class,
          methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
      public static io.grpc
      .MethodDescriptor<io.harness.delegate.progress.SubmitTaskRequest, io.harness.delegate.progress.SubmitTaskResponse>
      getSubmitTaskMethod() {
    io.grpc.MethodDescriptor<io.harness.delegate.progress.SubmitTaskRequest,
        io.harness.delegate.progress.SubmitTaskResponse> getSubmitTaskMethod;
    if ((getSubmitTaskMethod = DelegateServiceGrpc.getSubmitTaskMethod) == null) {
      synchronized (DelegateServiceGrpc.class) {
        if ((getSubmitTaskMethod = DelegateServiceGrpc.getSubmitTaskMethod) == null) {
          DelegateServiceGrpc.getSubmitTaskMethod = getSubmitTaskMethod =
              io.grpc.MethodDescriptor
                  .<io.harness.delegate.progress.SubmitTaskRequest,
                      io.harness.delegate.progress.SubmitTaskResponse>newBuilder()
                  .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                  .setFullMethodName(
                      generateFullMethodName("io.harness.delegate.progress.DelegateService", "SubmitTask"))
                  .setSampledToLocalTracing(true)
                  .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.delegate.progress.SubmitTaskRequest.getDefaultInstance()))
                  .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.delegate.progress.SubmitTaskResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new DelegateServiceMethodDescriptorSupplier("SubmitTask"))
                  .build();
        }
      }
    }
    return getSubmitTaskMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.harness.delegate.progress.CancelTaskRequest,
      io.harness.delegate.progress.CancelTaskResponse> getCancelTaskMethod;

  @io.grpc.stub.annotations
      .RpcMethod(fullMethodName = SERVICE_NAME + '/' + "CancelTask",
          requestType = io.harness.delegate.progress.CancelTaskRequest.class,
          responseType = io.harness.delegate.progress.CancelTaskResponse.class,
          methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
      public static io.grpc
      .MethodDescriptor<io.harness.delegate.progress.CancelTaskRequest, io.harness.delegate.progress.CancelTaskResponse>
      getCancelTaskMethod() {
    io.grpc.MethodDescriptor<io.harness.delegate.progress.CancelTaskRequest,
        io.harness.delegate.progress.CancelTaskResponse> getCancelTaskMethod;
    if ((getCancelTaskMethod = DelegateServiceGrpc.getCancelTaskMethod) == null) {
      synchronized (DelegateServiceGrpc.class) {
        if ((getCancelTaskMethod = DelegateServiceGrpc.getCancelTaskMethod) == null) {
          DelegateServiceGrpc.getCancelTaskMethod = getCancelTaskMethod =
              io.grpc.MethodDescriptor
                  .<io.harness.delegate.progress.CancelTaskRequest,
                      io.harness.delegate.progress.CancelTaskResponse>newBuilder()
                  .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                  .setFullMethodName(
                      generateFullMethodName("io.harness.delegate.progress.DelegateService", "CancelTask"))
                  .setSampledToLocalTracing(true)
                  .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.delegate.progress.CancelTaskRequest.getDefaultInstance()))
                  .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.delegate.progress.CancelTaskResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new DelegateServiceMethodDescriptorSupplier("CancelTask"))
                  .build();
        }
      }
    }
    return getCancelTaskMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.harness.delegate.progress.TaskProgressRequest,
      io.harness.delegate.progress.TaskProgressResponse> getTaskProgressMethod;

  @io.grpc.stub.annotations
      .RpcMethod(fullMethodName = SERVICE_NAME + '/' + "TaskProgress",
          requestType = io.harness.delegate.progress.TaskProgressRequest.class,
          responseType = io.harness.delegate.progress.TaskProgressResponse.class,
          methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
      public static io.grpc.MethodDescriptor<io.harness.delegate.progress.TaskProgressRequest,
          io.harness.delegate.progress.TaskProgressResponse>
      getTaskProgressMethod() {
    io.grpc.MethodDescriptor<io.harness.delegate.progress.TaskProgressRequest,
        io.harness.delegate.progress.TaskProgressResponse> getTaskProgressMethod;
    if ((getTaskProgressMethod = DelegateServiceGrpc.getTaskProgressMethod) == null) {
      synchronized (DelegateServiceGrpc.class) {
        if ((getTaskProgressMethod = DelegateServiceGrpc.getTaskProgressMethod) == null) {
          DelegateServiceGrpc.getTaskProgressMethod = getTaskProgressMethod =
              io.grpc.MethodDescriptor
                  .<io.harness.delegate.progress.TaskProgressRequest,
                      io.harness.delegate.progress.TaskProgressResponse>newBuilder()
                  .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                  .setFullMethodName(
                      generateFullMethodName("io.harness.delegate.progress.DelegateService", "TaskProgress"))
                  .setSampledToLocalTracing(true)
                  .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.delegate.progress.TaskProgressRequest.getDefaultInstance()))
                  .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.delegate.progress.TaskProgressResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new DelegateServiceMethodDescriptorSupplier("TaskProgress"))
                  .build();
        }
      }
    }
    return getTaskProgressMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static DelegateServiceStub newStub(io.grpc.Channel channel) {
    return new DelegateServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static DelegateServiceBlockingStub newBlockingStub(io.grpc.Channel channel) {
    return new DelegateServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static DelegateServiceFutureStub newFutureStub(io.grpc.Channel channel) {
    return new DelegateServiceFutureStub(channel);
  }

  /**
   */
  public static abstract class DelegateServiceImplBase implements io.grpc.BindableService {
    /**
     */
    public void submitTask(io.harness.delegate.progress.SubmitTaskRequest request,
        io.grpc.stub.StreamObserver<io.harness.delegate.progress.SubmitTaskResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getSubmitTaskMethod(), responseObserver);
    }

    /**
     */
    public void cancelTask(io.harness.delegate.progress.CancelTaskRequest request,
        io.grpc.stub.StreamObserver<io.harness.delegate.progress.CancelTaskResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCancelTaskMethod(), responseObserver);
    }

    /**
     */
    public void taskProgress(io.harness.delegate.progress.TaskProgressRequest request,
        io.grpc.stub.StreamObserver<io.harness.delegate.progress.TaskProgressResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getTaskProgressMethod(), responseObserver);
    }

    @java.
    lang.Override
    public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(getSubmitTaskMethod(),
              asyncUnaryCall(new MethodHandlers<io.harness.delegate.progress.SubmitTaskRequest,
                  io.harness.delegate.progress.SubmitTaskResponse>(this, METHODID_SUBMIT_TASK)))
          .addMethod(getCancelTaskMethod(),
              asyncUnaryCall(new MethodHandlers<io.harness.delegate.progress.CancelTaskRequest,
                  io.harness.delegate.progress.CancelTaskResponse>(this, METHODID_CANCEL_TASK)))
          .addMethod(getTaskProgressMethod(),
              asyncUnaryCall(new MethodHandlers<io.harness.delegate.progress.TaskProgressRequest,
                  io.harness.delegate.progress.TaskProgressResponse>(this, METHODID_TASK_PROGRESS)))
          .build();
    }
  }

  /**
   */
  public static final class DelegateServiceStub extends io.grpc.stub.AbstractStub<DelegateServiceStub> {
    private DelegateServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private DelegateServiceStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DelegateServiceStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new DelegateServiceStub(channel, callOptions);
    }

    /**
     */
    public void submitTask(io.harness.delegate.progress.SubmitTaskRequest request,
        io.grpc.stub.StreamObserver<io.harness.delegate.progress.SubmitTaskResponse> responseObserver) {
      asyncUnaryCall(getChannel().newCall(getSubmitTaskMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void cancelTask(io.harness.delegate.progress.CancelTaskRequest request,
        io.grpc.stub.StreamObserver<io.harness.delegate.progress.CancelTaskResponse> responseObserver) {
      asyncUnaryCall(getChannel().newCall(getCancelTaskMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void taskProgress(io.harness.delegate.progress.TaskProgressRequest request,
        io.grpc.stub.StreamObserver<io.harness.delegate.progress.TaskProgressResponse> responseObserver) {
      asyncUnaryCall(getChannel().newCall(getTaskProgressMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class DelegateServiceBlockingStub extends io.grpc.stub.AbstractStub<DelegateServiceBlockingStub> {
    private DelegateServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private DelegateServiceBlockingStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DelegateServiceBlockingStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new DelegateServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public io.harness.delegate.progress.SubmitTaskResponse submitTask(
        io.harness.delegate.progress.SubmitTaskRequest request) {
      return blockingUnaryCall(getChannel(), getSubmitTaskMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.harness.delegate.progress.CancelTaskResponse cancelTask(
        io.harness.delegate.progress.CancelTaskRequest request) {
      return blockingUnaryCall(getChannel(), getCancelTaskMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.harness.delegate.progress.TaskProgressResponse taskProgress(
        io.harness.delegate.progress.TaskProgressRequest request) {
      return blockingUnaryCall(getChannel(), getTaskProgressMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class DelegateServiceFutureStub extends io.grpc.stub.AbstractStub<DelegateServiceFutureStub> {
    private DelegateServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private DelegateServiceFutureStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected DelegateServiceFutureStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new DelegateServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.harness.delegate.progress.SubmitTaskResponse>
    submitTask(io.harness.delegate.progress.SubmitTaskRequest request) {
      return futureUnaryCall(getChannel().newCall(getSubmitTaskMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.harness.delegate.progress.CancelTaskResponse>
    cancelTask(io.harness.delegate.progress.CancelTaskRequest request) {
      return futureUnaryCall(getChannel().newCall(getCancelTaskMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.harness.delegate.progress.TaskProgressResponse>
    taskProgress(io.harness.delegate.progress.TaskProgressRequest request) {
      return futureUnaryCall(getChannel().newCall(getTaskProgressMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_SUBMIT_TASK = 0;
  private static final int METHODID_CANCEL_TASK = 1;
  private static final int METHODID_TASK_PROGRESS = 2;

  private static final class MethodHandlers<Req, Resp>
      implements io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
                 io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
                 io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
                 io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final DelegateServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(DelegateServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.
    lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SUBMIT_TASK:
          serviceImpl.submitTask((io.harness.delegate.progress.SubmitTaskRequest) request,
              (io.grpc.stub.StreamObserver<io.harness.delegate.progress.SubmitTaskResponse>) responseObserver);
          break;
        case METHODID_CANCEL_TASK:
          serviceImpl.cancelTask((io.harness.delegate.progress.CancelTaskRequest) request,
              (io.grpc.stub.StreamObserver<io.harness.delegate.progress.CancelTaskResponse>) responseObserver);
          break;
        case METHODID_TASK_PROGRESS:
          serviceImpl.taskProgress((io.harness.delegate.progress.TaskProgressRequest) request,
              (io.grpc.stub.StreamObserver<io.harness.delegate.progress.TaskProgressResponse>) responseObserver);
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

  private static abstract class DelegateServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    DelegateServiceBaseDescriptorSupplier() {}

    @java.
    lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.harness.delegate.progress.DelegateServiceOuterClass.getDescriptor();
    }

    @java.
    lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("DelegateService");
    }
  }

  private static final class DelegateServiceFileDescriptorSupplier extends DelegateServiceBaseDescriptorSupplier {
    DelegateServiceFileDescriptorSupplier() {}
  }

  private static final class DelegateServiceMethodDescriptorSupplier
      extends DelegateServiceBaseDescriptorSupplier implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    DelegateServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (DelegateServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
                                           .setSchemaDescriptor(new DelegateServiceFileDescriptorSupplier())
                                           .addMethod(getSubmitTaskMethod())
                                           .addMethod(getCancelTaskMethod())
                                           .addMethod(getTaskProgressMethod())
                                           .build();
        }
      }
    }
    return result;
  }
}
