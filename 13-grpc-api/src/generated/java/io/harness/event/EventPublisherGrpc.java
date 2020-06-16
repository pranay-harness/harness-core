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
@javax.annotation.
Generated(value = "by gRPC proto compiler", comments = "Source: io/harness/event/event_publisher.proto")
public final class EventPublisherGrpc {
  private EventPublisherGrpc() {}

  public static final String SERVICE_NAME = "io.harness.event.EventPublisher";

  // Static method descriptors that strictly reflect the proto.
  private static volatile io.grpc
      .MethodDescriptor<io.harness.event.PublishRequest, io.harness.event.PublishResponse> getPublishMethod;

  @io.grpc.stub.annotations
      .RpcMethod(fullMethodName = SERVICE_NAME + '/' + "Publish", requestType = io.harness.event.PublishRequest.class,
          responseType = io.harness.event.PublishResponse.class, methodType = io.grpc.MethodDescriptor.MethodType.UNARY)
      public static io.grpc.MethodDescriptor<io.harness.event.PublishRequest, io.harness.event.PublishResponse>
      getPublishMethod() {
    io.grpc.MethodDescriptor<io.harness.event.PublishRequest, io.harness.event.PublishResponse> getPublishMethod;
    if ((getPublishMethod = EventPublisherGrpc.getPublishMethod) == null) {
      synchronized (EventPublisherGrpc.class) {
        if ((getPublishMethod = EventPublisherGrpc.getPublishMethod) == null) {
          EventPublisherGrpc.getPublishMethod = getPublishMethod =
              io.grpc.MethodDescriptor.<io.harness.event.PublishRequest, io.harness.event.PublishResponse>newBuilder()
                  .setType(io.grpc.MethodDescriptor.MethodType.UNARY)
                  .setFullMethodName(generateFullMethodName(SERVICE_NAME, "Publish"))
                  .setSampledToLocalTracing(true)
                  .setRequestMarshaller(
                      io.grpc.protobuf.ProtoUtils.marshaller(io.harness.event.PublishRequest.getDefaultInstance()))
                  .setResponseMarshaller(
                      io.grpc.protobuf.ProtoUtils.marshaller(io.harness.event.PublishResponse.getDefaultInstance()))
                  .setSchemaDescriptor(new EventPublisherMethodDescriptorSupplier("Publish"))
                  .build();
        }
      }
    }
    return getPublishMethod;
  }

  /**
   * Creates a new async stub that supports all call types for the service
   */
  public static EventPublisherStub newStub(io.grpc.Channel channel) {
    return new EventPublisherStub(channel);
  }

  /**
   * Creates a new blocking-style stub that supports unary and streaming output calls on the service
   */
  public static EventPublisherBlockingStub newBlockingStub(io.grpc.Channel channel) {
    return new EventPublisherBlockingStub(channel);
  }

  /**
   * Creates a new ListenableFuture-style stub that supports unary calls on the service
   */
  public static EventPublisherFutureStub newFutureStub(io.grpc.Channel channel) {
    return new EventPublisherFutureStub(channel);
  }

  /**
   */
  public static abstract class EventPublisherImplBase implements io.grpc.BindableService {
    /**
     */
    public void publish(io.harness.event.PublishRequest request,
        io.grpc.stub.StreamObserver<io.harness.event.PublishResponse> responseObserver) {
      asyncUnimplementedUnaryCall(getPublishMethod(), responseObserver);
    }

    @java.
    lang.Override
    public final io.grpc.ServerServiceDefinition bindService() {
      return io.grpc.ServerServiceDefinition.builder(getServiceDescriptor())
          .addMethod(getPublishMethod(),
              asyncUnaryCall(new MethodHandlers<io.harness.event.PublishRequest, io.harness.event.PublishResponse>(
                  this, METHODID_PUBLISH)))
          .build();
    }
  }

  /**
   */
  public static final class EventPublisherStub extends io.grpc.stub.AbstractStub<EventPublisherStub> {
    private EventPublisherStub(io.grpc.Channel channel) {
      super(channel);
    }

    private EventPublisherStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected EventPublisherStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new EventPublisherStub(channel, callOptions);
    }

    /**
     */
    public void publish(io.harness.event.PublishRequest request,
        io.grpc.stub.StreamObserver<io.harness.event.PublishResponse> responseObserver) {
      asyncUnaryCall(getChannel().newCall(getPublishMethod(), getCallOptions()), request, responseObserver);
    }
  }

  /**
   */
  public static final class EventPublisherBlockingStub extends io.grpc.stub.AbstractStub<EventPublisherBlockingStub> {
    private EventPublisherBlockingStub(io.grpc.Channel channel) {
      super(channel);
    }

    private EventPublisherBlockingStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected EventPublisherBlockingStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new EventPublisherBlockingStub(channel, callOptions);
    }

    /**
     */
    public io.harness.event.PublishResponse publish(io.harness.event.PublishRequest request) {
      return blockingUnaryCall(getChannel(), getPublishMethod(), getCallOptions(), request);
    }
  }

  /**
   */
  public static final class EventPublisherFutureStub extends io.grpc.stub.AbstractStub<EventPublisherFutureStub> {
    private EventPublisherFutureStub(io.grpc.Channel channel) {
      super(channel);
    }

    private EventPublisherFutureStub(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      super(channel, callOptions);
    }

    @java.lang.Override
    protected EventPublisherFutureStub build(io.grpc.Channel channel, io.grpc.CallOptions callOptions) {
      return new EventPublisherFutureStub(channel, callOptions);
    }

    /**
     */
    public com.google.common.util.concurrent.ListenableFuture<io.harness.event.PublishResponse> publish(
        io.harness.event.PublishRequest request) {
      return futureUnaryCall(getChannel().newCall(getPublishMethod(), getCallOptions()), request);
    }
  }

  private static final int METHODID_PUBLISH = 0;

  private static final class MethodHandlers<Req, Resp>
      implements io.grpc.stub.ServerCalls.UnaryMethod<Req, Resp>,
                 io.grpc.stub.ServerCalls.ServerStreamingMethod<Req, Resp>,
                 io.grpc.stub.ServerCalls.ClientStreamingMethod<Req, Resp>,
                 io.grpc.stub.ServerCalls.BidiStreamingMethod<Req, Resp> {
    private final EventPublisherImplBase serviceImpl;
    private final int methodId;

    MethodHandlers(EventPublisherImplBase serviceImpl, int methodId) {
      this.serviceImpl = serviceImpl;
      this.methodId = methodId;
    }

    @java.
    lang.Override
    @java.lang.SuppressWarnings("unchecked")
    public void invoke(Req request, io.grpc.stub.StreamObserver<Resp> responseObserver) {
      switch (methodId) {
        case METHODID_PUBLISH:
          serviceImpl.publish((io.harness.event.PublishRequest) request,
              (io.grpc.stub.StreamObserver<io.harness.event.PublishResponse>) responseObserver);
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

  private static abstract class EventPublisherBaseDescriptorSupplier
      implements io.grpc.protobuf.ProtoFileDescriptorSupplier, io.grpc.protobuf.ProtoServiceDescriptorSupplier {
    EventPublisherBaseDescriptorSupplier() {}

    @java.
    lang.Override
    public com.google.protobuf.Descriptors.FileDescriptor getFileDescriptor() {
      return io.harness.event.EventPublisherOuterClass.getDescriptor();
    }

    @java.
    lang.Override
    public com.google.protobuf.Descriptors.ServiceDescriptor getServiceDescriptor() {
      return getFileDescriptor().findServiceByName("EventPublisher");
    }
  }

  private static final class EventPublisherFileDescriptorSupplier extends EventPublisherBaseDescriptorSupplier {
    EventPublisherFileDescriptorSupplier() {}
  }

  private static final class EventPublisherMethodDescriptorSupplier
      extends EventPublisherBaseDescriptorSupplier implements io.grpc.protobuf.ProtoMethodDescriptorSupplier {
    private final String methodName;

    EventPublisherMethodDescriptorSupplier(String methodName) {
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
      synchronized (EventPublisherGrpc.class) {
        result = serviceDescriptor;
        if (result == null) {
          serviceDescriptor = result = io.grpc.ServiceDescriptor.newBuilder(SERVICE_NAME)
                                           .setSchemaDescriptor(new EventPublisherFileDescriptorSupplier())
                                           .addMethod(getPublishMethod())
                                           .build();
        }
      }
    }
    return result;
  }
}
