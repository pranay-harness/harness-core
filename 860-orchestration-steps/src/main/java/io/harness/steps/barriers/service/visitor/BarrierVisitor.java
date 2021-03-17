package io.harness.steps.barriers.service.visitor;

import io.harness.exception.InvalidRequestException;
import io.harness.pms.yaml.YamlField;
import io.harness.pms.yaml.YamlNode;
import io.harness.steps.barriers.beans.BarrierSetupInfo;
import io.harness.steps.barriers.beans.StageDetail;
import io.harness.walktree.beans.VisitElementResult;
import io.harness.walktree.visitor.DummyVisitableElement;
import io.harness.walktree.visitor.SimpleVisitor;
import io.harness.yaml.core.timeout.Timeout;

import com.google.api.client.util.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Singleton
public class BarrierVisitor extends SimpleVisitor<DummyVisitableElement> {
  private static final String BARRIER_TYPE = "Barrier";

  private static final String FLOW_CONTROL_FIELD = "flowControl";
  private static final String STAGE_FIELD = "stage";
  private static final String BARRIERS_FIELD = "barriers";
  private static final String SPEC_FIELD = "spec";
  private static final String BARRIER_REF_FIELD = "barrierRef";

  private final Map<String, BarrierSetupInfo> barrierIdentifierMap;

  // state variable
  private String stageName; // this field will be changed each time we encounter a stage
  private String stageIdentifier; // this field will be changed each time we encounter a stage

  @Inject
  public BarrierVisitor(Injector injector) {
    super(injector);
    this.barrierIdentifierMap = new HashMap<>();
    this.stageName = null;
    this.stageIdentifier = null;
  }

  @Override
  public VisitElementResult visitElement(Object currentElement) {
    YamlNode element = (YamlNode) currentElement;

    addMetadataIfFlowControlNode(element);
    addMetadataIfStageNode(element);
    addMetadataIfBarrierStep(element);

    return VisitElementResult.CONTINUE;
  }

  /**
   * If condition is met, populates <code>barrierIdentifierMap</code> from flowControl field
   * @param element current yaml node
   */
  private void addMetadataIfFlowControlNode(YamlNode element) {
    // let's instantiate the map first from flowControl field in yaml
    YamlField flowControlField = element.nextSiblingNodeFromParentObject(FLOW_CONTROL_FIELD);
    if (flowControlField != null) {
      YamlField barriersField = flowControlField.getNode().getField(BARRIERS_FIELD);
      if (barriersField != null) {
        List<YamlNode> barriers = barriersField.getNode().asArray();
        for (YamlNode barrier : barriers) {
          String identifier = barrier.getIdentifier();
          String name = barrier.getName();
          barrierIdentifierMap.putIfAbsent(
              identifier, BarrierSetupInfo.builder().identifier(identifier).name(name).stages(new HashSet<>()).build());
        }
      }
    }
  }

  /**
   * If condition is met, populates the <code>stageName</code> field
   * @param element current yaml node
   */
  private void addMetadataIfStageNode(YamlNode element) {
    // let's obtain the current stage name by checking if the parent is a stage node
    if (element.nextSiblingNodeFromParentObject(STAGE_FIELD) != null) {
      stageName = element.getName();
      stageIdentifier = element.getIdentifier();
    }
  }

  /**
   * If condition is met, populates <code>barrierIdentifierMap</code> with stage in which barrier was found
   * @param element current yaml node
   */
  private void addMetadataIfBarrierStep(YamlNode element) {
    String type = element.getType();
    if (BARRIER_TYPE.equals(type)) {
      String identifier = obtainBarrierIdentifierFromStep(element);
      if (!barrierIdentifierMap.containsKey(identifier)) {
        throw new InvalidRequestException(
            String.format("Barrier Identifier %s was not present in flowControl", identifier));
      }
      barrierIdentifierMap.get(identifier)
          .getStages()
          .add(StageDetail.builder()
                   .name(Preconditions.checkNotNull(stageName, "Stage name should not be null"))
                   .identifier(Preconditions.checkNotNull(stageIdentifier, "Stage identifier should not be null"))
                   .build());
      barrierIdentifierMap.get(identifier).setTimeout(obtainBarrierTimeoutFromStep(element));
    }
  }

  private String obtainBarrierIdentifierFromStep(YamlNode currentElement) {
    return Preconditions.checkNotNull(currentElement.getField(SPEC_FIELD).getNode().getStringValue(BARRIER_REF_FIELD),
        String.format(BARRIER_REF_FIELD + " cannot be null -> %s", currentElement.asText()));
  }

  private Long obtainBarrierTimeoutFromStep(YamlNode currentElement) {
    return Objects.requireNonNull(Timeout.fromString(currentElement.getCurrJsonNode().get("timeout").asText()))
        .getTimeoutInMillis();
  }

  public Map<String, BarrierSetupInfo> getBarrierIdentifierMap() {
    return barrierIdentifierMap;
  }
}
