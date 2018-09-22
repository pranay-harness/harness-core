package software.wings.service.impl.aws.model;

import static software.wings.service.impl.aws.model.AwsIamRequest.AwsIamRequestType.LIST_IAM_ROLES;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsIamListRolesRequest extends AwsIamRequest {
  @Builder
  public AwsIamListRolesRequest(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    super(awsConfig, encryptionDetails, LIST_IAM_ROLES);
  }
}