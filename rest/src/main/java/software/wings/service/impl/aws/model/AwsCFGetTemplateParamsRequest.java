package software.wings.service.impl.aws.model;

import static software.wings.service.impl.aws.model.AwsCFRequest.AwsCFRequestType.GET_TEMPLATE_PARAMETERS;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.AwsConfig;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsCFGetTemplateParamsRequest extends AwsCFRequest {
  @NotNull private String type;
  @NotNull private String data;

  @Builder
  public AwsCFGetTemplateParamsRequest(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String data, String region, String type) {
    super(awsConfig, encryptionDetails, GET_TEMPLATE_PARAMETERS, region);
    this.data = data;
    this.type = type;
  }
}