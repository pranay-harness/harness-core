package software.wings.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class ServiceNowImportSetResult {
  @JsonProperty("transform_map") private String transformMap;
  @JsonProperty("table") private String table;
  @JsonProperty("display_name") private String displayName;
  @JsonProperty("display_value") private String displayValue;
  @JsonProperty("record_link") private String recordLink;
  @JsonProperty("status") private String status;
  @JsonProperty("sys_id") private String sysId;
  @JsonProperty("status_message") private String statusMessage;
}
