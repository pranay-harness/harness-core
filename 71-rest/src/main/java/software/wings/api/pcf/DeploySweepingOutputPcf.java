package software.wings.api.pcf;

import io.harness.data.SweepingOutput;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class DeploySweepingOutputPcf implements SweepingOutput {
  public static final String SWEEPING_OUTPUT_NAME = "pcfDeploySweepingOutput";

  private String uuid;
  private String name;
  private String commandName;
  private List<PcfServiceData> instanceData;
}
