package io.harness.delegate.task.artifacts.ecr;

import static io.harness.delegate.beans.connector.ConnectorCapabilityBaseHelper.populateDelegateSelectorCapability;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.beans.AwsInternalConfig;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.capability.EncryptedDataDetailsCapabilityHelper;
import io.harness.delegate.task.artifacts.ArtifactSourceDelegateRequest;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Value;

@Value
@Builder
@EqualsAndHashCode(callSuper = false)
@OwnedBy(HarnessTeam.PIPELINE)
public class EcrArtifactDelegateRequest implements ArtifactSourceDelegateRequest {
  /** Images in repos need to be referenced via a path. */
  String imagePath;
  /** Tag refers to exact tag number. */
  String tag;
  /** Tag regex is used to get latest build from builds matching regex. */
  String tagRegex;
  /** List of buildNumbers/tags */
  List<String> tagsList;
  /** Region */
  String region;
  String connectorRef;
  /** aws Connector*/
  AwsConnectorDTO awsConnectorDTO;
  /** Encrypted details for decrypting.*/
  List<EncryptedDataDetail> encryptedDataDetails;
  /** Artifact Source type.*/
  ArtifactSourceType sourceType;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    List<ExecutionCapability> capabilities =
        new ArrayList<>(EncryptedDataDetailsCapabilityHelper.fetchExecutionCapabilitiesForEncryptedDataDetails(
            encryptedDataDetails, maskingEvaluator));
    if (awsConnectorDTO.getCredential() != null) {
      if (awsConnectorDTO.getCredential().getAwsCredentialType() == AwsCredentialType.INHERIT_FROM_DELEGATE) {
        populateDelegateSelectorCapability(capabilities, awsConnectorDTO.getDelegateSelectors());
      } else if (awsConnectorDTO.getCredential().getAwsCredentialType() == AwsCredentialType.MANUAL_CREDENTIALS) {
        populateDelegateSelectorCapability(capabilities, awsConnectorDTO.getDelegateSelectors());
        capabilities.add(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
            AwsInternalConfig.AWS_URL, maskingEvaluator));
      } else {
        throw new UnknownEnumTypeException(
            "Ecr Credential Type", String.valueOf(awsConnectorDTO.getCredential().getAwsCredentialType()));
      }
    }
    return capabilities;
  }
}
