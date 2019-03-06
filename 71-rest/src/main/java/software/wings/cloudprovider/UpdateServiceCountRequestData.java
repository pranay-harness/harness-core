package software.wings.cloudprovider;

import com.amazonaws.services.ecs.model.ServiceEvent;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.AwsConfig;
import software.wings.beans.command.ExecutionLogCallback;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

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
}
