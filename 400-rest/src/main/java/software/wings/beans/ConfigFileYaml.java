package software.wings.beans;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.yaml.YamlType;
import software.wings.yaml.BaseEntityYaml;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@TargetModule(HarnessModule._870_CG_YAML_BEANS)
@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class ConfigFileYaml extends BaseEntityYaml {
  private String targetFilePath;
  private boolean encrypted;
  private String fileName;
  private String description;
  private String checksum;
  private String checksumType;
  private boolean targetToAllEnv;
  private List<String> targetEnvs = new ArrayList<>();

  public ConfigFileYaml(String harnessApiVersion) {
    super(YamlType.CONFIG_FILE.name(), harnessApiVersion);
  }

  @Builder
  public ConfigFileYaml(String harnessApiVersion, String targetFilePath, boolean encrypted, String fileName,
      String description, String checksum, String checksumType, boolean targetToAllEnv, List<String> targetEnvs) {
    super(YamlType.CONFIG_FILE.name(), harnessApiVersion);
    this.targetFilePath = targetFilePath;
    this.encrypted = encrypted;
    this.fileName = fileName;
    this.description = description;
    this.checksum = checksum;
    this.checksumType = checksumType;
    this.targetToAllEnv = targetToAllEnv;
    this.targetEnvs = targetEnvs;
  }
}
