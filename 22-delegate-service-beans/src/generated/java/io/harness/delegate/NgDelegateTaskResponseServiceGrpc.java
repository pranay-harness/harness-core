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
@javax.annotation.Generated(
    value = "by gRPC proto compiler", comments = "Source: io/harness/delegate/ng_delegate_task_response_service.proto")
public final class NgDelegateTaskResponseServiceGrpc {
  private NgDelegateTaskResponseServiceGrpc() {}

  public static final String SERVICE_NAME = "io.harness.delegate.NgDelegateTaskResponseService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.harness.delegate.SendTaskResultRequest,
      io.harness.delegate.SendTaskResultResponse> getSendTaskResultMethod;

  @io.grpc.stub.annotations
      .RpcMethod(fullMethodName = SERVICE_NAME + '/' + "SendTaskResult",
          requestType = io.harness.delegate.SendTaskResultRequest.class,
          responseType = io.harness.delegate.SendTaskResultResponse.class,
          methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
      public static io.grpc
      .MethodDescriptor<io.harness.delegate.SendTaskResultRequest, io.harness.delegate.SendTaskResultResponse>
      getSendTaskResultMethod() {
    io.grpc.MethodDescriptor<io.harness.delegate.SendTaskResultRequest, io.harness.delegate.SendTaskResultResponse>
        getSendTaskResultMethod;
    if ((getSendTaskResultMethod = NgDelegateTaskResponseServiceGrpc.getSendTaskResultMethod) == null) {
      synchronized (NgDelegateTaskResponseServiceGrpc.class) {
        if ((getSendTaskResultMethod = NgDelegateTaskResponseServiceGrpc.getSendTaskResultMethod) == null) {
          NgDelegateTaskResponseServiceGrpc.getSendTaskResultMethod = getSendTaskResultMethod =
              io.grpc.MethodDescriptor
                  .<io.harness.delegate.SendTaskResultRequest, io.harness.delegate.SendTaskResultResponse>newBuilder()
                  .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                  .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SendTaskResult"))
                  .setSampledToLocalTracing(true)
                  .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.delegate.SendTaskResultRequest.getDefaultInstance()))
                  .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.delegate.SendTaskResultResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new NgDelegateTaskResponseServiceMethodDescriptorSupplier("SendTaskResult"))
                  .build();
        }
      }
    }
    return getSendTaskResultMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static NgDelegateTaskResponseServiceStub newStub(io.grpc.Channel channel) {
    return new NgDelegateTaskResponseServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static NgDelegateTaskResponseServiceBlockingStub newBlockingStub(io.grpc.Channel channel) {
    return new NgDelegateTaskResponseServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static NgDelegateTaskResponseServiceFutureStub newFutureStub(io.grpc.Channel channel) {
    return new NgDelegateTaskResponseServiceFutureStub(channel);
  }

  /**
   */
  public static abstract class NgDelegateTaskResponseServiceImplBase implements io.grpc.BindableService {
    /**
     */
    public void sendTaskResult(io.harness.delegate.SendTaskResultRequest request,
        io.grpc.stub.StreamObserver<io.harness.delegate.SendTaskResultResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getSendTaskResultMethod(), responseObserver);
    }

    @java.
    lang.Override
    public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(getSendTaskResultMethod(),
              asyncUnaryCall(new MethodHandlers<io.harness.delegate.SendTaskResultRequest,
                  io.harness.delegate.SendTaskResultResponse>(this, METHODID_SEND_TASK_RESULT)))
          .build();
    }
  }

  /**
   */
  public static final class NgDelegateTaskResponseServiceStub
      extends io.grpc.stub.AbstractStub<NgDelegateTaskResponseServiceStub> {
    private NgDelegateTaskResponseServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private NgDelegateTaskResponseServiceStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected NgDelegateTaskResponseServiceStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new NgDelegateTaskResponseServiceStub(channel, callOptions);
    }

    /**
     */
    public void sendTaskResult(io.harness.delegate.SendTaskResultRequest request,
        io.grpc.stub.StreamObserver<io.harness.delegate.SendTaskResultResponse> responseObserver) {
      asyncUnaryCall(getChannel().newCall(getSendTaskResultMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class NgDelegateTaskResponseServiceBlockingStub
      extends io.grpc.stub.AbstractStub<NgDelegateTaskResponseServiceBlockingStub> {
    private NgDelegateTaskResponseServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private NgDelegateTaskResponseServiceBlockingStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected NgDelegateTaskResponseServiceBlockingStub build(
        io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new NgDelegateTaskResponseServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public io.harness.delegate.SendTaskResultResponse sendTaskResult(
        io.harness.delegate.SendTaskResultRequest request) {
      return blockingUnaryCall(getChannel(), getSendTaskResultMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class NgDelegateTaskResponseServiceFutureStub
      extends io.grpc.stub.AbstractStub<NgDelegateTaskResponseServiceFutureStub> {
    private NgDelegateTaskResponseServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private NgDelegateTaskResponseServiceFutureStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected NgDelegateTaskResponseServiceFutureStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new NgDelegateTaskResponseServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.harness.delegate.SendTaskResultResponse>
    sendTaskResult(io.harness.delegate.SendTaskResultRequest request) {
      return futureUnaryCall(getChannel().newCall(getSendTaskResultMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_SEND_TASK_RESULT = 0;

  private static final class MethodHandlers<Req, Resp>
      implements io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
                 io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
                 io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
                 io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final NgDelegateTaskResponseServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(NgDelegateTaskResponseServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.
    lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_SEND_TASK_RESULT:
          serviceImpl.sendTaskResult((io.harness.delegate.SendTaskResultRequest) request,
              (io.grpc.stub.StreamObserver<io.harness.delegate.SendTaskResultResponse>) responseObserver);
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

  private static abstract class NgDelegateTaskResponseServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    NgDelegateTaskResponseServiceBaseDescriptorSupplier() {}

    @java.
    lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.harness.delegate.NgDelegateTaskResponseServiceOuterClass.getDescriptor();
    }

    @java.
    lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("NgDelegateTaskResponseService");
    }
  }

  private static final class NgDelegateTaskResponseServiceFileDescriptorSupplier
      extends NgDelegateTaskResponseServiceBaseDescriptorSupplier {
    NgDelegateTaskResponseServiceFileDescriptorSupplier() {}
  }

  private static final class NgDelegateTaskResponseServiceMethodDescriptorSupplier
      extends NgDelegateTaskResponseServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    NgDelegateTaskResponseServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (NgDelegateTaskResponseServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result =
              io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
                  .setSchemaDescriptor(new NgDelegateTaskResponseServiceFileDescriptorSupplier())
                  .addMethod(getSendTaskResultMethod())
                  .build();
        }
      }
    }
    return result;
  }
}
