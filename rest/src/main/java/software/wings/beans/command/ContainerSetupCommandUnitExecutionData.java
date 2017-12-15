package software.wings.beans.command;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by brett on 11/18/17
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
public class ContainerSetupCommandUnitExecutionData extends CommandExecutionData {
  private String containerServiceName;
}
