package software.wings.helpers.ext.pcf.response;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.harness.delegate.command.CommandExecutionResult.CommandExecutionStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = PcfDeployCommandResponse.class, name = "pcfDeployCommandResponse")
  , @JsonSubTypes.Type(value = PcfInfraMappingDataResponse.class, name = "pcfInfraMappingDataResponse"),
      @JsonSubTypes.Type(value = PcfInstanceSyncResponse.class, name = "pcfInstanceSyncResponse"),
      @JsonSubTypes.Type(value = PcfSetupCommandResponse.class, name = "pcfSetupCommandResponse")
})
public class PcfCommandResponse {
  private CommandExecutionStatus commandExecutionStatus;
  private String output;
}
