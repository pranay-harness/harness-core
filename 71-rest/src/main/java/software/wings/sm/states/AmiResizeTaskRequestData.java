package software.wings.sm.states;

import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import software.wings.api.AmiServiceSetupElement;
import software.wings.beans.AwsConfig;
import software.wings.service.impl.aws.model.AwsAmiResizeData;
import software.wings.sm.ExecutionContext;

import java.util.List;

@Data
@Builder
public class AmiResizeTaskRequestData {
  ExecutionContext context;
  private String appId;
  private String accountId;
  private String envId;
  private String activityId;
  private String region;
  private String commandName;
  private AwsConfig awsConfig;
  private List<EncryptedDataDetail> encryptionDetails;
  private boolean resizeNewFirst;
  private String newAutoScalingGroupName;
  private Integer newAsgFinalDesiredCount;
  private List<AwsAmiResizeData> resizeData;
  private List<String> classicLBs;
  private List<String> targetGroupArns;
  private boolean rollback;
  private AmiServiceSetupElement serviceSetupElement;
}
