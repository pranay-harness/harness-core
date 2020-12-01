package io.harness.pms.cdng.manifest.yaml;

import io.harness.data.validator.EntityIdentifier;
import io.harness.pms.yaml.core.intfc.OverrideSetsWrapperPms;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Value;
import org.springframework.data.annotation.TypeAlias;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeName("overrideSet")
@TypeAlias("manifestOverrideSetsPms")
public class ManifestOverrideSetsPms implements OverrideSetsWrapperPms {
  String uuid;
  @EntityIdentifier String identifier;
  List<ManifestConfigWrapperPms> manifests;
}
