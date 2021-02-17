package io.harness.connector.mappers.ceawsmapper;

import io.harness.connector.entities.embedded.ceawsconnector.CEAwsConfig;
import io.harness.connector.entities.embedded.ceawsconnector.CURAttributes;
import io.harness.connector.mappers.ConnectorEntityToDTOMapper;
import io.harness.delegate.beans.connector.ceawsconnector.AwsCurAttributesDTO;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsConnectorDTO;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsConnectorDTO.CEAwsConnectorDTOBuilder;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsFeatures;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.AllArgsConstructor;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
public class CEAwsEntityToDTO implements ConnectorEntityToDTOMapper<CEAwsConnectorDTO, CEAwsConfig> {
  @Override
  public CEAwsConnectorDTO createConnectorDTO(CEAwsConfig ceAwsConfig) {
    CEAwsConnectorDTOBuilder ceAwsConnectorDTOBuilder = CEAwsConnectorDTO.builder();

    List<CEAwsFeatures> ceAwsFeaturesList = ceAwsConfig.getFeaturesEnabled();
    if (ceAwsFeaturesList.contains(CEAwsFeatures.CUR)) {
      final CURAttributes curAttributes = ceAwsConfig.getCurAttributes();
      ceAwsConnectorDTOBuilder.curAttributes(AwsCurAttributesDTO.builder()
                                                 .s3BucketName(curAttributes.getS3BucketDetails().getS3BucketName())
                                                 .reportName(curAttributes.getReportName())
                                                 .build());
    }

    return ceAwsConnectorDTOBuilder.crossAccountAccess(ceAwsConfig.getCrossAccountAccess())
        .featuresEnabled(ceAwsFeaturesList)
        .build();
  }
}
