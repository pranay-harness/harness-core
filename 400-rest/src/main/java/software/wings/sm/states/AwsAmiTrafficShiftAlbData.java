package software.wings.sm.states;

import io.harness.beans.EmbeddedUser;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.Application;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsConfig;
import software.wings.beans.Environment;
import software.wings.beans.Service;
import software.wings.beans.artifact.Artifact;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsAmiTrafficShiftAlbData {
  private Artifact artifact;
  private Application app;
  private Service service;
  private Environment env;
  private AwsConfig awsConfig;
  private AwsAmiInfrastructureMapping infrastructureMapping;
  private List<EncryptedDataDetail> awsEncryptedDataDetails;
  private String region;
  private String serviceId;
  private EmbeddedUser currentUser;
}
