package io.harness.pms.sdk.core.recast;

import static io.harness.rule.OwnerRule.ALEXEI;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.ambiance.Level;
import io.harness.pms.contracts.execution.AsyncExecutableResponse;
import io.harness.pms.contracts.execution.ExecutableResponse;
import io.harness.pms.contracts.execution.ExecutionErrorInfo;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.execution.Status;
import io.harness.pms.sdk.core.PmsSdkCoreTestBase;
import io.harness.pms.serializer.recaster.RecastOrchestrationUtils;
import io.harness.rule.Owner;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Timestamp;
import com.google.protobuf.util.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.bson.Document;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class RecastTest extends PmsSdkCoreTestBase {
  private static final String RECAST_KEY = "__recast";
  private static final String ENCODED_PROTO = "__encodedProto";

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecastWithProtoAsAField() throws InvalidProtocolBufferException {
    ExecutionErrorInfo executionErrorInfo = ExecutionErrorInfo.newBuilder().setMessage("some-message").build();
    ProtoAsAFieldClass protoAsAFieldClass = ProtoAsAFieldClass.builder().executionErrorInfo(executionErrorInfo).build();

    Document expectedDocument = new Document()
                                    .append(RECAST_KEY, ProtoAsAFieldClass.class.getName())
                                    .append("executionErrorInfo", JsonFormat.printer().print(executionErrorInfo));

    Document document = RecastOrchestrationUtils.toDocument(protoAsAFieldClass);
    assertThat(document).isNotNull();
    assertThat(document).isEqualTo(expectedDocument);

    ProtoAsAFieldClass recastedClass = RecastOrchestrationUtils.fromDocument(document, ProtoAsAFieldClass.class);
    assertThat(recastedClass).isEqualTo(protoAsAFieldClass);
  }

  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  @EqualsAndHashCode
  private static class ProtoAsAFieldClass {
    private ExecutionErrorInfo executionErrorInfo;
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecastWithProtoExecutionErrorInfo() throws InvalidProtocolBufferException {
    ExecutionErrorInfo executionErrorInfo = ExecutionErrorInfo.newBuilder().setMessage("some-message").build();

    Document expectedDocument = new Document()
                                    .append(RECAST_KEY, ExecutionErrorInfo.class.getName())
                                    .append(ENCODED_PROTO, JsonFormat.printer().print(executionErrorInfo));

    Document document = RecastOrchestrationUtils.toDocument(executionErrorInfo);
    assertThat(document).isNotNull();
    assertThat(document).isEqualTo(expectedDocument);

    ExecutionErrorInfo recastedClass = RecastOrchestrationUtils.fromDocument(document, ExecutionErrorInfo.class);
    assertThat(recastedClass).isEqualTo(executionErrorInfo);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecastWithProtoNodeExecution() throws InvalidProtocolBufferException {
    NodeExecutionProto nodeExecutionProto =
        NodeExecutionProto.newBuilder()
            .setAmbiance(
                Ambiance.newBuilder()
                    .addLevels(Level.newBuilder().setRuntimeId("runtimeId").setIdentifier("identifier").build())
                    .setPlanExecutionId("planExecutionId")
                    .build())
            .setStartTs(Timestamp.newBuilder().setSeconds(15).build())
            .setEndTs(Timestamp.newBuilder().setSeconds(20).build())
            .setStatus(Status.SUCCEEDED)
            .addExecutableResponses(
                ExecutableResponse.newBuilder()
                    .setAsync(AsyncExecutableResponse.newBuilder().addCallbackIds("callbackId").build())
                    .setMetadata("metadata")
                    .build())
            .build();

    Document expectedDocument = new Document()
                                    .append(RECAST_KEY, NodeExecutionProto.class.getName())
                                    .append(ENCODED_PROTO, JsonFormat.printer().print(nodeExecutionProto));

    Document document = RecastOrchestrationUtils.toDocument(nodeExecutionProto);
    assertThat(document).isNotNull();
    assertThat(document).isEqualTo(expectedDocument);

    NodeExecutionProto recastedClass = RecastOrchestrationUtils.fromDocument(document, NodeExecutionProto.class);
    assertThat(recastedClass).isEqualTo(nodeExecutionProto);
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(UnitTests.class)
  public void shouldTestRecastWithProtoNodeExecutionFromOnly() throws InvalidProtocolBufferException {
    NodeExecutionProto nodeExecutionProto =
        NodeExecutionProto.newBuilder()
            .setAmbiance(
                Ambiance.newBuilder()
                    .addLevels(Level.newBuilder().setRuntimeId("runtimeId").setIdentifier("identifier").build())
                    .setPlanExecutionId("planExecutionId")
                    .build())
            .setStartTs(Timestamp.newBuilder().setSeconds(15).build())
            .setEndTs(Timestamp.newBuilder().setSeconds(20).build())
            .setStatus(Status.SUCCEEDED)
            .addExecutableResponses(
                ExecutableResponse.newBuilder()
                    .setAsync(AsyncExecutableResponse.newBuilder().addCallbackIds("callbackId").build())
                    .setMetadata("metadata")
                    .build())
            .build();

    Document document = new Document()
                            .append(RECAST_KEY, NodeExecutionProto.class.getName())
                            .append(ENCODED_PROTO, JsonFormat.printer().print(nodeExecutionProto));

    NodeExecutionProto recastedClass = RecastOrchestrationUtils.fromDocument(document, NodeExecutionProto.class);
    assertThat(recastedClass).isEqualTo(nodeExecutionProto);
  }
}
