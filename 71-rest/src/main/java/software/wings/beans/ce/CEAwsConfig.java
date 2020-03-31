package software.wings.beans.ce;

import static software.wings.audit.ResourceType.CE_CONNECTOR;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsCrossAccountAttributes;
import software.wings.settings.SettingValue;

import java.util.List;

@JsonTypeName("CE_AWS")
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class CEAwsConfig extends SettingValue {
  private String s3BucketName;
  private String awsAccountId;
  private String curReportName;
  private String awsAccountType;
  private String awsMasterAccountId;
  private String masterAccountSettingId;
  private AwsCrossAccountAttributes awsCrossAccountAttributes;

  @Override
  public String fetchResourceCategory() {
    return CE_CONNECTOR.name();
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return null;
  }

  public CEAwsConfig() {
    super(SettingVariableTypes.CE_AWS.name());
  }

  public CEAwsConfig(String s3BucketName, String awsAccountId, String curReportName, String awsAccountType,
      String awsMasterAccountId, String masterAccountSettingId, AwsCrossAccountAttributes awsCrossAccountAttributes) {
    this();
    this.s3BucketName = s3BucketName;
    this.awsAccountId = awsAccountId;
    this.curReportName = curReportName;
    this.awsAccountType = awsAccountType;
    this.awsMasterAccountId = awsMasterAccountId;
    this.masterAccountSettingId = masterAccountSettingId;
    this.awsCrossAccountAttributes = awsCrossAccountAttributes;
  }

  public enum AWSAccountType { MASTER_ACCOUNT, LINKED_ACCOUNT }
}
