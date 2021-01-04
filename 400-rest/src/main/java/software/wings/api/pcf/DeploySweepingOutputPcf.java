package software.wings.api.pcf;

import io.harness.pms.sdk.core.data.SweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonTypeName("deploySweepingOutputPcf")
public class DeploySweepingOutputPcf implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "pcfDeploySweepingOutput";

  private String uuid;
  private String name;
  private String commandName;
  private List<PcfServiceData> instanceData;

  @Override
  public String getType() {
    return "deploySweepingOutputPcf";
  }
}
