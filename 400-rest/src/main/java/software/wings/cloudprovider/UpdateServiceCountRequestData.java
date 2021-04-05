package software.wings.cloudprovider;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.AwsConfig;
import software.wings.beans.command.ExecutionLogCallback;

import com.amazonaws.services.ecs.model.ServiceEvent;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@OwnedBy(CDP)
@Data
@Builder
public class UpdateServiceCountRequestData {
  private String region;
  private String cluster;
  private String serviceName;
  private List<ServiceEvent> serviceEvents;
  private AwsConfig awsConfig;
  private ExecutionLogCallback executionLogCallback;
  private List<EncryptedDataDetail> encryptedDataDetails;
  private int desiredCount;
  private Integer timeOut;
}
