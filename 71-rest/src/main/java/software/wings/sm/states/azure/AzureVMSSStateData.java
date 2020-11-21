package software.wings.sm.states.azure;

import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.Application;
import software.wings.beans.AzureConfig;
import software.wings.beans.AzureVMSSInfrastructureMapping;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.artifact.Artifact;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AzureVMSSStateData {
  private Application application;
  private Service service;
  private String serviceId;
  private Artifact artifact;
  private AzureConfig azureConfig;
  private Environment environment;
  private AzureVMSSInfrastructureMapping infrastructureMapping;
  private List<EncryptedDataDetail> azureEncryptedDataDetails;
}
