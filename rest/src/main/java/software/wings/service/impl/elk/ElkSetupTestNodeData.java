package software.wings.service.impl.elk;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.api.InstanceElement;
import software.wings.service.impl.analysis.SetupTestNodeData;

/**
 * ELK request payload for TestNodeData.
 * Created by Pranjal on 08/17/2018
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@EqualsAndHashCode(callSuper = false)
public class ElkSetupTestNodeData extends SetupTestNodeData {
  private String query;
  private String indices;
  private String messageField;
  private String timeStampField;
  private String timeStampFieldFormat;
  private ElkQueryType queryType;
  private String hostNameField;
  private boolean formattedQuery;

  @Builder
  public ElkSetupTestNodeData(String appId, String settingId, String instanceName, InstanceElement instanceElement,
      String hostExpression, String workflowId, long fromTime, long toTime, String query, String indices,
      String messageField, String timeStampField, String timeStampFieldFormat, ElkQueryType queryType,
      String hostNameField, boolean formattedQuery) {
    super(appId, settingId, instanceName, instanceElement, hostExpression, workflowId, fromTime, toTime);
    this.query = query;
    this.indices = indices;
    this.messageField = messageField;
    this.timeStampField = timeStampField;
    this.timeStampFieldFormat = timeStampFieldFormat;
    this.queryType = queryType;
    this.hostNameField = hostNameField;
    this.formattedQuery = formattedQuery;
  }
}
