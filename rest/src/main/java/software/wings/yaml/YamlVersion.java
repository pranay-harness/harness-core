package software.wings.yaml;

public class YamlVersion extends YamlHistory {
  public String yamlVersionId;
  public int version;
  public YamlType type;
  public String entityId;
  public String inEffectStart;
  public String inEffectEnd;

  public YamlVersion() {}

  public String getYamlVersionId() {
    return yamlVersionId;
  }

  public void setYamlVersionId(String yamlVersionId) {
    this.yamlVersionId = yamlVersionId;
  }

  public int getVersion() {
    return version;
  }

  public void setVersion(int version) {
    this.version = version;
  }

  public YamlType getType() {
    return type;
  }

  public void setType(YamlType type) {
    this.type = type;
  }

  public String getEntityId() {
    return entityId;
  }

  public void setEntityId(String entityId) {
    this.entityId = entityId;
  }

  public String getInEffectStart() {
    return inEffectStart;
  }

  public void setInEffectStart(String inEffectStart) {
    this.inEffectStart = inEffectStart;
  }

  public String getInEffectEnd() {
    return inEffectEnd;
  }

  public void setInEffectEnd(String inEffectEnd) {
    this.inEffectEnd = inEffectEnd;
  }
}
