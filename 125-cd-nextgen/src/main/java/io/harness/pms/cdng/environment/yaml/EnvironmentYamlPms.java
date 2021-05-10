package io.harness.pms.cdng.environment.yaml;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.ng.core.environment.beans.EnvironmentTypePms;
import io.harness.pms.yaml.YamlNode;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModelProperty;
import java.util.Map;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.Wither;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDC)
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
@TypeAlias("environmentYamlPms")
public class EnvironmentYamlPms {
  @JsonProperty(YamlNode.UUID_FIELD_NAME)
  @Getter(onMethod_ = { @ApiModelProperty(hidden = true) })
  @ApiModelProperty(hidden = true)
  String uuid;

  @Wither EnvironmentTypePms type;
  @Wither Map<String, String> tags;
}
