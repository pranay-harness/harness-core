package software.wings.beans;

import com.google.common.base.MoreObjects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.github.zafarkhaja.semver.Version;
import software.wings.settings.SettingValue;

import java.util.List;
import java.util.Objects;

/**
 * Created by peeyushaggarwal on 10/20/16.
 */
public class AccountPlugin implements WingsPlugin {
  private String type;
  private String displayName;
  private Class<? extends SettingValue> settingClass;
  private List<PluginCategory> pluginCategories;
  @JsonIgnore private Object uiSchema;
  boolean isEnabled;
  private String accountId;
  private Version version;

  /** {@inheritDoc} */
  @Override
  public String getType() {
    return type;
  }

  /**
   * Setter for property 'type'.
   *
   * @param type Value to set for property 'type'.
   */
  public void setType(String type) {
    this.type = type;
  }

  /** {@inheritDoc} */
  @Override
  public Class<? extends SettingValue> getSettingClass() {
    return settingClass;
  }

  /**
   * Setter for property 'settingClass'.
   *
   * @param settingClass Value to set for property 'settingClass'.
   */
  public void setSettingClass(Class<? extends SettingValue> settingClass) {
    this.settingClass = settingClass;
  }

  /** {@inheritDoc} */
  @Override
  public List<PluginCategory> getPluginCategories() {
    return pluginCategories;
  }

  /**
   * Setter for property 'pluginCategories'.
   *
   * @param pluginCategories Value to set for property 'pluginCategories'.
   */
  public void setPluginCategories(List<PluginCategory> pluginCategories) {
    this.pluginCategories = pluginCategories;
  }

  /** {@inheritDoc} */
  @Override
  public boolean isEnabled() {
    return isEnabled;
  }

  /**
   * Setter for property 'enabled'.
   *
   * @param enabled Value to set for property 'enabled'.
   */
  public void setEnabled(boolean enabled) {
    isEnabled = enabled;
  }

  /**
   * Getter for property 'accountId'.
   *
   * @return Value for property 'accountId'.
   */
  public String getAccountId() {
    return accountId;
  }

  /**
   * Setter for property 'accountId'.
   *
   * @param accountId Value to set for property 'accountId'.
   */
  public void setAccountId(String accountId) {
    this.accountId = accountId;
  }

  /** {@inheritDoc} */
  @Override
  public Version getVersion() {
    return version;
  }

  /**
   * Setter for property 'version'.
   *
   * @param version Value to set for property 'version'.
   */
  public void setVersion(Version version) {
    this.version = version;
  }

  /**
   * Getter for property 'displayName'.
   *
   * @return Value for property 'displayName'.
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Setter for property 'displayName'.
   *
   * @param displayName Value to set for property 'displayName'.
   */
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  /**
   * Getter for property 'uiSchema'.
   *
   * @return Value for property 'uiSchema'.
   */
  public Object getUiSchema() {
    return uiSchema;
  }

  /**
   * Setter for property 'uiSchema'.
   *
   * @param uiSchema Value to set for property 'uiSchema'.
   */
  public void setUiSchema(Object uiSchema) {
    this.uiSchema = uiSchema;
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, displayName, settingClass, pluginCategories, isEnabled, accountId, version);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final AccountPlugin other = (AccountPlugin) obj;
    return Objects.equals(this.type, other.type) && Objects.equals(this.displayName, other.displayName)
        && Objects.equals(this.settingClass, other.settingClass)
        && Objects.equals(this.pluginCategories, other.pluginCategories)
        && Objects.equals(this.isEnabled, other.isEnabled) && Objects.equals(this.accountId, other.accountId)
        && Objects.equals(this.version, other.version);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("type", type)
        .add("displayName", displayName)
        .add("settingClass", settingClass)
        .add("pluginCategories", pluginCategories)
        .add("isEnabled", isEnabled)
        .add("accountId", accountId)
        .add("version", version)
        .add("enabled", isEnabled())
        .toString();
  }

  public static final class Builder {
    boolean isEnabled;
    private String type;
    private String displayName;
    private Class<? extends SettingValue> settingClass;
    private List<PluginCategory> pluginCategories;
    private Object uiSchema;
    private String accountId;
    private Version version;

    private Builder() {}

    public static Builder anAccountPlugin() {
      return new Builder();
    }

    public Builder withType(String type) {
      this.type = type;
      return this;
    }

    public Builder withDisplayName(String displayName) {
      this.displayName = displayName;
      return this;
    }

    public Builder withSettingClass(Class<? extends SettingValue> settingClass) {
      this.settingClass = settingClass;
      return this;
    }

    public Builder withPluginCategories(List<PluginCategory> pluginCategories) {
      this.pluginCategories = pluginCategories;
      return this;
    }

    public Builder withUiSchema(Object uiSchema) {
      this.uiSchema = uiSchema;
      return this;
    }

    public Builder withIsEnabled(boolean isEnabled) {
      this.isEnabled = isEnabled;
      return this;
    }

    public Builder withAccountId(String accountId) {
      this.accountId = accountId;
      return this;
    }

    public Builder withVersion(Version version) {
      this.version = version;
      return this;
    }

    public Builder but() {
      return anAccountPlugin()
          .withType(type)
          .withDisplayName(displayName)
          .withSettingClass(settingClass)
          .withPluginCategories(pluginCategories)
          .withUiSchema(uiSchema)
          .withIsEnabled(isEnabled)
          .withAccountId(accountId)
          .withVersion(version);
    }

    public AccountPlugin build() {
      AccountPlugin accountPlugin = new AccountPlugin();
      accountPlugin.setType(type);
      accountPlugin.setDisplayName(displayName);
      accountPlugin.setSettingClass(settingClass);
      accountPlugin.setPluginCategories(pluginCategories);
      accountPlugin.setUiSchema(uiSchema);
      accountPlugin.setAccountId(accountId);
      accountPlugin.setVersion(version);
      accountPlugin.isEnabled = this.isEnabled;
      return accountPlugin;
    }
  }
}
