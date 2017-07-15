package software.wings.beans;

import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.Link.Builder.aLink;
import static software.wings.beans.Graph.Node.Builder.aNode;
import static software.wings.sm.StateType.FORK;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.annotations.Transient;
import software.wings.api.DeploymentType;
import software.wings.beans.Graph.Builder;
import software.wings.beans.Graph.Node;
import software.wings.common.Constants;
import software.wings.common.UUIDGenerator;
import software.wings.sm.ExecutionStatus;
import software.wings.sm.StateType;
import software.wings.sm.TransitionType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by rishi on 12/21/16.
 */
public class PhaseStep {
  private String uuid = UUIDGenerator.getUuid();
  private String name;
  private PhaseStepType phaseStepType;
  @JsonIgnore private List<String> stepsIds = new ArrayList<>();
  @Transient private List<Node> steps = new ArrayList<>();
  private boolean stepsInParallel;
  @Embedded private List<FailureStrategy> failureStrategies = new ArrayList<>();

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
    this.uuid = UUIDGenerator.getUuid();
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

  public List<Node> getSteps() {
    return steps;
  }

  public void setSteps(List<Node> steps) {
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

  public Node generatePhaseStepNode() {
    // TODO: removing failure strategy as part of Node due to mongo driver limitation - we can try with later version
    // CodecConfigurationException: Can't find a codec for class software.wings.beans.FailureStrategy

    return aNode()
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

  public Graph generateSubworkflow(DeploymentType deploymentType) {
    Builder graphBuilder = aGraph().withGraphName(name);
    if (steps == null || steps.isEmpty()) {
      return graphBuilder.build();
    }

    Node originNode = null;

    if (stepsInParallel && steps.size() > 1) {
      Node forkNode = aNode().withId(getUuid()).withType(FORK.name()).withName(name + "-FORK").build();
      graphBuilder.addNodes(forkNode);
      for (Node step : steps) {
        step.setOrigin(false);
        graphBuilder.addNodes(step);
        graphBuilder.addLinks(aLink()
                                  .withId(getUuid())
                                  .withFrom(forkNode.getId())
                                  .withTo(step.getId())
                                  .withType(TransitionType.FORK.name())
                                  .build());
      }
      if (originNode == null) {
        originNode = forkNode;
      }
    } else {
      String id1 = null;
      String id2;
      for (Node step : getSteps()) {
        id2 = step.getId();
        step.setOrigin(false);
        graphBuilder.addNodes(step);
        if (id1 == null && originNode == null) {
          originNode = step;
        } else {
          graphBuilder.addLinks(
              aLink().withId(getUuid()).withFrom(id1).withTo(id2).withType(TransitionType.SUCCESS.name()).build());
        }
        id1 = id2;
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
          steps.stream().filter(step -> !step.validate()).map(Node::getName).collect(Collectors.toList());
      if (invalidChildren != null && !invalidChildren.isEmpty()) {
        valid = false;
        validationMessage = String.format(Constants.PHASE_STEP_VALIDATION_MESSAGE, invalidChildren.toString());
      }
    }
    return valid;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o)
      return true;
    if (o == null || getClass() != o.getClass())
      return false;

    PhaseStep phaseStep = (PhaseStep) o;

    if (stepsInParallel != phaseStep.stepsInParallel)
      return false;
    if (rollback != phaseStep.rollback)
      return false;
    if (uuid != null ? !uuid.equals(phaseStep.uuid) : phaseStep.uuid != null)
      return false;
    if (name != null ? !name.equals(phaseStep.name) : phaseStep.name != null)
      return false;
    if (phaseStepType != phaseStep.phaseStepType)
      return false;
    if (stepsIds != null ? !stepsIds.equals(phaseStep.stepsIds) : phaseStep.stepsIds != null)
      return false;
    if (steps != null ? !steps.equals(phaseStep.steps) : phaseStep.steps != null)
      return false;
    if (failureStrategies != null ? !failureStrategies.equals(phaseStep.failureStrategies)
                                  : phaseStep.failureStrategies != null)
      return false;
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
    private String uuid = UUIDGenerator.getUuid();
    private String name;
    private PhaseStepType phaseStepType;
    private List<String> stepsIds = new ArrayList<>();
    private List<Node> steps = new ArrayList<>();
    private boolean stepsInParallel;
    private List<FailureStrategy> failureStrategies = new ArrayList<>();
    private boolean rollback;
    private String phaseStepNameForRollback;
    private ExecutionStatus statusForRollback;
    private boolean valid = true;
    private String validationMessage;

    private PhaseStepBuilder() {}

    public static PhaseStepBuilder aPhaseStep(PhaseStepType phaseStepType, String name) {
      PhaseStepBuilder phaseStepBuilder = new PhaseStepBuilder();
      phaseStepBuilder.phaseStepType = phaseStepType;
      phaseStepBuilder.name = name;
      phaseStepBuilder.uuid = UUIDGenerator.getUuid();
      return phaseStepBuilder;
    }

    public PhaseStepBuilder withPhaseStepType(PhaseStepType phaseStepType) {
      this.phaseStepType = phaseStepType;
      return this;
    }

    public PhaseStepBuilder addStep(Node step) {
      this.steps.add(step);
      return this;
    }

    public PhaseStepBuilder addAllSteps(List<Node> steps) {
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
      return phaseStep;
    }
  }
}
