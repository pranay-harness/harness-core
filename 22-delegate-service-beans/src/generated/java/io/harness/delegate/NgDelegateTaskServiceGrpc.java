package io.harness.delegate;

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
Generated(value = "by gRPC proto compiler", comments = "Source: io/harness/delegate/ng_delegate_task_service.proto")
public final class NgDelegateTaskServiceGrpc {
  private NgDelegateTaskServiceGrpc() {}

  public static final String SERVICE_NAME = "io.harness.delegate.NgDelegateTaskService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc
      .MethodDescriptor<io.harness.delegate.SendTaskRequest, io.harness.delegate.SendTaskResponse> getSendTaskMethod;

  @io.grpc.stub.annotations
      .RpcMethod(fullMethodName = SERVICE_NAME + '/' + "SendTask",
          requestType = io.harness.delegate.SendTaskRequest.class,
          responseType = io.harness.delegate.SendTaskResponse.class,
          methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
      public static io.grpc.MethodDescriptor<io.harness.delegate.SendTaskRequest, io.harness.delegate.SendTaskResponse>
      getSendTaskMethod() {
    io.grpc
        .MethodDescriptor<io.harness.delegate.SendTaskRequest, io.harness.delegate.SendTaskResponse> getSendTaskMethod;
    if ((getSendTaskMethod = NgDelegateTaskServiceGrpc.getSendTaskMethod) == null) {
      synchronized (NgDelegateTaskServiceGrpc.class) {
        if ((getSendTaskMethod = NgDelegateTaskServiceGrpc.getSendTaskMethod) == null) {
          NgDelegateTaskServiceGrpc.getSendTaskMethod = getSendTaskMethod =
              io.grpc.MethodDescriptor
                  .<io.harness.delegate.SendTaskRequest, io.harness.delegate.SendTaskResponse>newBuilder()
                  .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                  .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SendTask"))
                  .setSampledToLocalTracing(true)
                  .setRequestMarshaller(
                      io.grpc.protobuf.ProtoUtils.marshaller(io.harness.delegate.SendTaskRequest.getDefaultInstance()))
                  .setResponseMarshaller(
                      io.grpc.protobuf.ProtoUtils.marshaller(io.harness.delegate.SendTaskResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new NgDelegateTaskServiceMethodDescriptorSupplier("SendTask"))
                  .build();
        }
      }
    }
    return getSendTaskMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.harness.delegate.SendTaskAsyncRequest,
      io.harness.delegate.SendTaskAsyncResponse> getSendTaskAsyncMethod;

  @io.grpc.stub.annotations
      .RpcMethod(fullMethodName = SERVICE_NAME + '/' + "SendTaskAsync",
          requestType = io.harness.delegate.SendTaskAsyncRequest.class,
          responseType = io.harness.delegate.SendTaskAsyncResponse.class,
          methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
      public static io.grpc
      .MethodDescriptor<io.harness.delegate.SendTaskAsyncRequest, io.harness.delegate.SendTaskAsyncResponse>
      getSendTaskAsyncMethod() {
    io.grpc.MethodDescriptor<io.harness.delegate.SendTaskAsyncRequest, io.harness.delegate.SendTaskAsyncResponse>
        getSendTaskAsyncMethod;
    if ((getSendTaskAsyncMethod = NgDelegateTaskServiceGrpc.getSendTaskAsyncMethod) == null) {
      synchronized (NgDelegateTaskServiceGrpc.class) {
        if ((getSendTaskAsyncMethod = NgDelegateTaskServiceGrpc.getSendTaskAsyncMethod) == null) {
          NgDelegateTaskServiceGrpc.getSendTaskAsyncMethod = getSendTaskAsyncMethod =
              io.grpc.MethodDescriptor
                  .<io.harness.delegate.SendTaskAsyncRequest, io.harness.delegate.SendTaskAsyncResponse>newBuilder()
                  .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                  .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SendTaskAsync"))
                  .setSampledToLocalTracing(true)
                  .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.delegate.SendTaskAsyncRequest.getDefaultInstance()))
                  .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.delegate.SendTaskAsyncResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new NgDelegateTaskServiceMethodDescriptorSupplier("SendTaskAsync"))
                  .build();
        }
      }
    }
    return getSendTaskAsyncMethod;
  }

  private static volatile io.grpc
      .MethodDescriptor<io.harness.delegate.AbortTaskRequest, io.harness.delegate.AbortTaskResponse> getAbortTaskMethod;

  @io.grpc.stub.annotations
      .RpcMethod(fullMethodName = SERVICE_NAME + '/' + "AbortTask",
          requestType = io.harness.delegate.AbortTaskRequest.class,
          responseType = io.harness.delegate.AbortTaskResponse.class,
          methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
      public static io.grpc
      .MethodDescriptor<io.harness.delegate.AbortTaskRequest, io.harness.delegate.AbortTaskResponse>
      getAbortTaskMethod() {
    io.grpc.MethodDescriptor<io.harness.delegate.AbortTaskRequest, io.harness.delegate.AbortTaskResponse>
        getAbortTaskMethod;
    if ((getAbortTaskMethod = NgDelegateTaskServiceGrpc.getAbortTaskMethod) == null) {
      synchronized (NgDelegateTaskServiceGrpc.class) {
        if ((getAbortTaskMethod = NgDelegateTaskServiceGrpc.getAbortTaskMethod) == null) {
          NgDelegateTaskServiceGrpc.getAbortTaskMethod = getAbortTaskMethod =
              io.grpc.MethodDescriptor
                  .<io.harness.delegate.AbortTaskRequest, io.harness.delegate.AbortTaskResponse>newBuilder()
                  .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                  .setFullMethodName(generateFullMethodName(SERVICE_NAME, "AbortTask"))
                  .setSampledToLocalTracing(true)
                  .setRequestMarshaller(
                      io.grpc.protobuf.ProtoUtils.marshaller(io.harness.delegate.AbortTaskRequest.getDefaultInstance()))
                  .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.delegate.AbortTaskResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new NgDelegateTaskServiceMethodDescriptorSupplier("AbortTask"))
                  .build();
        }
      }
    }
    return getAbortTaskMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static NgDelegateTaskServiceStub newStub(io.grpc.Channel channel) {
    return new NgDelegateTaskServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static NgDelegateTaskServiceBlockingStub newBlockingStub(io.grpc.Channel channel) {
    return new NgDelegateTaskServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static NgDelegateTaskServiceFutureStub newFutureStub(io.grpc.Channel channel) {
    return new NgDelegateTaskServiceFutureStub(channel);
  }

  /**
   */
  public static abstract class NgDelegateTaskServiceImplBase implements io.grpc.BindableService {
    /**
     */
    public void sendTask(io.harness.delegate.SendTaskRequest request,
        io.grpc.stub.StreamObserver<io.harness.delegate.SendTaskResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getSendTaskMethod(), responseObserver);
    }

    /**
     */
    public void sendTaskAsync(io.harness.delegate.SendTaskAsyncRequest request,
        io.grpc.stub.StreamObserver<io.harness.delegate.SendTaskAsyncResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getSendTaskAsyncMethod(), responseObserver);
    }

    /**
     */
    public void abortTask(io.harness.delegate.AbortTaskRequest request,
        io.grpc.stub.StreamObserver<io.harness.delegate.AbortTaskResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getAbortTaskMethod(), responseObserver);
    }

    @java.
    lang.Override
    public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(getSendTaskMethod(),
              asyncUnaryCall(
                  new MethodHandlers<io.harness.delegate.SendTaskRequest, io.harness.delegate.SendTaskResponse>(
                      this, METHODID_SEND_TASK)))
          .addMethod(getSendTaskAsyncMethod(),
              asyncUnaryCall(new MethodHandlers<io.harness.delegate.SendTaskAsyncRequest,
                  io.harness.delegate.SendTaskAsyncResponse>(this, METHODID_SEND_TASK_ASYNC)))
          .addMethod(getAbortTaskMethod(),
              asyncUnaryCall(
                  new MethodHandlers<io.harness.delegate.AbortTaskRequest, io.harness.delegate.AbortTaskResponse>(
                      this, METHODID_ABORT_TASK)))
          .build();
    }
  }

  /**
   */
  public static final class NgDelegateTaskServiceStub extends io.grpc.stub.AbstractStub<NgDelegateTaskServiceStub> {
    private NgDelegateTaskServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private NgDelegateTaskServiceStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected NgDelegateTaskServiceStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new NgDelegateTaskServiceStub(channel, callOptions);
    }

    /**
     */
    public void sendTask(io.harness.delegate.SendTaskRequest request,
        io.grpc.stub.StreamObserver<io.harness.delegate.SendTaskResponse> responseObserver) {
      asyncUnaryCall(getChannel().newCall(getSendTaskMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void sendTaskAsync(io.harness.delegate.SendTaskAsyncRequest request,
        io.grpc.stub.StreamObserver<io.harness.delegate.SendTaskAsyncResponse> responseObserver) {
      asyncUnaryCall(getChannel().newCall(getSendTaskAsyncMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void abortTask(io.harness.delegate.AbortTaskRequest request,
        io.grpc.stub.StreamObserver<io.harness.delegate.AbortTaskResponse> responseObserver) {
      asyncUnaryCall(getChannel().newCall(getAbortTaskMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class NgDelegateTaskServiceBlockingStub
      extends io.grpc.stub.AbstractStub<NgDelegateTaskServiceBlockingStub> {
    private NgDelegateTaskServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private NgDelegateTaskServiceBlockingStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected NgDelegateTaskServiceBlockingStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new NgDelegateTaskServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public io.harness.delegate.SendTaskResponse sendTask(io.harness.delegate.SendTaskRequest request) {
      return blockingUnaryCall(getChannel(), getSendTaskMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.harness.delegate.SendTaskAsyncResponse sendTaskAsync(io.harness.delegate.SendTaskAsyncRequest request) {
      return blockingUnaryCall(getChannel(), getSendTaskAsyncMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.harness.delegate.AbortTaskResponse abortTask(io.harness.delegate.AbortTaskRequest request) {
      return blockingUnaryCall(getChannel(), getAbortTaskMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class NgDelegateTaskServiceFutureStub
      extends io.grpc.stub.AbstractStub<NgDelegateTaskServiceFutureStub> {
    private NgDelegateTaskServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private NgDelegateTaskServiceFutureStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected NgDelegateTaskServiceFutureStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new NgDelegateTaskServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.harness.delegate.SendTaskResponse> sendTask(
        io.harness.delegate.SendTaskRequest request) {
      return futureUnaryCall(getChannel().newCall(getSendTaskMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.harness.delegate.SendTaskAsyncResponse> sendTaskAsync(
        io.harness.delegate.SendTaskAsyncRequest request) {
      return futureUnaryCall(getChannel().newCall(getSendTaskAsyncMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.harness.delegate.AbortTaskResponse> abortTask(
        io.harness.delegate.AbortTaskRequest request) {
      return futureUnaryCall(getChannel().newCall(getAbortTaskMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_SEND_TASK = 0;
  private static final int METHODID_SEND_TASK_ASYNC = 1;
  private static final int METHODID_ABORT_TASK = 2;

  private static final class MethodHandlers<Req, Resp>
      implements io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
                 io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
                 io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
                 io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final NgDelegateTaskServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(NgDelegateTaskServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.
    lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SEND_TASK:
          serviceImpl.sendTask((io.harness.delegate.SendTaskRequest) request,
              (io.grpc.stub.StreamObserver<io.harness.delegate.SendTaskResponse>) responseObserver);
          break;
        case METHODID_SEND_TASK_ASYNC:
          serviceImpl.sendTaskAsync((io.harness.delegate.SendTaskAsyncRequest) request,
              (io.grpc.stub.StreamObserver<io.harness.delegate.SendTaskAsyncResponse>) responseObserver);
          break;
        case METHODID_ABORT_TASK:
          serviceImpl.abortTask((io.harness.delegate.AbortTaskRequest) request,
              (io.grpc.stub.StreamObserver<io.harness.delegate.AbortTaskResponse>) responseObserver);
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

  private static abstract class NgDelegateTaskServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    NgDelegateTaskServiceBaseDescriptorSupplier() {}

    @java.
    lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.harness.delegate.NgDelegateTaskServiceOuterClass.getDescriptor();
    }

    @java.
    lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("NgDelegateTaskService");
    }
  }

  private static final class NgDelegateTaskServiceFileDescriptorSupplier
      extends NgDelegateTaskServiceBaseDescriptorSupplier {
    NgDelegateTaskServiceFileDescriptorSupplier() {}
  }

  private static final class NgDelegateTaskServiceMethodDescriptorSupplier
      extends NgDelegateTaskServiceBaseDescriptorSupplier implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    NgDelegateTaskServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (NgDelegateTaskServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
                                           .setSchemaDescriptor(new NgDelegateTaskServiceFileDescriptorSupplier())
                                           .addMethod(getSendTaskMethod())
                                           .addMethod(getSendTaskAsyncMethod())
                                           .addMethod(getAbortTaskMethod())
                                           .build();
        }
      }
    }
    return result;
  }
}
