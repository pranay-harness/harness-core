package software.wings.beans;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static java.lang.String.format;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.delegate.beans.DelegateTaskDetails;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.NoArgsConstructor;
import software.wings.beans.template.TemplateMetadata;
import software.wings.beans.template.dto.ImportedTemplateDetails;
import software.wings.common.Constants;
import software.wings.sm.InstanceStatusSummary;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Class Node.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GraphNode {
  @Default private String id = generateUuid();
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

  private boolean hasInspection;

  @Default boolean valid = true;
  private String validationMessage;
  private Map<String, String> inValidFieldMessages;

  private List<ElementExecutionSummary> elementStatusSummary;
  private List<InstanceStatusSummary> instanceStatusSummary;
  private List<TemplateExpression> templateExpressions;
  private List<NameValuePair> variableOverrides;
  private List<Variable> templateVariables;
  private String templateUuid;
  private String templateVersion;
  private ImportedTemplateDetails importedTemplateDetails;
  private TemplateMetadata templateMetadata;

  @Default private Map<String, Object> properties = new HashMap<>();

  private GraphNode next;
  private GraphGroup group;

  /**
   * @deprecated Use delegateTasksDetails instead.
   */
  @Deprecated private String delegateTaskId;
  /**
   * @deprecated Use selectionLogsTrackingForTasksEnabled instead.
   */
  @Deprecated private boolean selectionLogsTrackingForTaskEnabled;

  private List<DelegateTaskDetails> delegateTasksDetails;
  private boolean selectionLogsTrackingForTasksEnabled;

  public GraphNode cloneInternal() {
    GraphNode clonedNode = GraphNode.builder()
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
                               .templateVariables(templateVariables)
                               .templateUuid(templateUuid)
                               .templateVersion(templateVersion)
                               .templateMetadata(templateMetadata)
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
}
