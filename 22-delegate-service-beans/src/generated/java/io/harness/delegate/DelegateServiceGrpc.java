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
Generated(value = "by gRPC proto compiler", comments = "Source: io/harness/delegate/delegate_service.proto")
public final class DelegateServiceGrpc {
  private DelegateServiceGrpc() {}

  public static final String SERVICE_NAME = "io.harness.delegate.DelegateService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.harness.delegate.SubmitTaskRequest,
      io.harness.delegate.SubmitTaskResponse> getSubmitTaskMethod;

  @io.grpc.stub.annotations
      .RpcMethod(fullMethodName = SERVICE_NAME + '/' + "SubmitTask",
          requestType = io.harness.delegate.SubmitTaskRequest.class,
          responseType = io.harness.delegate.SubmitTaskResponse.class,
          methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
      public static io.grpc
      .MethodDescriptor<io.harness.delegate.SubmitTaskRequest, io.harness.delegate.SubmitTaskResponse>
      getSubmitTaskMethod() {
    io.grpc.MethodDescriptor<io.harness.delegate.SubmitTaskRequest, io.harness.delegate.SubmitTaskResponse>
        getSubmitTaskMethod;
    if ((getSubmitTaskMethod = DelegateServiceGrpc.getSubmitTaskMethod) == null) {
      synchronized (DelegateServiceGrpc.class) {
        if ((getSubmitTaskMethod = DelegateServiceGrpc.getSubmitTaskMethod) == null) {
          DelegateServiceGrpc.getSubmitTaskMethod = getSubmitTaskMethod =
              io.grpc.MethodDescriptor
                  .<io.harness.delegate.SubmitTaskRequest, io.harness.delegate.SubmitTaskResponse>newBuilder()
                  .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                  .setFullMethodName(generateFullMethodName(SERVICE_NAME, "SubmitTask"))
                  .setSampledToLocalTracing(true)
                  .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.delegate.SubmitTaskRequest.getDefaultInstance()))
                  .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.delegate.SubmitTaskResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new DelegateServiceMethodDescriptorSupplier("SubmitTask"))
                  .build();
        }
      }
    }
    return getSubmitTaskMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.harness.delegate.CancelTaskRequest,
      io.harness.delegate.CancelTaskResponse> getCancelTaskMethod;

  @io.grpc.stub.annotations
      .RpcMethod(fullMethodName = SERVICE_NAME + '/' + "CancelTask",
          requestType = io.harness.delegate.CancelTaskRequest.class,
          responseType = io.harness.delegate.CancelTaskResponse.class,
          methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
      public static io.grpc
      .MethodDescriptor<io.harness.delegate.CancelTaskRequest, io.harness.delegate.CancelTaskResponse>
      getCancelTaskMethod() {
    io.grpc.MethodDescriptor<io.harness.delegate.CancelTaskRequest, io.harness.delegate.CancelTaskResponse>
        getCancelTaskMethod;
    if ((getCancelTaskMethod = DelegateServiceGrpc.getCancelTaskMethod) == null) {
      synchronized (DelegateServiceGrpc.class) {
        if ((getCancelTaskMethod = DelegateServiceGrpc.getCancelTaskMethod) == null) {
          DelegateServiceGrpc.getCancelTaskMethod = getCancelTaskMethod =
              io.grpc.MethodDescriptor
                  .<io.harness.delegate.CancelTaskRequest, io.harness.delegate.CancelTaskResponse>newBuilder()
                  .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                  .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CancelTask"))
                  .setSampledToLocalTracing(true)
                  .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.delegate.CancelTaskRequest.getDefaultInstance()))
                  .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.delegate.CancelTaskResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new DelegateServiceMethodDescriptorSupplier("CancelTask"))
                  .build();
        }
      }
    }
    return getCancelTaskMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.harness.delegate.TaskProgressRequest,
      io.harness.delegate.TaskProgressResponse> getTaskProgressMethod;

  @io.grpc.stub.annotations
      .RpcMethod(fullMethodName = SERVICE_NAME + '/' + "TaskProgress",
          requestType = io.harness.delegate.TaskProgressRequest.class,
          responseType = io.harness.delegate.TaskProgressResponse.class,
          methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
      public static io.grpc
      .MethodDescriptor<io.harness.delegate.TaskProgressRequest, io.harness.delegate.TaskProgressResponse>
      getTaskProgressMethod() {
    io.grpc.MethodDescriptor<io.harness.delegate.TaskProgressRequest, io.harness.delegate.TaskProgressResponse>
        getTaskProgressMethod;
    if ((getTaskProgressMethod = DelegateServiceGrpc.getTaskProgressMethod) == null) {
      synchronized (DelegateServiceGrpc.class) {
        if ((getTaskProgressMethod = DelegateServiceGrpc.getTaskProgressMethod) == null) {
          DelegateServiceGrpc.getTaskProgressMethod = getTaskProgressMethod =
              io.grpc.MethodDescriptor
                  .<io.harness.delegate.TaskProgressRequest, io.harness.delegate.TaskProgressResponse>newBuilder()
                  .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                  .setFullMethodName(generateFullMethodName(SERVICE_NAME, "TaskProgress"))
                  .setSampledToLocalTracing(true)
                  .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.delegate.TaskProgressRequest.getDefaultInstance()))
                  .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.delegate.TaskProgressResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new DelegateServiceMethodDescriptorSupplier("TaskProgress"))
                  .build();
        }
      }
    }
    return getTaskProgressMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.harness.delegate.TaskProgressUpdatesRequest,
      io.harness.delegate.TaskProgressUpdatesResponse> getTaskProgressUpdatesMethod;

  @io.grpc.stub.annotations
      .RpcMethod(fullMethodName = SERVICE_NAME + '/' + "TaskProgressUpdates",
          requestType = io.harness.delegate.TaskProgressUpdatesRequest.class,
          responseType = io.harness.delegate.TaskProgressUpdatesResponse.class,
          methodType = io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
      public static io.grpc
      .MethodDescriptor<io.harness.delegate.TaskProgressUpdatesRequest, io.harness.delegate.TaskProgressUpdatesResponse>
      getTaskProgressUpdatesMethod() {
    io.grpc.MethodDescriptor<io.harness.delegate.TaskProgressUpdatesRequest,
        io.harness.delegate.TaskProgressUpdatesResponse> getTaskProgressUpdatesMethod;
    if ((getTaskProgressUpdatesMethod = DelegateServiceGrpc.getTaskProgressUpdatesMethod) == null) {
      synchronized (DelegateServiceGrpc.class) {
        if ((getTaskProgressUpdatesMethod = DelegateServiceGrpc.getTaskProgressUpdatesMethod) == null) {
          DelegateServiceGrpc.getTaskProgressUpdatesMethod = getTaskProgressUpdatesMethod =
              io.grpc.MethodDescriptor
                  .<io.harness.delegate.TaskProgressUpdatesRequest,
                      io.harness.delegate.TaskProgressUpdatesResponse>newBuilder()
                  .setType(io.grpc.MethodDescriptor.MethodType.SERVER_STREAMING)
                  .setFullMethodName(generateFullMethodName(SERVICE_NAME, "TaskProgressUpdates"))
                  .setSampledToLocalTracing(true)
                  .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.delegate.TaskProgressUpdatesRequest.getDefaultInstance()))
                  .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.delegate.TaskProgressUpdatesResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new DelegateServiceMethodDescriptorSupplier("TaskProgressUpdates"))
                  .build();
        }
      }
    }
    return getTaskProgressUpdatesMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest,
      io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse> getRegisterPerpetualTaskClientEntrypointMethod;

  @io.grpc.stub.annotations
      .RpcMethod(fullMethodName = SERVICE_NAME + '/' + "RegisterPerpetualTaskClientEntrypoint",
          requestType = io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest.class,
          responseType = io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse.class,
          methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
      public static io.grpc.MethodDescriptor<io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest,
          io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse>
      getRegisterPerpetualTaskClientEntrypointMethod() {
    io.grpc.MethodDescriptor<io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest,
        io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse>
        getRegisterPerpetualTaskClientEntrypointMethod;
    if ((getRegisterPerpetualTaskClientEntrypointMethod =
                DelegateServiceGrpc.getRegisterPerpetualTaskClientEntrypointMethod)
        == null) {
      synchronized (DelegateServiceGrpc.class) {
        if ((getRegisterPerpetualTaskClientEntrypointMethod =
                    DelegateServiceGrpc.getRegisterPerpetualTaskClientEntrypointMethod)
            == null) {
          DelegateServiceGrpc.getRegisterPerpetualTaskClientEntrypointMethod =
              getRegisterPerpetualTaskClientEntrypointMethod =
                  io.grpc.MethodDescriptor
                      .<io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest,
                          io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse>newBuilder()
                      .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                      .setFullMethodName(generateFullMethodName(SERVICE_NAME, "RegisterPerpetualTaskClientEntrypoint"))
                      .setSampledToLocalTracing(true)
                      .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                          io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest.getDefaultInstance()))
                      .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                          io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse.getDefaultInstance()))
                      .setSchemaDescriptor(
                          new DelegateServiceMethodDescriptorSupplier("RegisterPerpetualTaskClientEntrypoint"))
                      .build();
        }
      }
    }
    return getRegisterPerpetualTaskClientEntrypointMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.harness.delegate.CreatePerpetualTaskRequest,
      io.harness.delegate.CreatePerpetualTaskResponse> getCreatePerpetualTaskMethod;

  @io.grpc.stub.annotations
      .RpcMethod(fullMethodName = SERVICE_NAME + '/' + "CreatePerpetualTask",
          requestType = io.harness.delegate.CreatePerpetualTaskRequest.class,
          responseType = io.harness.delegate.CreatePerpetualTaskResponse.class,
          methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
      public static io.grpc
      .MethodDescriptor<io.harness.delegate.CreatePerpetualTaskRequest, io.harness.delegate.CreatePerpetualTaskResponse>
      getCreatePerpetualTaskMethod() {
    io.grpc.MethodDescriptor<io.harness.delegate.CreatePerpetualTaskRequest,
        io.harness.delegate.CreatePerpetualTaskResponse> getCreatePerpetualTaskMethod;
    if ((getCreatePerpetualTaskMethod = DelegateServiceGrpc.getCreatePerpetualTaskMethod) == null) {
      synchronized (DelegateServiceGrpc.class) {
        if ((getCreatePerpetualTaskMethod = DelegateServiceGrpc.getCreatePerpetualTaskMethod) == null) {
          DelegateServiceGrpc.getCreatePerpetualTaskMethod = getCreatePerpetualTaskMethod =
              io.grpc.MethodDescriptor
                  .<io.harness.delegate.CreatePerpetualTaskRequest,
                      io.harness.delegate.CreatePerpetualTaskResponse>newBuilder()
                  .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                  .setFullMethodName(generateFullMethodName(SERVICE_NAME, "CreatePerpetualTask"))
                  .setSampledToLocalTracing(true)
                  .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.delegate.CreatePerpetualTaskRequest.getDefaultInstance()))
                  .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.delegate.CreatePerpetualTaskResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new DelegateServiceMethodDescriptorSupplier("CreatePerpetualTask"))
                  .build();
        }
      }
    }
    return getCreatePerpetualTaskMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.harness.delegate.DeletePerpetualTaskRequest,
      io.harness.delegate.DeletePerpetualTaskResponse> getDeletePerpetualTaskMethod;

  @io.grpc.stub.annotations
      .RpcMethod(fullMethodName = SERVICE_NAME + '/' + "DeletePerpetualTask",
          requestType = io.harness.delegate.DeletePerpetualTaskRequest.class,
          responseType = io.harness.delegate.DeletePerpetualTaskResponse.class,
          methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
      public static io.grpc
      .MethodDescriptor<io.harness.delegate.DeletePerpetualTaskRequest, io.harness.delegate.DeletePerpetualTaskResponse>
      getDeletePerpetualTaskMethod() {
    io.grpc.MethodDescriptor<io.harness.delegate.DeletePerpetualTaskRequest,
        io.harness.delegate.DeletePerpetualTaskResponse> getDeletePerpetualTaskMethod;
    if ((getDeletePerpetualTaskMethod = DelegateServiceGrpc.getDeletePerpetualTaskMethod) == null) {
      synchronized (DelegateServiceGrpc.class) {
        if ((getDeletePerpetualTaskMethod = DelegateServiceGrpc.getDeletePerpetualTaskMethod) == null) {
          DelegateServiceGrpc.getDeletePerpetualTaskMethod = getDeletePerpetualTaskMethod =
              io.grpc.MethodDescriptor
                  .<io.harness.delegate.DeletePerpetualTaskRequest,
                      io.harness.delegate.DeletePerpetualTaskResponse>newBuilder()
                  .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                  .setFullMethodName(generateFullMethodName(SERVICE_NAME, "DeletePerpetualTask"))
                  .setSampledToLocalTracing(true)
                  .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.delegate.DeletePerpetualTaskRequest.getDefaultInstance()))
                  .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.delegate.DeletePerpetualTaskResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new DelegateServiceMethodDescriptorSupplier("DeletePerpetualTask"))
                  .build();
        }
      }
    }
    return getDeletePerpetualTaskMethod;
  }

  private static volatile io.grpc.MethodDescriptor<io.harness.delegate.ResetPerpetualTaskRequest,
      io.harness.delegate.ResetPerpetualTaskResponse> getResetPerpetualTaskMethod;

  @io.grpc.stub.annotations
      .RpcMethod(fullMethodName = SERVICE_NAME + '/' + "ResetPerpetualTask",
          requestType = io.harness.delegate.ResetPerpetualTaskRequest.class,
          responseType = io.harness.delegate.ResetPerpetualTaskResponse.class,
          methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
      public static io.grpc
      .MethodDescriptor<io.harness.delegate.ResetPerpetualTaskRequest, io.harness.delegate.ResetPerpetualTaskResponse>
      getResetPerpetualTaskMethod() {
    io.grpc.MethodDescriptor<io.harness.delegate.ResetPerpetualTaskRequest,
        io.harness.delegate.ResetPerpetualTaskResponse> getResetPerpetualTaskMethod;
    if ((getResetPerpetualTaskMethod = DelegateServiceGrpc.getResetPerpetualTaskMethod) == null) {
      synchronized (DelegateServiceGrpc.class) {
        if ((getResetPerpetualTaskMethod = DelegateServiceGrpc.getResetPerpetualTaskMethod) == null) {
          DelegateServiceGrpc.getResetPerpetualTaskMethod = getResetPerpetualTaskMethod =
              io.grpc.MethodDescriptor
                  .<io.harness.delegate.ResetPerpetualTaskRequest,
                      io.harness.delegate.ResetPerpetualTaskResponse>newBuilder()
                  .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                  .setFullMethodName(generateFullMethodName(SERVICE_NAME, "ResetPerpetualTask"))
                  .setSampledToLocalTracing(true)
                  .setRequestMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.delegate.ResetPerpetualTaskRequest.getDefaultInstance()))
                  .setResponseMarshaller(io.grpc.protobuf.ProtoUtils.marshaller(
                      io.harness.delegate.ResetPerpetualTaskResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new DelegateServiceMethodDescriptorSupplier("ResetPerpetualTask"))
                  .build();
        }
      }
    }
    return getResetPerpetualTaskMethod;
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
    public void submitTask(io.harness.delegate.SubmitTaskRequest request,
        io.grpc.stub.StreamObserver<io.harness.delegate.SubmitTaskResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getSubmitTaskMethod(), responseObserver);
    }

    /**
     */
    public void cancelTask(io.harness.delegate.CancelTaskRequest request,
        io.grpc.stub.StreamObserver<io.harness.delegate.CancelTaskResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCancelTaskMethod(), responseObserver);
    }

    /**
     */
    public void taskProgress(io.harness.delegate.TaskProgressRequest request,
        io.grpc.stub.StreamObserver<io.harness.delegate.TaskProgressResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getTaskProgressMethod(), responseObserver);
    }

    /**
     */
    public void taskProgressUpdates(io.harness.delegate.TaskProgressUpdatesRequest request,
        io.grpc.stub.StreamObserver<io.harness.delegate.TaskProgressUpdatesResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getTaskProgressUpdatesMethod(), responseObserver);
    }

    /**
     */
    public void registerPerpetualTaskClientEntrypoint(
        io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest request,
        io.grpc.stub
            .StreamObserver<io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getRegisterPerpetualTaskClientEntrypointMethod(), responseObserver);
    }

    /**
     */
    public void createPerpetualTask(io.harness.delegate.CreatePerpetualTaskRequest request,
        io.grpc.stub.StreamObserver<io.harness.delegate.CreatePerpetualTaskResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getCreatePerpetualTaskMethod(), responseObserver);
    }

    /**
     */
    public void deletePerpetualTask(io.harness.delegate.DeletePerpetualTaskRequest request,
        io.grpc.stub.StreamObserver<io.harness.delegate.DeletePerpetualTaskResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getDeletePerpetualTaskMethod(), responseObserver);
    }

    /**
     */
    public void resetPerpetualTask(io.harness.delegate.ResetPerpetualTaskRequest request,
        io.grpc.stub.StreamObserver<io.harness.delegate.ResetPerpetualTaskResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getResetPerpetualTaskMethod(), responseObserver);
    }

    @java.
    lang.Override
    public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(getSubmitTaskMethod(),
              asyncUnaryCall(
                  new MethodHandlers<io.harness.delegate.SubmitTaskRequest, io.harness.delegate.SubmitTaskResponse>(
                      this, METHODID_SUBMIT_TASK)))
          .addMethod(getCancelTaskMethod(),
              asyncUnaryCall(
                  new MethodHandlers<io.harness.delegate.CancelTaskRequest, io.harness.delegate.CancelTaskResponse>(
                      this, METHODID_CANCEL_TASK)))
          .addMethod(getTaskProgressMethod(),
              asyncUnaryCall(
                  new MethodHandlers<io.harness.delegate.TaskProgressRequest, io.harness.delegate.TaskProgressResponse>(
                      this, METHODID_TASK_PROGRESS)))
          .addMethod(getTaskProgressUpdatesMethod(),
              asyncServerStreamingCall(new MethodHandlers<io.harness.delegate.TaskProgressUpdatesRequest,
                  io.harness.delegate.TaskProgressUpdatesResponse>(this, METHODID_TASK_PROGRESS_UPDATES)))
          .addMethod(getRegisterPerpetualTaskClientEntrypointMethod(),
              asyncUnaryCall(new MethodHandlers<io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest,
                  io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse>(
                  this, METHODID_REGISTER_PERPETUAL_TASK_CLIENT_ENTRYPOINT)))
          .addMethod(getCreatePerpetualTaskMethod(),
              asyncUnaryCall(new MethodHandlers<io.harness.delegate.CreatePerpetualTaskRequest,
                  io.harness.delegate.CreatePerpetualTaskResponse>(this, METHODID_CREATE_PERPETUAL_TASK)))
          .addMethod(getDeletePerpetualTaskMethod(),
              asyncUnaryCall(new MethodHandlers<io.harness.delegate.DeletePerpetualTaskRequest,
                  io.harness.delegate.DeletePerpetualTaskResponse>(this, METHODID_DELETE_PERPETUAL_TASK)))
          .addMethod(getResetPerpetualTaskMethod(),
              asyncUnaryCall(new MethodHandlers<io.harness.delegate.ResetPerpetualTaskRequest,
                  io.harness.delegate.ResetPerpetualTaskResponse>(this, METHODID_RESET_PERPETUAL_TASK)))
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
    public void submitTask(io.harness.delegate.SubmitTaskRequest request,
        io.grpc.stub.StreamObserver<io.harness.delegate.SubmitTaskResponse> responseObserver) {
      asyncUnaryCall(getChannel().newCall(getSubmitTaskMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void cancelTask(io.harness.delegate.CancelTaskRequest request,
        io.grpc.stub.StreamObserver<io.harness.delegate.CancelTaskResponse> responseObserver) {
      asyncUnaryCall(getChannel().newCall(getCancelTaskMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void taskProgress(io.harness.delegate.TaskProgressRequest request,
        io.grpc.stub.StreamObserver<io.harness.delegate.TaskProgressResponse> responseObserver) {
      asyncUnaryCall(getChannel().newCall(getTaskProgressMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void taskProgressUpdates(io.harness.delegate.TaskProgressUpdatesRequest request,
        io.grpc.stub.StreamObserver<io.harness.delegate.TaskProgressUpdatesResponse> responseObserver) {
      asyncServerStreamingCall(
          getChannel().newCall(getTaskProgressUpdatesMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void registerPerpetualTaskClientEntrypoint(
        io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest request,
        io.grpc.stub
            .StreamObserver<io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse> responseObserver) {
      asyncUnaryCall(getChannel().newCall(getRegisterPerpetualTaskClientEntrypointMethod(), getCallOptions()), request,
          responseObserver);
    }

    /**
     */
    public void createPerpetualTask(io.harness.delegate.CreatePerpetualTaskRequest request,
        io.grpc.stub.StreamObserver<io.harness.delegate.CreatePerpetualTaskResponse> responseObserver) {
      asyncUnaryCall(getChannel().newCall(getCreatePerpetualTaskMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void deletePerpetualTask(io.harness.delegate.DeletePerpetualTaskRequest request,
        io.grpc.stub.StreamObserver<io.harness.delegate.DeletePerpetualTaskResponse> responseObserver) {
      asyncUnaryCall(getChannel().newCall(getDeletePerpetualTaskMethod(), getCallOptions()), request, responseObserver);
    }

    /**
     */
    public void resetPerpetualTask(io.harness.delegate.ResetPerpetualTaskRequest request,
        io.grpc.stub.StreamObserver<io.harness.delegate.ResetPerpetualTaskResponse> responseObserver) {
      asyncUnaryCall(getChannel().newCall(getResetPerpetualTaskMethod(), getCallOptions()), request, responseObserver);
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
    public io.harness.delegate.SubmitTaskResponse submitTask(io.harness.delegate.SubmitTaskRequest request) {
      return blockingUnaryCall(getChannel(), getSubmitTaskMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.harness.delegate.CancelTaskResponse cancelTask(io.harness.delegate.CancelTaskRequest request) {
      return blockingUnaryCall(getChannel(), getCancelTaskMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.harness.delegate.TaskProgressResponse taskProgress(io.harness.delegate.TaskProgressRequest request) {
      return blockingUnaryCall(getChannel(), getTaskProgressMethod(), getCallOptions(), request);
    }

    /**
     */
    public java.util.Iterator<io.harness.delegate.TaskProgressUpdatesResponse> taskProgressUpdates(
        io.harness.delegate.TaskProgressUpdatesRequest request) {
      return blockingServerStreamingCall(getChannel(), getTaskProgressUpdatesMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse registerPerpetualTaskClientEntrypoint(
        io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest request) {
      return blockingUnaryCall(
          getChannel(), getRegisterPerpetualTaskClientEntrypointMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.harness.delegate.CreatePerpetualTaskResponse createPerpetualTask(
        io.harness.delegate.CreatePerpetualTaskRequest request) {
      return blockingUnaryCall(getChannel(), getCreatePerpetualTaskMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.harness.delegate.DeletePerpetualTaskResponse deletePerpetualTask(
        io.harness.delegate.DeletePerpetualTaskRequest request) {
      return blockingUnaryCall(getChannel(), getDeletePerpetualTaskMethod(), getCallOptions(), request);
    }

    /**
     */
    public io.harness.delegate.ResetPerpetualTaskResponse resetPerpetualTask(
        io.harness.delegate.ResetPerpetualTaskRequest request) {
      return blockingUnaryCall(getChannel(), getResetPerpetualTaskMethod(), getCallOptions(), request);
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
    public com.google.common.util.concurrent.ListenableFuture<io.harness.delegate.SubmitTaskResponse> submitTask(
        io.harness.delegate.SubmitTaskRequest request) {
      return futureUnaryCall(getChannel().newCall(getSubmitTaskMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.harness.delegate.CancelTaskResponse> cancelTask(
        io.harness.delegate.CancelTaskRequest request) {
      return futureUnaryCall(getChannel().newCall(getCancelTaskMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.harness.delegate.TaskProgressResponse> taskProgress(
        io.harness.delegate.TaskProgressRequest request) {
      return futureUnaryCall(getChannel().newCall(getTaskProgressMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent
        .ListenableFuture<io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse>
        registerPerpetualTaskClientEntrypoint(
            io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest request) {
      return futureUnaryCall(
          getChannel().newCall(getRegisterPerpetualTaskClientEntrypointMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.harness.delegate.CreatePerpetualTaskResponse>
    createPerpetualTask(io.harness.delegate.CreatePerpetualTaskRequest request) {
      return futureUnaryCall(getChannel().newCall(getCreatePerpetualTaskMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.harness.delegate.DeletePerpetualTaskResponse>
    deletePerpetualTask(io.harness.delegate.DeletePerpetualTaskRequest request) {
      return futureUnaryCall(getChannel().newCall(getDeletePerpetualTaskMethod(), getCallOptions()), request);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.harness.delegate.ResetPerpetualTaskResponse>
    resetPerpetualTask(io.harness.delegate.ResetPerpetualTaskRequest request) {
      return futureUnaryCall(getChannel().newCall(getResetPerpetualTaskMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_SUBMIT_TASK = 0;
  private static final int METHODID_CANCEL_TASK = 1;
  private static final int METHODID_TASK_PROGRESS = 2;
  private static final int METHODID_TASK_PROGRESS_UPDATES = 3;
  private static final int METHODID_REGISTER_PERPETUAL_TASK_CLIENT_ENTRYPOINT = 4;
  private static final int METHODID_CREATE_PERPETUAL_TASK = 5;
  private static final int METHODID_DELETE_PERPETUAL_TASK = 6;
  private static final int METHODID_RESET_PERPETUAL_TASK = 7;

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
          serviceImpl.submitTask((io.harness.delegate.SubmitTaskRequest) request,
              (io.grpc.stub.StreamObserver<io.harness.delegate.SubmitTaskResponse>) responseObserver);
          break;
        case METHODID_CANCEL_TASK:
          serviceImpl.cancelTask((io.harness.delegate.CancelTaskRequest) request,
              (io.grpc.stub.StreamObserver<io.harness.delegate.CancelTaskResponse>) responseObserver);
          break;
        case METHODID_TASK_PROGRESS:
          serviceImpl.taskProgress((io.harness.delegate.TaskProgressRequest) request,
              (io.grpc.stub.StreamObserver<io.harness.delegate.TaskProgressResponse>) responseObserver);
          break;
        case METHODID_TASK_PROGRESS_UPDATES:
          serviceImpl.taskProgressUpdates((io.harness.delegate.TaskProgressUpdatesRequest) request,
              (io.grpc.stub.StreamObserver<io.harness.delegate.TaskProgressUpdatesResponse>) responseObserver);
          break;
        case METHODID_REGISTER_PERPETUAL_TASK_CLIENT_ENTRYPOINT:
          serviceImpl.registerPerpetualTaskClientEntrypoint(
              (io.harness.delegate.RegisterPerpetualTaskClientEntrypointRequest) request,
              (io.grpc.stub.StreamObserver<io.harness.delegate.RegisterPerpetualTaskClientEntrypointResponse>)
                  responseObserver);
          break;
        case METHODID_CREATE_PERPETUAL_TASK:
          serviceImpl.createPerpetualTask((io.harness.delegate.CreatePerpetualTaskRequest) request,
              (io.grpc.stub.StreamObserver<io.harness.delegate.CreatePerpetualTaskResponse>) responseObserver);
          break;
        case METHODID_DELETE_PERPETUAL_TASK:
          serviceImpl.deletePerpetualTask((io.harness.delegate.DeletePerpetualTaskRequest) request,
              (io.grpc.stub.StreamObserver<io.harness.delegate.DeletePerpetualTaskResponse>) responseObserver);
          break;
        case METHODID_RESET_PERPETUAL_TASK:
          serviceImpl.resetPerpetualTask((io.harness.delegate.ResetPerpetualTaskRequest) request,
              (io.grpc.stub.StreamObserver<io.harness.delegate.ResetPerpetualTaskResponse>) responseObserver);
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
      return io.harness.delegate.DelegateServiceOuterClass.getDescriptor();
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
                                           .addMethod(getTaskProgressUpdatesMethod())
                                           .addMethod(getRegisterPerpetualTaskClientEntrypointMethod())
                                           .addMethod(getCreatePerpetualTaskMethod())
                                           .addMethod(getDeletePerpetualTaskMethod())
                                           .addMethod(getResetPerpetualTaskMethod())
                                           .build();
        }
      }
    }
    return result;
  }
}
