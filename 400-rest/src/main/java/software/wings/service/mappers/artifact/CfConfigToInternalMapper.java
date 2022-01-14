package software.wings.service.mappers.artifact;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.pcf.CfInternalConfig;

import software.wings.beans.PcfConfig;

import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(CDP)
@TargetModule(HarnessModule._957_CG_BEANS)
public class CfConfigToInternalMapper {
  public CfInternalConfig toCfInternalConfig(PcfConfig pcfConfig) {
    return CfInternalConfig.builder()
        .endpointUrl(pcfConfig.getEndpointUrl())
        .username(pcfConfig.getUsername())
        .password(pcfConfig.getPassword())
        .accountId(pcfConfig.getAccountId())
        .useEncryptedUsername(pcfConfig.isUseEncryptedUsername())
        .encryptedUsername(pcfConfig.getEncryptedUsername())
        .encryptedPassword(pcfConfig.getEncryptedPassword())
        .skipValidation(pcfConfig.isSkipValidation())
        .build();
  }
}
