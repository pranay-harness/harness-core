package io.harness.steps.dummy;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.SweepingOutput;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Map;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDC)
@Value
@Builder
@TypeAlias("dummySectionStepTransput")
@JsonTypeName("dummySectionStepTransput")
public class DummySectionStepTransput implements SweepingOutput {
  Map<String, String> map;

  @Override
  public String getType() {
    return "dummySectionStepTransput";
  }
}
