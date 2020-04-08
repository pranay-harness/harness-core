package software.wings.sm.states.spotinst;

import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.Application;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.AwsConfig;
import software.wings.beans.Environment;
import software.wings.beans.SpotInstConfig;

import java.util.List;

@Data
@Builder
class SpotinstTrafficShiftDataBag {
  private Application app;
  private Environment env;
  private AwsAmiInfrastructureMapping infrastructureMapping;
  private AwsConfig awsConfig;
  private List<EncryptedDataDetail> awsEncryptedDataDetails;
  private SpotInstConfig spotinstConfig;
  private List<EncryptedDataDetail> spotinstEncryptedDataDetails;
}
