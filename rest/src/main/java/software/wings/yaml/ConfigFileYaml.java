package software.wings.yaml;

import com.google.common.collect.Maps;

import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.ChecksumType;
import software.wings.beans.ConfigFile.ConfigOverrideType;

import java.util.Map;

/**
 * Yaml file for configFile.
 * @author rktummala on 09/28/17
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ConfigFileYaml extends BaseYaml {
  @YamlSerialize private String mimeType;
  @YamlSerialize private long size;
  @YamlSerialize private ChecksumType checksumType = ChecksumType.MD5;
  @YamlSerialize private String checksum;
  @YamlSerialize private String parentConfigFileId;
  @YamlSerialize private String relativeFilePath;
  @YamlSerialize private boolean targetToAllEnv;
  @YamlSerialize private int defaultVersion;

  @YamlSerialize private Map<String, EntityVersionYaml> envNameVersionMap = Maps.newHashMap();
  @YamlSerialize private boolean setAsDefault;
  @YamlSerialize private String notes;
  @YamlSerialize private String description;
  @YamlSerialize private String overridePath;
  @YamlSerialize private String overrideEnvName;
  @YamlSerialize ConfigOverrideType configOverrideType;
  @YamlSerialize private String configOverrideExpression;

  @YamlSerialize private String config;
  @YamlSerialize private boolean encrypted = false;

  public ConfigFileYaml() {}

  public static final class Builder {
    ConfigOverrideType configOverrideType;
    private String mimeType;
    private long size;
    private ChecksumType checksumType = ChecksumType.MD5;
    private String checksum;
    private String parentConfigFileId;
    private String relativeFilePath;
    private boolean targetToAllEnv;
    private int defaultVersion;
    private Map<String, EntityVersionYaml> envNameVersionMap = Maps.newHashMap();
    private boolean setAsDefault;
    private String notes;
    private String description;
    private String overridePath;
    private String overrideEnvName;
    private String configOverrideExpression;
    private String config;
    private boolean encrypted = false;

    private Builder() {}

    public static Builder aConfigFileYaml() {
      return new Builder();
    }

    public Builder withMimeType(String mimeType) {
      this.mimeType = mimeType;
      return this;
    }

    public Builder withSize(long size) {
      this.size = size;
      return this;
    }

    public Builder withChecksumType(ChecksumType checksumType) {
      this.checksumType = checksumType;
      return this;
    }

    public Builder withChecksum(String checksum) {
      this.checksum = checksum;
      return this;
    }

    public Builder withParentConfigFileId(String parentConfigFileId) {
      this.parentConfigFileId = parentConfigFileId;
      return this;
    }

    public Builder withRelativeFilePath(String relativeFilePath) {
      this.relativeFilePath = relativeFilePath;
      return this;
    }

    public Builder withTargetToAllEnv(boolean targetToAllEnv) {
      this.targetToAllEnv = targetToAllEnv;
      return this;
    }

    public Builder withDefaultVersion(int defaultVersion) {
      this.defaultVersion = defaultVersion;
      return this;
    }

    public Builder withEnvNameVersionMap(Map<String, EntityVersionYaml> envNameVersionMap) {
      this.envNameVersionMap = envNameVersionMap;
      return this;
    }

    public Builder withSetAsDefault(boolean setAsDefault) {
      this.setAsDefault = setAsDefault;
      return this;
    }

    public Builder withNotes(String notes) {
      this.notes = notes;
      return this;
    }

    public Builder withDescription(String description) {
      this.description = description;
      return this;
    }

    public Builder withOverridePath(String overridePath) {
      this.overridePath = overridePath;
      return this;
    }

    public Builder withOverrideEnvName(String overrideEnvName) {
      this.overrideEnvName = overrideEnvName;
      return this;
    }

    public Builder withConfigOverrideType(ConfigOverrideType configOverrideType) {
      this.configOverrideType = configOverrideType;
      return this;
    }

    public Builder withConfigOverrideExpression(String configOverrideExpression) {
      this.configOverrideExpression = configOverrideExpression;
      return this;
    }

    public Builder withConfig(String config) {
      this.config = config;
      return this;
    }

    public Builder withEncrypted(boolean encrypted) {
      this.encrypted = encrypted;
      return this;
    }

    public Builder but() {
      return aConfigFileYaml()
          .withMimeType(mimeType)
          .withSize(size)
          .withChecksumType(checksumType)
          .withChecksum(checksum)
          .withParentConfigFileId(parentConfigFileId)
          .withRelativeFilePath(relativeFilePath)
          .withTargetToAllEnv(targetToAllEnv)
          .withDefaultVersion(defaultVersion)
          .withEnvNameVersionMap(envNameVersionMap)
          .withSetAsDefault(setAsDefault)
          .withNotes(notes)
          .withDescription(description)
          .withOverridePath(overridePath)
          .withOverrideEnvName(overrideEnvName)
          .withConfigOverrideType(configOverrideType)
          .withConfigOverrideExpression(configOverrideExpression)
          .withConfig(config)
          .withEncrypted(encrypted);
    }

    public ConfigFileYaml build() {
      ConfigFileYaml configFileYaml = new ConfigFileYaml();
      configFileYaml.setMimeType(mimeType);
      configFileYaml.setSize(size);
      configFileYaml.setChecksumType(checksumType);
      configFileYaml.setChecksum(checksum);
      configFileYaml.setParentConfigFileId(parentConfigFileId);
      configFileYaml.setRelativeFilePath(relativeFilePath);
      configFileYaml.setTargetToAllEnv(targetToAllEnv);
      configFileYaml.setDefaultVersion(defaultVersion);
      configFileYaml.setEnvNameVersionMap(envNameVersionMap);
      configFileYaml.setSetAsDefault(setAsDefault);
      configFileYaml.setNotes(notes);
      configFileYaml.setDescription(description);
      configFileYaml.setOverridePath(overridePath);
      configFileYaml.setOverrideEnvName(overrideEnvName);
      configFileYaml.setConfigOverrideType(configOverrideType);
      configFileYaml.setConfigOverrideExpression(configOverrideExpression);
      configFileYaml.setConfig(config);
      configFileYaml.setEncrypted(encrypted);
      return configFileYaml;
    }
  }
}
