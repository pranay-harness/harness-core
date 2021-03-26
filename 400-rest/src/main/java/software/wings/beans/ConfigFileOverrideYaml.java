package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.yaml.YamlType;
import software.wings.yaml.BaseEntityYaml;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ConfigFileOverrideYaml extends BaseEntityYaml {
  private String serviceName;
  private String targetFilePath;
  private String fileName;
  private String checksum;
  private String checksumType;
  private boolean encrypted;

  public ConfigFileOverrideYaml(String harnessApiVersion) {
    super(YamlType.CONFIG_FILE_OVERRIDE.name(), harnessApiVersion);
  }

  @Builder
  public ConfigFileOverrideYaml(String harnessApiVersion, String serviceName, String targetFilePath, String fileName,
      String checksum, String checksumType, boolean encrypted) {
    super(YamlType.CONFIG_FILE_OVERRIDE.name(), harnessApiVersion);
    this.serviceName = serviceName;
    this.targetFilePath = targetFilePath;
    this.fileName = fileName;
    this.checksum = checksum;
    this.checksumType = checksumType;
    this.encrypted = encrypted;
  }
}
