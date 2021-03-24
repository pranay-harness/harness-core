package software.wings.resources.stats.model;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.yaml.BaseYaml;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@TargetModule(Module._870_CG_YAML_BEANS)
@Data
public final class TimeRangeYaml extends BaseYaml {
  private String from;
  private String to;
  private String timeZone;

  @Builder
  public TimeRangeYaml(
      @JsonProperty("from") String from, @JsonProperty("to") String to, @JsonProperty("timeZone") String timeZone) {
    setFrom(from);
    setTo(to);
    setTimeZone(timeZone);
  }
}
