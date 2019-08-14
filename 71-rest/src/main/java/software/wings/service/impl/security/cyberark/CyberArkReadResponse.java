package software.wings.service.impl.security.cyberark;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

/**
 * @author marklu on 2019-08-01
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class CyberArkReadResponse {
  @JsonProperty("Name") private String name;
  @JsonProperty("UserName") private String userName;
  @JsonProperty("Content") private String content;
  @JsonProperty("Folder") private String folder;
  @JsonProperty("Safe") private String safe;
  @JsonProperty("Address") private String address;
  @JsonProperty("LogonDomain") private String logonDomain;
  @JsonProperty("DeviceType") private String deviceType;
  @JsonProperty("CreationMethod") private String creationMethod;
  @JsonProperty("PasswordChangeInProcess") private String passwordChangeInProcess;
}
