package software.wings.service.impl;

import io.fabric8.kubernetes.api.model.HasMetadata;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.SettingAttribute;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

@Data
@Builder
public class ContainerServiceParams {
  private SettingAttribute settingAttribute;
  private List<EncryptedDataDetail> encryptionDetails;
  private String containerServiceName;
  private String clusterName;
  private String namespace;
  private String region;
  private String kubernetesType;
}
