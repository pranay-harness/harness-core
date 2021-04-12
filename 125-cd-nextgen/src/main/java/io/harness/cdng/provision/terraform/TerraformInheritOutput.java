package io.harness.cdng.provision.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.yaml.StoreConfig;
import io.harness.pms.sdk.core.data.ExecutionSweepingOutput;
import io.harness.security.encryption.EncryptedRecordData;
import io.harness.security.encryption.EncryptionConfig;

import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(CDP)
@Data
@Builder
@TypeAlias("terraformInheritOutput")
@JsonTypeName("terraformInheritOutput")
public class TerraformInheritOutput implements ExecutionSweepingOutput {
  String workspace;
  StoreConfig configFiles;
  List<String> inlineVarFiles;
  List<StoreConfig> remoteVarFiles;
  String backendConfig;
  List<String> targets;

  EncryptionConfig encryptionConfig;
  EncryptedRecordData encryptedTfPlan;
  String planName;

  @Override
  public String getType() {
    return "terraformInheritOutput";
  }
}
