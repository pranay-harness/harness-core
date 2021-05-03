package io.harness.delegate.task.artifacts.ecr;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.beans.AwsInternalConfig;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsCredentialType;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.SelectorCapability;
import io.harness.delegate.task.artifacts.ArtifactSourceDelegateRequest;
import io.harness.delegate.task.artifacts.ArtifactSourceType;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.exception.UnknownEnumTypeException;
import io.harness.expression.ExpressionEvaluator;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.Arrays;
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
  /** aws Connector*/
  AwsConnectorDTO awsConnectorDTO;
  /** Encrypted details for decrypting.*/
  List<EncryptedDataDetail> encryptedDataDetails;
  /** Artifact Source type.*/
  ArtifactSourceType sourceType;

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities(ExpressionEvaluator maskingEvaluator) {
    if (awsConnectorDTO.getCredential() != null) {
      if (awsConnectorDTO.getCredential().getAwsCredentialType() == AwsCredentialType.INHERIT_FROM_DELEGATE) {
        if (EmptyPredicate.isNotEmpty(awsConnectorDTO.getDelegateSelectors())) {
          return singletonList(SelectorCapability.builder().selectors(awsConnectorDTO.getDelegateSelectors()).build());
        }
        return emptyList();
      } else if (awsConnectorDTO.getCredential().getAwsCredentialType() == AwsCredentialType.MANUAL_CREDENTIALS) {
        return Arrays.asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
            AwsInternalConfig.AWS_URL, maskingEvaluator));
      } else {
        throw new UnknownEnumTypeException(
            "Ecr Credential Type", String.valueOf(awsConnectorDTO.getCredential().getAwsCredentialType()));
      }
    }
    return emptyList();
  }
}
