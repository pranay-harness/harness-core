package software.wings.service.impl.aws.model;

import static software.wings.service.impl.aws.model.AwsIamRequest.AwsIamRequestType.LIST_IAM_INSTANCE_ROLES;

import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsIamListInstanceRolesRequest extends AwsIamRequest {
  @Builder
  public AwsIamListInstanceRolesRequest(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    super(awsConfig, encryptionDetails, LIST_IAM_INSTANCE_ROLES);
  }
}
