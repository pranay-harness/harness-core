package software.wings.beans;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.lang.String.format;
import static software.wings.beans.GraphNode.GraphNodeBuilder.aGraphNode;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import org.mongodb.morphia.annotations.Embedded;
import software.wings.common.Constants;
import software.wings.sm.InstanceStatusSummary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Class Node.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphNode {
  private String id = generateUuid();
  private String name;
  private String type;
  private boolean rollback;
  private String status;

  private Object executionSummary;
  private Object executionDetails;
  private String detailsReference;
  private boolean origin;

  private int executionHistoryCount;
  private int interruptHistoryCount;

  private boolean valid = true;
  private String validationMessage;
  private Map<String, String> inValidFieldMessages;

  private List<ElementExecutionSummary> elementStatusSummary;
  private List<InstanceStatusSummary> instanceStatusSummary;
  private List<TemplateExpression> templateExpressions;
  private List<NameValuePair> variableOverrides;

  @Embedded private Map<String, Object> properties = new HashMap<>();

  private GraphNode next;
  private GraphGroup group;

  public GraphNode cloneInternal() {
    GraphNode clonedNode = aGraphNode()
                               .withId("node_" + generateUuid())
                               .withName(getName())
                               .withType(getType())
                               .withRollback(isRollback())
                               .withStatus(getStatus())
                               .withExecutionSummary(getExecutionSummary())
                               .withExecutionDetails(getExecutionDetails())
                               .withDetailsReference(getDetailsReference())
                               .withOrigin(isOrigin())
                               .withExecutionHistoryCount(getExecutionHistoryCount())
                               .withInterruptHistoryCount(getInterruptHistoryCount())
                               .withValid(isValid())
                               .withValidationMessage(getValidationMessage())
                               .withInValidFieldMessages(getInValidFieldMessages())
                               .withElementStatusSummary(getElementStatusSummary())
                               .withInstanceStatusSummary(getInstanceStatusSummary())
                               .withTemplateExpressions(getTemplateExpressions())
                               .withVariableOverrides(getVariableOverrides())
                               .build();
    clonedNode.setProperties(getProperties());
    return clonedNode;
  }

  public boolean validate() {
    if (isEmpty(inValidFieldMessages)) {
      valid = true;
      validationMessage = null;
    } else {
      valid = false;
      validationMessage = format(Constants.STEP_VALIDATION_MESSAGE, inValidFieldMessages.keySet());
    }
    return valid;
  }

  public static final class GraphNodeBuilder {
    private String id = generateUuid();
    private String name;
    private String type;
    private boolean rollback;
    private String status;
    private Object executionSummary;
    private Object executionDetails;
    private String detailsReference;
    private boolean origin;
    private int executionHistoryCount;
    private int interruptHistoryCount;
    private boolean valid = true;
    private String validationMessage;
    private Map<String, String> inValidFieldMessages;
    private List<ElementExecutionSummary> elementStatusSummary;
    private List<InstanceStatusSummary> instanceStatusSummary;
    private List<TemplateExpression> templateExpressions;
    private List<NameValuePair> variableOverrides;
    private Map<String, Object> properties = new HashMap<>();
    private GraphNode next;
    private GraphGroup group;

    private GraphNodeBuilder() {}

    public static GraphNodeBuilder aGraphNode() {
      return new GraphNodeBuilder();
    }

    public GraphNodeBuilder withId(String id) {
      this.id = id;
      return this;
    }

    public GraphNodeBuilder withName(String name) {
      this.name = name;
      return this;
    }

    public GraphNodeBuilder withType(String type) {
      this.type = type;
      return this;
    }

    public GraphNodeBuilder withRollback(boolean rollback) {
      this.rollback = rollback;
      return this;
    }

    public GraphNodeBuilder withStatus(String status) {
      this.status = status;
      return this;
    }

    public GraphNodeBuilder withExecutionSummary(Object executionSummary) {
      this.executionSummary = executionSummary;
      return this;
    }

    public GraphNodeBuilder withExecutionDetails(Object executionDetails) {
      this.executionDetails = executionDetails;
      return this;
    }

    public GraphNodeBuilder withDetailsReference(String detailsReference) {
      this.detailsReference = detailsReference;
      return this;
    }

    public GraphNodeBuilder withOrigin(boolean origin) {
      this.origin = origin;
      return this;
    }

    public GraphNodeBuilder withExecutionHistoryCount(int executionHistoryCount) {
      this.executionHistoryCount = executionHistoryCount;
      return this;
    }

    public GraphNodeBuilder withInterruptHistoryCount(int interruptHistoryCount) {
      this.interruptHistoryCount = interruptHistoryCount;
      return this;
    }

    public GraphNodeBuilder withValid(boolean valid) {
      this.valid = valid;
      return this;
    }

    public GraphNodeBuilder withValidationMessage(String validationMessage) {
      this.validationMessage = validationMessage;
      return this;
    }

    public GraphNodeBuilder withInValidFieldMessages(Map<String, String> inValidFieldMessages) {
      this.inValidFieldMessages = inValidFieldMessages;
      return this;
    }

    public GraphNodeBuilder withElementStatusSummary(List<ElementExecutionSummary> elementStatusSummary) {
      this.elementStatusSummary = elementStatusSummary;
      return this;
    }

    public GraphNodeBuilder withInstanceStatusSummary(List<InstanceStatusSummary> instanceStatusSummary) {
      this.instanceStatusSummary = instanceStatusSummary;
      return this;
    }

    public GraphNodeBuilder withTemplateExpressions(List<TemplateExpression> templateExpressions) {
      this.templateExpressions = templateExpressions;
      return this;
    }

    public GraphNodeBuilder withVariableOverrides(List<NameValuePair> variableOverrides) {
      this.variableOverrides = variableOverrides;
      return this;
    }

    public GraphNodeBuilder addProperty(String name, Object property) {
      if (properties == null) {
        properties = new HashMap<>();
      }
      properties.put(name, property);
      return this;
    }

    public GraphNodeBuilder withProperties(Map<String, Object> properties) {
      this.properties = properties;
      return this;
    }

    public GraphNodeBuilder withNext(GraphNode next) {
      this.next = next;
      return this;
    }

    public GraphNodeBuilder withGroup(GraphGroup group) {
      this.group = group;
      return this;
    }

    public GraphNode build() {
      GraphNode graphNode = new GraphNode();
      graphNode.setId(id);
      graphNode.setName(name);
      graphNode.setType(type);
      graphNode.setRollback(rollback);
      graphNode.setStatus(status);
      graphNode.setExecutionSummary(executionSummary);
      graphNode.setExecutionDetails(executionDetails);
      graphNode.setDetailsReference(detailsReference);
      graphNode.setOrigin(origin);
      graphNode.setExecutionHistoryCount(executionHistoryCount);
      graphNode.setInterruptHistoryCount(interruptHistoryCount);
      graphNode.setValid(valid);
      graphNode.setValidationMessage(validationMessage);
      graphNode.setInValidFieldMessages(inValidFieldMessages);
      graphNode.setElementStatusSummary(elementStatusSummary);
      graphNode.setInstanceStatusSummary(instanceStatusSummary);
      graphNode.setTemplateExpressions(templateExpressions);
      graphNode.setVariableOverrides(variableOverrides);
      graphNode.setProperties(properties);
      graphNode.setNext(next);
      graphNode.setGroup(group);
      return graphNode;
    }
  }
}
