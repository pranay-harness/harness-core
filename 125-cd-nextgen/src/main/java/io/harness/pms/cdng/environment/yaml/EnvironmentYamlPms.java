package io.harness.pms.cdng.environment.yaml;

import io.harness.pms.ng.core.environment.beans.EnvironmentTypePms;

import java.util.Map;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Wither;
import org.springframework.data.annotation.TypeAlias;

@Data
@Builder
@TypeAlias("environmentYamlPms")
public class EnvironmentYamlPms {
  String uuid;
  @Wither EnvironmentTypePms type;
  @Wither Map<String, String> tags;
}
