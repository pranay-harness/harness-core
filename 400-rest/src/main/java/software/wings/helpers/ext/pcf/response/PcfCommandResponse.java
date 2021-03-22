package software.wings.helpers.ext.pcf.response;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.logging.CommandExecutionStatus;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
  @JsonSubTypes.Type(value = PcfDeployCommandResponse.class, name = "pcfDeployCommandResponse")
  , @JsonSubTypes.Type(value = PcfInfraMappingDataResponse.class, name = "pcfInfraMappingDataResponse"),
      @JsonSubTypes.Type(value = PcfInstanceSyncResponse.class, name = "pcfInstanceSyncResponse"),
      @JsonSubTypes.Type(value = PcfSetupCommandResponse.class, name = "pcfSetupCommandResponse")
})
@OwnedBy(CDP)
public class PcfCommandResponse {
  private CommandExecutionStatus commandExecutionStatus;
  private String output;
}
