package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.mongodb.morphia.annotations.Entity;

@Entity(value = "managerConfiguration", noClassnameStored = true)
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@EqualsAndHashCode(callSuper = true)
public class ManagerConfiguration extends Base {
  public static final String GLOBAL_CONFIG_ID = "__GLOBAL_CONFIG_ID__";
  public static final String MATCH_ALL_VERSION = "*";
  String primaryVersion;

  public static final class Builder {
    String primaryVersion;

    private Builder() {}

    public static ManagerConfiguration.Builder aManagerConfiguration() {
      return new ManagerConfiguration.Builder();
    }

    public ManagerConfiguration.Builder withPrimaryVersion(String primaryVersion) {
      this.primaryVersion = primaryVersion;
      return this;
    }

    public ManagerConfiguration build() {
      ManagerConfiguration managerConfiguration = new ManagerConfiguration();
      managerConfiguration.setUuid(GLOBAL_CONFIG_ID);
      managerConfiguration.setPrimaryVersion(primaryVersion);
      return managerConfiguration;
    }
  }
}
