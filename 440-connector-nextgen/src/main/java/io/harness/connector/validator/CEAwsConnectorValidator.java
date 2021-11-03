package io.harness.connector.validator;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.aws.AwsClient;
import io.harness.ccm.manager.CENextGenResourceClient;
import io.harness.connector.ConnectivityStatus;
import io.harness.connector.ConnectorResponseDTO;
import io.harness.connector.ConnectorValidationResult;
import io.harness.connector.entities.embedded.ceawsconnector.S3BucketDetails;
import io.harness.delegate.beans.connector.CEFeatures;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.awsconnector.CrossAccountAccessDTO;
import io.harness.delegate.beans.connector.ceawsconnector.AwsCurAttributesDTO;
import io.harness.delegate.beans.connector.ceawsconnector.CEAwsConnectorDTO;
import io.harness.delegate.task.TaskParameters;
import io.harness.exception.InvalidArgumentsException;
import io.harness.instanceng.InstanceNGResourceClient;
import io.harness.ng.core.dto.ErrorDetail;
import io.harness.remote.CEAwsSetupConfig;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.policy.Action;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.auth.policy.Resource;
import com.amazonaws.auth.policy.Statement;
import com.amazonaws.services.costandusagereport.model.ReportDefinition;
import com.amazonaws.services.identitymanagement.model.AmazonIdentityManagementException;
import com.amazonaws.services.identitymanagement.model.EvaluationResult;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import com.amazonaws.services.securitytoken.model.AWSSecurityTokenServiceException;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.validation.constraints.NotNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Singleton
@OwnedBy(HarnessTeam.CE)
public class CEAwsConnectorValidator extends AbstractConnectorValidator {
  @Autowired
  private CENextGenResourceClient ceNextGenResourceClient;

  @Override
  public <T extends ConnectorConfigDTO> TaskParameters getTaskParameters(
      T connectorConfig, String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return null;
  }

  @Override
  public String getTaskType() {
    return null;
  }

  @Override
  public ConnectorValidationResult validate(ConnectorConfigDTO connectorDTO, String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    return null;
  }

  @Override
  public ConnectorValidationResult validate(ConnectorResponseDTO connectorResponseDTO, String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier) {
    return (ConnectorValidationResult) ceNextGenResourceClient.testConnection(accountIdentifier, connectorResponseDTO);
  }
}