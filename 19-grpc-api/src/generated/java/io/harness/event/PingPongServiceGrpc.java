package io.harness.event;

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
    value = "by gRPC proto compiler (version 1.21.0)", comments = "Source: io/harness/pingpong/ping_pong_service.proto")
public final class PingPongServiceGrpc {
  private PingPongServiceGrpc() {}

  public static final String SERVICE_NAME = "io.harness.event.PingPongService";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc.MethodDescriptor<io.harness.event.Ping, io.harness.event.Pong> getTryPingMethod;

  @io.grpc.stub.annotations
      .RpcMethod(fullMethodName = SERVICE_NAME + '/' + "TryPing", requestType = io.harness.event.Ping.class,
          responseType = io.harness.event.Pong.class, methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
      public static io.grpc.MethodDescriptor<io.harness.event.Ping, io.harness.event.Pong>
      getTryPingMethod() {
    io.grpc.MethodDescriptor<io.harness.event.Ping, io.harness.event.Pong> getTryPingMethod;
    if ((getTryPingMethod = PingPongServiceGrpc.getTryPingMethod) == null) {
      synchronized (PingPongServiceGrpc.class) {
        if ((getTryPingMethod = PingPongServiceGrpc.getTryPingMethod) == null) {
          PingPongServiceGrpc.getTryPingMethod = getTryPingMethod =
              io.grpc.MethodDescriptor.<io.harness.event.Ping, io.harness.event.Pong>newBuilder()
                  .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                  .setFullMethodName(generateFullMethodName("io.harness.event.PingPongService", "TryPing"))
                  .setSampledToLocalTracing(true)
                  .setRequestMarshaller(
                      io.grpc.protobuf.ProtoUtils.marshaller(io.harness.event.Ping.getDefaultInstance()))
                  .setResponseMarshaller(
                      io.grpc.protobuf.ProtoUtils.marshaller(io.harness.event.Pong.getDefaultInstance()))
                  .setSchemaDescriptor(new PingPongServiceMethodDescriptorSupplier("TryPing"))
                  .build();
        }
      }
    }
    return getTryPingMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static PingPongServiceStub newStub(io.grpc.Channel channel) {
    return new PingPongServiceStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static PingPongServiceBlockingStub newBlockingStub(io.grpc.Channel channel) {
    return new PingPongServiceBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static PingPongServiceFutureStub newFutureStub(io.grpc.Channel channel) {
    return new PingPongServiceFutureStub(channel);
  }

  /**
   */
  public static abstract class PingPongServiceImplBase implements io.grpc.BindableService {
    /**
     */
    public void tryPing(
        io.harness.event.Ping request, io.grpc.stub.StreamObserver<io.harness.event.Pong> responseObserver) {
      asyncUnimplementedUnaryCall(getTryPingMethod(), responseObserver);
    }

    @java.
    lang.Override
    public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(getTryPingMethod(),
              asyncUnaryCall(new MethodHandlers<io.harness.event.Ping, io.harness.event.Pong>(this, METHODID_TRY_PING)))
          .build();
    }
  }

  /**
   */
  public static final class PingPongServiceStub extends io.grpc.stub.AbstractStub<PingPongServiceStub> {
    private PingPongServiceStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PingPongServiceStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PingPongServiceStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PingPongServiceStub(channel, callOptions);
    }

    /**
     */
    public void tryPing(
        io.harness.event.Ping request, io.grpc.stub.StreamObserver<io.harness.event.Pong> responseObserver) {
      asyncUnaryCall(getChannel().newCall(getTryPingMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class PingPongServiceBlockingStub extends io.grpc.stub.AbstractStub<PingPongServiceBlockingStub> {
    private PingPongServiceBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PingPongServiceBlockingStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PingPongServiceBlockingStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PingPongServiceBlockingStub(channel, callOptions);
    }

    /**
     */
    public io.harness.event.Pong tryPing(io.harness.event.Ping request) {
      return blockingUnaryCall(getChannel(), getTryPingMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class PingPongServiceFutureStub extends io.grpc.stub.AbstractStub<PingPongServiceFutureStub> {
    private PingPongServiceFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private PingPongServiceFutureStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected PingPongServiceFutureStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new PingPongServiceFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.harness.event.Pong> tryPing(
        io.harness.event.Ping request) {
      return futureUnaryCall(getChannel().newCall(getTryPingMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_TRY_PING = 0;

  private static final class MethodHandlers<Req, Resp>
      implements io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
                 io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
                 io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
                 io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final PingPongServiceImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(PingPongServiceImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.
    lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_TRY_PING:
          serviceImpl.tryPing(
              (io.harness.event.Ping) request, (io.grpc.stub.StreamObserver<io.harness.event.Pong>) responseObserver);
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

  private static abstract class PingPongServiceBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    PingPongServiceBaseDescriptorSupplier() {}

    @java.
    lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.harness.event.PingPongServiceOuterClass.getDescriptor();
    }

    @java.
    lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("PingPongService");
    }
  }

  private static final class PingPongServiceFileDescriptorSupplier extends PingPongServiceBaseDescriptorSupplier {
    PingPongServiceFileDescriptorSupplier() {}
  }

  private static final class PingPongServiceMethodDescriptorSupplier
      extends PingPongServiceBaseDescriptorSupplier implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    PingPongServiceMethodDescriptorSupplier(String methodName) {
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
      synchronized (PingPongServiceGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
                                           .setSchemaDescriptor(new PingPongServiceFileDescriptorSupplier())
                                           .addMethod(getTryPingMethod())
                                           .build();
        }
      }
    }
    return result;
  }
}
