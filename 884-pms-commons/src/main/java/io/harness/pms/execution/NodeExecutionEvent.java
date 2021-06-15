package io.harness.pms.execution;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.logging.AutoLogContext.OverrideBehavior.OVERRIDE_NESTS;

import io.harness.annotation.HarnessEntity;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.logging.AutoLogContext;
import io.harness.metrics.ThreadAutoLogContext;
import io.harness.pms.contracts.execution.NodeExecutionProto;
import io.harness.pms.contracts.plan.NodeExecutionEventType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.queue.Queuable;
import io.harness.queue.WithMonitoring;

import java.util.HashMap;
import java.util.Map;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import lombok.experimental.NonFinal;
import org.mongodb.morphia.annotations.Entity;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.TypeAlias;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

@OwnedBy(HarnessTeam.CDC)
@Value
@Builder
@EqualsAndHashCode(callSuper = false)
@FieldNameConstants(innerTypeName = "NodeExecutionEventKeys")
@Entity(value = "nodeExecutionEventQueue", noClassnameStored = true)
@Document("nodeExecutionEventQueue")
@TypeAlias("nodeExecutionEvent")
@HarnessEntity(exportable = false)
public class NodeExecutionEvent extends Queuable implements WithMonitoring {
  NodeExecutionProto nodeExecution;
  NodeExecutionEventType eventType;
  NodeExecutionEventData eventData;
  @Getter @Setter @NonFinal @CreatedDate Long createdAt;
  @Builder.Default String notifyId = generateUuid();
  @Setter @NonFinal @Version Long version;
  @Getter @Setter @NonFinal boolean monitoringEnabled;

  public AutoLogContext autoLogContext() {
    return new AutoLogContext(logContextMap(), OVERRIDE_NESTS);
  }

  public ThreadAutoLogContext metricContext() {
    Map<String, String> logContext = new HashMap<>();
    logContext.putAll(AmbianceUtils.logContextMap(nodeExecution.getAmbiance()));
    logContext.put("module", nodeExecution.getNode().getServiceName());
    logContext.put("pipelineIdentifier", nodeExecution.getAmbiance().getMetadata().getPipelineIdentifier());
    return new ThreadAutoLogContext(logContext, OVERRIDE_NESTS);
  }

  @Override
  public String getMetricPrefix() {
    return "node_execution_" + eventType.name().toLowerCase();
  }

  public Map<String, String> logContextMap() {
    Map<String, String> logContext = new HashMap<>();
    logContext.put(NodeExecutionEventKeys.eventType, eventType.name());
    logContext.putAll(AmbianceUtils.logContextMap(nodeExecution.getAmbiance()));
    logContext.put(NodeExecutionEventKeys.notifyId, notifyId);
    return logContext;
  }
}
