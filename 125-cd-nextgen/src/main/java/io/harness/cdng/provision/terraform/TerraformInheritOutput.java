package io.harness.cdng.provision.terraform;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.yaml.GitStoreConfig;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDP)
@Data
@Builder
@TypeAlias("terraformInheritOutput")
@JsonTypeName("terraformInheritOutput")
public class TerraformInheritOutput implements ExecutionSweepingOutput {
  String workspace;
  GitStoreConfig configFiles;
  List<TerraformVarFileConfig> varFileConfigs;
  String backendConfig;
  List<String> targets;
  Map<String, String> environmentVariables;

  EncryptionConfig encryptionConfig;
  EncryptedRecordData encryptedTfPlan;
  String planName;

  @Override
  public String getType() {
    return "terraformInheritOutput";
  }
}
