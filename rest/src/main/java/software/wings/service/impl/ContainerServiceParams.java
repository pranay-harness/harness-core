package software.wings.service.impl;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.SettingAttribute;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import java.util.Set;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@Builder
public class ContainerServiceParams {
  private SettingAttribute settingAttribute;
  private List<EncryptedDataDetail> encryptionDetails;
  private String containerServiceName;
  private String clusterName;
  private String namespace;
  private String region;
  private String subscriptionId;
  private String resourceGroup;
  private Set<String> containerServiceNames;
}
