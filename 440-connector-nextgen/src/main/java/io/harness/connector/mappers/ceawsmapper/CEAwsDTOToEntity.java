package io.harness.connector.mappers.ceawsmapper;

import io.harness.connector.entities.embedded.ceawsconnector.CEAwsConfig;
import io.harness.connector.entities.embedded.ceawsconnector.CEAwsConfig.CEAwsConfigBuilder;
import io.harness.connector.entities.embedded.ceawsconnector.CURAttributes;
import io.harness.connector.entities.embedded.ceawsconnector.S3BucketDetails;
import io.harness.connector.mappers.ConnectorDTOToEntityMapper;
import io.harness.delegate.beans.connector.ConnectorCategory;
import io.harness.delegate.beans.connector.awsconnector.CrossAccountAccessDTO;
import io.harness.delegate.beans.connector.ceawsconnector.AwsCurAttributesDTO;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsConnectorDTO;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsFeatures;
import io.harness.exception.InvalidRequestException;

import com.amazonaws.arn.Arn;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.List;

@Singleton
public class CEAwsDTOToEntity implements ConnectorDTOToEntityMapper<CEAwsConnectorDTO> {
  @Override
  public CEAwsConfig toConnectorEntity(CEAwsConnectorDTO connectorDTO) {
    CEAwsConfigBuilder ceAwsConfigBuilder = CEAwsConfig.builder();

    List<CEAwsFeatures> featuresList = connectorDTO.getFeaturesEnabled();
    if (featuresList.contains(CEAwsFeatures.CUR)) {
      final AwsCurAttributesDTO awsCurAttributes = connectorDTO.getCurAttributes();

      if (awsCurAttributes == null) {
        throw new InvalidRequestException("AwsCurAttributes can't be null when the features 'CUR' is enabled.");
      }

      // TODO (UTSAV) : fix fetching s3bucket region and prefix using network call.
      final S3BucketDetails s3BucketDetails = S3BucketDetails.builder()
                                                  .region("region")
                                                  .s3Prefix("s3Prefix")
                                                  .s3BucketName(awsCurAttributes.getS3BucketName())
                                                  .build();

      final CURAttributes curAttributes =
          CURAttributes.builder().reportName(awsCurAttributes.getReportName()).s3BucketDetails(s3BucketDetails).build();
      ceAwsConfigBuilder.curAttributes(curAttributes);
    }

    final CrossAccountAccessDTO crossAccountAccessDTO = connectorDTO.getCrossAccountAccess();

    return ceAwsConfigBuilder.crossAccountAccess(crossAccountAccessDTO)
        .awsAccountId(getAccountId(crossAccountAccessDTO))
        .featuresEnabled(featuresList)
        .build();
  }

  private static String getAccountId(final CrossAccountAccessDTO crossAccountAccessDTO) {
    Arn roleArn = Arn.fromString(crossAccountAccessDTO.getCrossAccountRoleArn());
    return roleArn.getAccountId();
  }

  @Override
  public List<ConnectorCategory> getConnectorCategory() {
    return Collections.singletonList(ConnectorCategory.CLOUD_COST);
  }
}
