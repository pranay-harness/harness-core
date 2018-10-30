package software.wings.service.impl.yaml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import software.wings.security.UserPermissionInfo;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class YamlDirectoryFetchPayload {
  private String accountId;
  private String appId;
  private String entityId;
  private boolean applyPermissions;
  private UserPermissionInfo userPermissionInfo;
  private boolean appLevelYamlTreeOnly;
  private boolean addApplication;
}
