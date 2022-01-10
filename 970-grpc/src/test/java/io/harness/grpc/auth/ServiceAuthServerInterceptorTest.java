/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.grpc.auth;

import static io.harness.grpc.utils.GrpcAuthUtils.IS_AUTHENTICATED_CONTEXT_KEY;
import static io.harness.rule.OwnerRule.ROHIT_KUMAR;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.event.EventPublisherGrpc;
import io.harness.event.PublishMessage;
import io.harness.event.PublishRequest;
import io.harness.event.client.FakeService;
import io.harness.grpc.auth.DelegateAuthServerInterceptorTest.ContextRecordingInterceptor;
import io.harness.grpc.utils.GrpcAuthUtils;
import io.harness.rule.Owner;
import io.harness.security.ServiceTokenGenerator;

import com.google.common.collect.ImmutableMap;
import io.grpc.Channel;
import io.grpc.Metadata;
import io.grpc.StatusRuntimeException;
import io.grpc.health.v1.HealthCheckRequest;
import io.grpc.health.v1.HealthGrpc;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.services.HealthStatusManager;
import io.grpc.stub.MetadataUtils;
import io.grpc.testing.GrpcCleanupRule;
import lombok.val;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class ServiceAuthServerInterceptorTest extends CategoryTest {
  private ContextRecordingInterceptor contextRecordingInterceptor;
  private Channel channel;
  @Rule public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();
  private FakeService fakeService;

  @Before
  public void setUp() throws Exception {
    fakeService = new FakeService();
    contextRecordingInterceptor = new ContextRecordingInterceptor();
    String serverName = InProcessServerBuilder.generateName();
    grpcCleanup.register(InProcessServerBuilder.forName(serverName)
                             .directExecutor()
                             .addService(new HealthStatusManager().getHealthService())
                             .addService(fakeService)
                             .intercept(contextRecordingInterceptor)
                             .intercept(new ServiceAuthServerInterceptor(
                                 ImmutableMap.<String, ServiceInfo>builder()
                                     .put(EventPublisherGrpc.SERVICE_NAME,
                                         ServiceInfo.builder().id("manager").secret("managersecret").build())
                                     .build()))
                             .build()
                             .start());
    channel = grpcCleanup.register(InProcessChannelBuilder.forName(serverName).build());
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void shouldPassThroughIfExcludedService() throws Exception {
    val healthStub = HealthGrpc.newBlockingStub(channel);
    assertThatCode(() -> healthStub.check(HealthCheckRequest.newBuilder().build())).doesNotThrowAnyException();
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void shouldBlockIfNoTokenInMetadata() throws Exception {
    val metadata = new Metadata();
    GrpcAuthUtils.setServiceAuthDetailsInRequest("manager", "", metadata);
    val eventSvcStub = EventPublisherGrpc.newBlockingStub(channel).withInterceptors(
        MetadataUtils.newAttachHeadersInterceptor(metadata));
    assertThatExceptionOfType(StatusRuntimeException.class)
        .isThrownBy(() -> eventSvcStub.publish(PublishRequest.newBuilder().build()))
        .withMessage("UNAUTHENTICATED: Token missing");
    assertThat(fakeService.getMessageCount()).isEqualTo(0);
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void shouldBlockIfNoServiceIdInMetadata() throws Exception {
    val metadata = new Metadata();
    GrpcAuthUtils.setServiceAuthDetailsInRequest("", "token", metadata);
    val eventSvcStub = EventPublisherGrpc.newBlockingStub(channel).withInterceptors(
        MetadataUtils.newAttachHeadersInterceptor(metadata));
    assertThatExceptionOfType(StatusRuntimeException.class)
        .isThrownBy(() -> eventSvcStub.publish(PublishRequest.newBuilder().build()))
        .withMessage("UNAUTHENTICATED: serviceId missing");
    assertThat(fakeService.getMessageCount()).isEqualTo(0);
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void shouldBlockIfUnknownServiceIdInMetadata() throws Exception {
    val metadata = new Metadata();
    GrpcAuthUtils.setServiceAuthDetailsInRequest("unknown", "token", metadata);
    val eventSvcStub = EventPublisherGrpc.newBlockingStub(channel).withInterceptors(
        MetadataUtils.newAttachHeadersInterceptor(metadata));
    assertThatExceptionOfType(StatusRuntimeException.class)
        .isThrownBy(() -> eventSvcStub.publish(PublishRequest.newBuilder().build()))
        .withMessage("UNAUTHENTICATED: unsupported serviceId =unknown");
    assertThat(fakeService.getMessageCount()).isEqualTo(0);
  }
  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void shouldBlockIfTokenValidationFails() throws Exception {
    val metadata = new Metadata();
    GrpcAuthUtils.setServiceAuthDetailsInRequest("manager", "token", metadata);
    val eventSvcStub = EventPublisherGrpc.newBlockingStub(channel).withInterceptors(
        MetadataUtils.newAttachHeadersInterceptor(metadata));
    assertThatExceptionOfType(StatusRuntimeException.class)
        .isThrownBy(() -> eventSvcStub.publish(PublishRequest.newBuilder().build()))
        .withMessageContaining("invalid token");
    assertThat(fakeService.getMessageCount()).isEqualTo(0);
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void shouldPassValidation() throws Exception {
    final String token = ServiceTokenGenerator.newInstance().getServiceToken("managersecret");
    val metadata = new Metadata();
    GrpcAuthUtils.setServiceAuthDetailsInRequest("manager", token, metadata);
    val eventSvcStub = EventPublisherGrpc.newBlockingStub(channel).withInterceptors(
        MetadataUtils.newAttachHeadersInterceptor(metadata));
    eventSvcStub.publish(PublishRequest.newBuilder().addMessages(PublishMessage.newBuilder()).build());

    assertThat(IS_AUTHENTICATED_CONTEXT_KEY.get(contextRecordingInterceptor.lastContext)).isTrue();
    assertThat(fakeService.getMessageCount()).isEqualTo(1);
  }
}
