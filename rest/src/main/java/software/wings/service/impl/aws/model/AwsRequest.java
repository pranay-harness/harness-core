package software.wings.service.impl.aws.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@AllArgsConstructor
public class AwsRequest {
  @NotNull private AwsConfig awsConfig;
  @NotNull private List<EncryptedDataDetail> encryptionDetails;
}