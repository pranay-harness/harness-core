package software.wings.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import lombok.Data;

@Data

public class ServiceNowImportSetResponse {
  @JsonProperty("import_set") private String importSet;

  @JsonProperty("staging_table") private String stagingTable;

  @JsonProperty("result") private List<ServiceNowImportSetResult> result;
}
