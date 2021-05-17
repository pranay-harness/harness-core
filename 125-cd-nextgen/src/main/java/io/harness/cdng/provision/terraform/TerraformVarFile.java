package io.harness.cdng.provision.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotations.dev.OwnedBy;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
public class TerraformVarFile {
  String type;
  @NonNull String identifier;

  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true) TerraformVarFileSpec spec;

  @Builder
  public TerraformVarFile(String type, TerraformVarFileSpec spec, String identifier) {
    this.type = type;
    this.spec = spec;
    this.identifier = identifier;
  }
}
