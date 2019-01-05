package software.wings.beans;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.lang.String.format;
import static software.wings.beans.GraphNode.GraphNodeBuilder.aGraphNode;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
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
  private List<Variable> templateVariables;
  private String templateUuid;
  private String templateVersion;

  private Map<String, Object> properties = new HashMap<>();

  private GraphNode next;
  private GraphGroup group;

  public GraphNode cloneInternal() {
    GraphNode clonedNode = aGraphNode()
                               .id("node_" + generateUuid())
                               .name(getName())
                               .type(getType())
                               .rollback(isRollback())
                               .status(getStatus())
                               .executionSummary(getExecutionSummary())
                               .executionDetails(getExecutionDetails())
                               .detailsReference(getDetailsReference())
                               .origin(isOrigin())
                               .executionHistoryCount(getExecutionHistoryCount())
                               .interruptHistoryCount(getInterruptHistoryCount())
                               .valid(isValid())
                               .validationMessage(getValidationMessage())
                               .inValidFieldMessages(getInValidFieldMessages())
                               .elementStatusSummary(getElementStatusSummary())
                               .instanceStatusSummary(getInstanceStatusSummary())
                               .templateExpressions(getTemplateExpressions())
                               .variableOverrides(getVariableOverrides())
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
    private List<Variable> templateVariables;
    private String templateUuid;
    private String templateVersion;

    private GraphNodeBuilder() {}

    public static GraphNodeBuilder aGraphNode() {
      return new GraphNodeBuilder();
    }

    public GraphNodeBuilder id(String id) {
      this.id = id;
      return this;
    }

    public GraphNodeBuilder name(String name) {
      this.name = name;
      return this;
    }

    public GraphNodeBuilder type(String type) {
      this.type = type;
      return this;
    }

    public GraphNodeBuilder rollback(boolean rollback) {
      this.rollback = rollback;
      return this;
    }

    public GraphNodeBuilder status(String status) {
      this.status = status;
      return this;
    }

    public GraphNodeBuilder executionSummary(Object executionSummary) {
      this.executionSummary = executionSummary;
      return this;
    }

    public GraphNodeBuilder executionDetails(Object executionDetails) {
      this.executionDetails = executionDetails;
      return this;
    }

    public GraphNodeBuilder detailsReference(String detailsReference) {
      this.detailsReference = detailsReference;
      return this;
    }

    public GraphNodeBuilder origin(boolean origin) {
      this.origin = origin;
      return this;
    }

    public GraphNodeBuilder executionHistoryCount(int executionHistoryCount) {
      this.executionHistoryCount = executionHistoryCount;
      return this;
    }

    public GraphNodeBuilder interruptHistoryCount(int interruptHistoryCount) {
      this.interruptHistoryCount = interruptHistoryCount;
      return this;
    }

    public GraphNodeBuilder valid(boolean valid) {
      this.valid = valid;
      return this;
    }

    public GraphNodeBuilder validationMessage(String validationMessage) {
      this.validationMessage = validationMessage;
      return this;
    }

    public GraphNodeBuilder inValidFieldMessages(Map<String, String> inValidFieldMessages) {
      this.inValidFieldMessages = inValidFieldMessages;
      return this;
    }

    public GraphNodeBuilder elementStatusSummary(List<ElementExecutionSummary> elementStatusSummary) {
      this.elementStatusSummary = elementStatusSummary;
      return this;
    }

    public GraphNodeBuilder instanceStatusSummary(List<InstanceStatusSummary> instanceStatusSummary) {
      this.instanceStatusSummary = instanceStatusSummary;
      return this;
    }

    public GraphNodeBuilder templateExpressions(List<TemplateExpression> templateExpressions) {
      this.templateExpressions = templateExpressions;
      return this;
    }

    public GraphNodeBuilder variableOverrides(List<NameValuePair> variableOverrides) {
      this.variableOverrides = variableOverrides;
      return this;
    }

    public GraphNodeBuilder templateVariables(List<Variable> templateVariables) {
      this.templateVariables = templateVariables;
      return this;
    }

    public GraphNodeBuilder templateUuid(String templateUuid) {
      this.templateUuid = templateUuid;
      return this;
    }

    public GraphNodeBuilder templateVersion(String templateVersion) {
      this.templateVersion = templateVersion;
      return this;
    }

    public GraphNodeBuilder properties(Map<String, Object> properties) {
      this.properties = properties;
      return this;
    }

    public GraphNodeBuilder next(GraphNode next) {
      this.next = next;
      return this;
    }

    public GraphNodeBuilder group(GraphGroup group) {
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
      graphNode.setTemplateVariables(templateVariables);
      graphNode.setTemplateVersion(templateVersion);
      graphNode.setTemplateUuid(templateUuid);
      return graphNode;
    }
  }
}
