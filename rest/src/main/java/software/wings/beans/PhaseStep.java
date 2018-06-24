package software.wings.beans;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.GraphLink.Builder.aLink;
import static software.wings.beans.GraphNode.GraphNodeBuilder.aGraphNode;
import static software.wings.beans.PhaseStep.PhaseStepBuilder.aPhaseStep;
import static software.wings.beans.PhaseStepType.CONTAINER_SETUP;
import static software.wings.beans.PhaseStepType.INFRASTRUCTURE_NODE;
import static software.wings.sm.StateType.FORK;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.DeploymentType;
import software.wings.beans.Graph.Builder;
import software.wings.common.Constants;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;
import software.wings.sm.TransitionType;
import software.wings.yaml.BaseYamlWithType;
import software.wings.yaml.workflow.StepYaml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by rishi on 12/21/16.
 */
public class PhaseStep {
  private String uuid = generateUuid();
  private String name;
  private PhaseStepType phaseStepType;
  @JsonIgnore private List<String> stepsIds = new ArrayList<>();
  @Transient private List<GraphNode> steps = new ArrayList<>();
  private boolean stepsInParallel;
  private List<FailureStrategy> failureStrategies = new ArrayList<>();

  private boolean rollback;
  private String phaseStepNameForRollback;
  private ExecutionStatus statusForRollback;
  private boolean artifactNeeded;

  private boolean valid = true;
  private String validationMessage;

  private Integer waitInterval;

  public PhaseStep() {}

  public PhaseStep(PhaseStepType phaseStepType, String name) {
    this.phaseStepType = phaseStepType;
    this.uuid = generateUuid();
    this.name = name;
  }

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public PhaseStepType getPhaseStepType() {
    return phaseStepType;
  }

  public void setPhaseStepType(PhaseStepType phaseStepType) {
    this.phaseStepType = phaseStepType;
  }

  public List<GraphNode> getSteps() {
    return steps;
  }

  public void setSteps(List<GraphNode> steps) {
    this.steps = steps;
  }

  public boolean isStepsInParallel() {
    return stepsInParallel;
  }

  public void setStepsInParallel(boolean stepsInParallel) {
    this.stepsInParallel = stepsInParallel;
  }

  public List<FailureStrategy> getFailureStrategies() {
    return failureStrategies;
  }

  public void setFailureStrategies(List<FailureStrategy> failureStrategies) {
    this.failureStrategies = failureStrategies;
  }

  public List<String> getStepsIds() {
    return stepsIds;
  }

  public void setStepsIds(List<String> stepsIds) {
    this.stepsIds = stepsIds;
  }

  public boolean isRollback() {
    return rollback;
  }

  public void setRollback(boolean rollback) {
    this.rollback = rollback;
  }

  public String getPhaseStepNameForRollback() {
    return phaseStepNameForRollback;
  }

  public void setPhaseStepNameForRollback(String phaseStepNameForRollback) {
    this.phaseStepNameForRollback = phaseStepNameForRollback;
  }

  public boolean isValid() {
    return valid;
  }

  public void setValid(boolean valid) {
    this.valid = valid;
  }

  public String getValidationMessage() {
    return validationMessage;
  }

  public void setValidationMessage(String validationMessage) {
    this.validationMessage = validationMessage;
  }

  public ExecutionStatus getStatusForRollback() {
    return statusForRollback;
  }

  public void setStatusForRollback(ExecutionStatus statusForRollback) {
    this.statusForRollback = statusForRollback;
  }

  public Integer getWaitInterval() {
    return waitInterval;
  }

  public void setWaitInterval(Integer waitInterval) {
    this.waitInterval = waitInterval;
  }

  public boolean isArtifactNeeded() {
    return artifactNeeded;
  }

  public void setArtifactNeeded(boolean artifactNeeded) {
    this.artifactNeeded = artifactNeeded;
  }

  public GraphNode generatePhaseStepNode() {
    // TODO: removing failure strategy as part of Node due to mongo driver limitation - we can try with later version
    // CodecConfigurationException: Can't find a codec for class software.wings.beans.FailureStrategy

    return aGraphNode()
        .withId(uuid)
        .withName(getName())
        .withType(StateType.PHASE_STEP.name())
        .withRollback(rollback)
        .addProperty("phaseStepType", phaseStepType)
        .addProperty("stepsInParallel", stepsInParallel)
        .addProperty(Constants.SUB_WORKFLOW_ID, uuid)
        .addProperty("phaseStepNameForRollback", phaseStepNameForRollback)
        .addProperty("statusForRollback", statusForRollback)
        .addProperty("waitInterval", waitInterval)
        .addProperty("artifactNeeded", artifactNeeded)
        .build();
  }

  private static boolean isExecuteWithPreviousSteps(GraphNode step) {
    if (step.getProperties() == null) {
      return false;
    }
    return Boolean.TRUE.equals(step.getProperties().get(Constants.EXECUTE_WITH_PREVIOUS_STEPS));
  }

  public Graph generateSubworkflow(DeploymentType deploymentType) {
    Builder graphBuilder = aGraph().withGraphName(name);
    if (isEmpty(steps)) {
      return graphBuilder.build();
    }
    for (GraphNode step : getSteps()) {
      step.getProperties().put("parentId", getUuid());
    }

    GraphNode originNode = null;

    if (stepsInParallel && steps.size() > 1) {
      GraphNode forkNode = aGraphNode()
                               .withId(generateUuid())
                               .withType(FORK.name())
                               .withName(name + "-FORK")
                               .addProperty("parentId", getUuid())
                               .build();
      graphBuilder.addNodes(forkNode);
      for (GraphNode step : steps) {
        step.setOrigin(false);
        graphBuilder.addNodes(step);
        graphBuilder.addLinks(aLink()
                                  .withId(generateUuid())
                                  .withFrom(forkNode.getId())
                                  .withTo(step.getId())
                                  .withType(TransitionType.FORK.name())
                                  .build());
      }
      if (originNode == null) {
        originNode = forkNode;
      }
    } else {
      GraphNode forkNode = null;
      GraphNode prevNode = null;
      for (int i = 0; i < steps.size(); ++i) {
        GraphNode step = steps.get(i);
        step.setOrigin(false);
        graphBuilder.addNodes(step);
        if (i > 0 && isExecuteWithPreviousSteps(step)) {
          graphBuilder.addLinks(
              aLink().withFrom(forkNode.getId()).withTo(step.getId()).withType(TransitionType.FORK.name()).build());
          continue;
        }
        if (i < steps.size() - 1 && isExecuteWithPreviousSteps(steps.get(i + 1))) {
          forkNode =
              aGraphNode().withId(generateUuid()).withType(FORK.name()).withName("Fork-" + step.getName()).build();
          graphBuilder.addNodes(forkNode);
          graphBuilder.addLinks(
              aLink().withFrom(forkNode.getId()).withTo(step.getId()).withType(TransitionType.FORK.name()).build());
          if (prevNode == null) {
            originNode = forkNode;
          } else {
            graphBuilder.addLinks(aLink()
                                      .withFrom(prevNode.getId())
                                      .withTo(forkNode.getId())
                                      .withType(TransitionType.SUCCESS.name())
                                      .build());
          }
          prevNode = forkNode;
          continue;
        }
        if (prevNode == null) {
          originNode = step;
        } else {
          graphBuilder.addLinks(
              aLink().withFrom(prevNode.getId()).withTo(step.getId()).withType(TransitionType.SUCCESS.name()).build());
        }
        prevNode = step;
      }
    }
    originNode.setOrigin(true);

    return graphBuilder.build();
  }

  public boolean validate() {
    valid = true;
    validationMessage = null;
    if (steps != null) {
      List<String> invalidChildren =
          steps.stream().filter(step -> !step.validate()).map(GraphNode::getName).collect(toList());
      if (isNotEmpty(invalidChildren)) {
        valid = false;
        validationMessage = format(Constants.PHASE_STEP_VALIDATION_MESSAGE, invalidChildren.toString());
      }
    }
    return valid;
  }

  public PhaseStep cloneIntenal() {
    PhaseStepType phaseStepType = getPhaseStepType();
    if (phaseStepType != null && phaseStepType.equals(CONTAINER_SETUP)) {
      return null;
    }
    PhaseStep clonedPhaseStep = aPhaseStep(phaseStepType, getName())
                                    .withPhaseStepNameForRollback(getPhaseStepNameForRollback())
                                    .withPhaseStepType(phaseStepType)
                                    .withRollback(isRollback())
                                    .withFailureStrategies(getFailureStrategies())
                                    .withStatusForRollback(getStatusForRollback())
                                    .withStepsInParallel(isStepsInParallel())
                                    .withArtifactNeeded(isArtifactNeeded())
                                    .withWaitInterval(getWaitInterval())
                                    .build();
    List<GraphNode> steps = getSteps();
    List<String> clonedStepIds = new ArrayList<>();
    List<GraphNode> clonedSteps = new ArrayList<>();
    if (steps != null) {
      for (GraphNode step : steps) {
        GraphNode clonedStep = step.cloneInternal();
        if (INFRASTRUCTURE_NODE.equals(clonedPhaseStep.getPhaseStepType())
            && (clonedStep.getType().equals(StateType.DC_NODE_SELECT.name())
                   || clonedStep.getType().equals(StateType.AWS_NODE_SELECT.name()))) {
          Map<String, Object> properties = new HashMap<>(clonedStep.getProperties());
          if ((Boolean) properties.get("specificHosts")) {
            properties.remove("hostNames");
            clonedStep.setProperties(properties);
          }
        }
        clonedSteps.add(clonedStep);
        clonedStepIds.add(clonedStep.getId());
      }
    }
    clonedPhaseStep.setStepsIds(clonedStepIds);
    clonedPhaseStep.setSteps(clonedSteps);
    return clonedPhaseStep;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    PhaseStep phaseStep = (PhaseStep) o;

    if (stepsInParallel != phaseStep.stepsInParallel) {
      return false;
    }
    if (rollback != phaseStep.rollback) {
      return false;
    }
    if (uuid != null ? !uuid.equals(phaseStep.uuid) : phaseStep.uuid != null) {
      return false;
    }
    if (name != null ? !name.equals(phaseStep.name) : phaseStep.name != null) {
      return false;
    }
    if (phaseStepType != phaseStep.phaseStepType) {
      return false;
    }
    if (stepsIds != null ? !stepsIds.equals(phaseStep.stepsIds) : phaseStep.stepsIds != null) {
      return false;
    }
    if (steps != null ? !steps.equals(phaseStep.steps) : phaseStep.steps != null) {
      return false;
    }
    if (failureStrategies != null ? !failureStrategies.equals(phaseStep.failureStrategies)
                                  : phaseStep.failureStrategies != null) {
      return false;
    }
    return phaseStepNameForRollback != null ? phaseStepNameForRollback.equals(phaseStep.phaseStepNameForRollback)
                                            : phaseStep.phaseStepNameForRollback == null;
  }

  @Override
  public int hashCode() {
    int result = uuid != null ? uuid.hashCode() : 0;
    result = 31 * result + (name != null ? name.hashCode() : 0);
    result = 31 * result + (phaseStepType != null ? phaseStepType.hashCode() : 0);
    result = 31 * result + (stepsIds != null ? stepsIds.hashCode() : 0);
    result = 31 * result + (steps != null ? steps.hashCode() : 0);
    result = 31 * result + (stepsInParallel ? 1 : 0);
    result = 31 * result + (failureStrategies != null ? failureStrategies.hashCode() : 0);
    result = 31 * result + (rollback ? 1 : 0);
    result = 31 * result + (phaseStepNameForRollback != null ? phaseStepNameForRollback.hashCode() : 0);
    return result;
  }

  public static final class PhaseStepBuilder {
    private String uuid = generateUuid();
    private String name;
    private PhaseStepType phaseStepType;
    private List<String> stepsIds = new ArrayList<>();
    private List<GraphNode> steps = new ArrayList<>();
    private boolean stepsInParallel;
    private List<FailureStrategy> failureStrategies = new ArrayList<>();
    private boolean rollback;
    private String phaseStepNameForRollback;
    private ExecutionStatus statusForRollback;
    private boolean artifactNeeded;
    private boolean valid = true;
    private String validationMessage;
    private Integer waitInterval;

    private PhaseStepBuilder() {}

    public static PhaseStepBuilder aPhaseStep(PhaseStepType phaseStepType, String name) {
      PhaseStepBuilder phaseStepBuilder = new PhaseStepBuilder();
      phaseStepBuilder.phaseStepType = phaseStepType;
      phaseStepBuilder.name = name;
      phaseStepBuilder.uuid = generateUuid();
      return phaseStepBuilder;
    }

    public static PhaseStepBuilder aPhaseStep(PhaseStepType phaseStepType, String name, String uuid) {
      PhaseStepBuilder phaseStepBuilder = new PhaseStepBuilder();
      phaseStepBuilder.phaseStepType = phaseStepType;
      phaseStepBuilder.name = name;
      phaseStepBuilder.uuid = uuid;
      return phaseStepBuilder;
    }

    public PhaseStepBuilder withPhaseStepType(PhaseStepType phaseStepType) {
      this.phaseStepType = phaseStepType;
      return this;
    }

    public PhaseStepBuilder addStep(GraphNode step) {
      this.steps.add(step);
      return this;
    }

    public PhaseStepBuilder addAllSteps(List<GraphNode> steps) {
      this.steps.addAll(steps);
      return this;
    }

    public PhaseStepBuilder withStepsInParallel(boolean stepsInParallel) {
      this.stepsInParallel = stepsInParallel;
      return this;
    }

    public PhaseStepBuilder withFailureStrategies(List<FailureStrategy> failureStrategies) {
      this.failureStrategies = failureStrategies;
      return this;
    }

    public PhaseStepBuilder withRollback(boolean rollback) {
      this.rollback = rollback;
      return this;
    }

    public PhaseStepBuilder withPhaseStepNameForRollback(String phaseStepNameForRollback) {
      this.phaseStepNameForRollback = phaseStepNameForRollback;
      return this;
    }

    public PhaseStepBuilder withStatusForRollback(ExecutionStatus statusForRollback) {
      this.statusForRollback = statusForRollback;
      return this;
    }

    public PhaseStepBuilder withValid(boolean valid) {
      this.valid = valid;
      return this;
    }

    public PhaseStepBuilder withValidationMessage(String validationMessage) {
      this.validationMessage = validationMessage;
      return this;
    }

    public PhaseStepBuilder withArtifactNeeded(boolean artifactNeeded) {
      this.artifactNeeded = artifactNeeded;
      return this;
    }

    public PhaseStepBuilder withWaitInterval(Integer waitInterval) {
      this.waitInterval = waitInterval;
      return this;
    }

    public PhaseStep build() {
      PhaseStep phaseStep = new PhaseStep();
      phaseStep.setUuid(uuid);
      phaseStep.setName(name);
      phaseStep.setPhaseStepType(phaseStepType);
      phaseStep.setStepsIds(stepsIds);
      phaseStep.setSteps(steps);
      phaseStep.setStepsInParallel(stepsInParallel);
      phaseStep.setFailureStrategies(failureStrategies);
      phaseStep.setRollback(rollback);
      phaseStep.setPhaseStepNameForRollback(phaseStepNameForRollback);
      phaseStep.setStatusForRollback(statusForRollback);
      phaseStep.setValid(valid);
      phaseStep.setValidationMessage(validationMessage);
      phaseStep.setArtifactNeeded(artifactNeeded);
      phaseStep.setWaitInterval(waitInterval);
      return phaseStep;
    }
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends BaseYamlWithType {
    private String name;
    private String statusForRollback;
    private boolean stepsInParallel;
    private List<StepYaml> steps = new ArrayList<>();
    private List<FailureStrategy.Yaml> failureStrategies = new ArrayList<>();
    private String phaseStepNameForRollback;
    private Integer waitInterval;

    @lombok.Builder
    public Yaml(String type, String name, String statusForRollback, boolean stepsInParallel, List<StepYaml> steps,
        List<FailureStrategy.Yaml> failureStrategies, String phaseStepNameForRollback, Integer waitInterval) {
      super(type);
      this.name = name;
      this.statusForRollback = statusForRollback;
      this.stepsInParallel = stepsInParallel;
      this.steps = steps;
      this.failureStrategies = failureStrategies;
      this.phaseStepNameForRollback = phaseStepNameForRollback;
      this.waitInterval = waitInterval;
    }
  }
}
