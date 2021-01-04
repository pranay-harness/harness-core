package io.harness.delegate.task.citasks.cik8handler;

import static io.harness.aws.AwsExceptionHandler.handleAmazonClientException;
import static io.harness.aws.AwsExceptionHandler.handleAmazonServiceException;

import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import io.harness.aws.AwsClient;
import io.harness.aws.AwsConfig;
import io.harness.delegate.beans.ci.pod.ConnectorDetails;
import io.harness.delegate.beans.ci.pod.ImageDetailsWithConnector;
import io.harness.delegate.beans.connector.ConnectorType;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerAuthType;
import io.harness.delegate.beans.connector.docker.DockerConnectorDTO;
import io.harness.delegate.beans.connector.docker.DockerUserNamePasswordDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.beans.connector.gcpconnector.GcpCredentialType;
import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.harness.delegate.task.aws.AwsNgConfigMapper;
import io.harness.exception.InvalidArgumentsException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.SecretDecryptionService;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;

@Singleton
@Slf4j
public class ImageSecretBuilder {
  private static final String BASE_GCR_HOSTNAME = "gcr.io";
  private static final String GCR_USERNAME = "_json_key";
  private static final String BASE_ECR_HOSTNAME = "amazonaws.com";
  private static final String HTTPS_URL = "https://";
  private static final String PATH_SEPARATOR = "/";
  private static final String DOT_SEPARATOR = "\\.";
  private static final String USERNAME = "username";
  private static final String PASSWORD = "password";
  private static final String AUTH = "auth";

  @Inject private SecretDecryptionService secretDecryptionService;
  @Inject private AwsNgConfigMapper awsNgConfigMapper;
  @Inject private AwsClient awsClient;

  public String getJSONEncodedImageCredentials(ImageDetailsWithConnector imageDetailsWithConnector) {
    ConnectorDetails connectorDetails = imageDetailsWithConnector.getImageConnectorDetails();
    if (connectorDetails == null) {
      return null;
    }

    String imageName = imageDetailsWithConnector.getImageDetails().getName();
    if (connectorDetails.getConnectorType() == ConnectorType.DOCKER) {
      return getJSONEncodedDockerCredentials(imageName, connectorDetails);
    } else if (connectorDetails.getConnectorType() == ConnectorType.GCP) {
      return getJSONEncodedGCRCredentials(imageName, connectorDetails);
    } else if (connectorDetails.getConnectorType() == ConnectorType.AWS) {
      return getJSONEncodedECRCredentials(imageName, connectorDetails);
    }
    return null;
  }

  private String getJSONEncodedDockerCredentials(String imageName, ConnectorDetails connectorDetails) {
    DockerConnectorDTO dockerConfig = (DockerConnectorDTO) connectorDetails.getConnectorConfig();
    if (dockerConfig.getAuth().getAuthType() != DockerAuthType.USER_PASSWORD) {
      throw new InvalidArgumentsException(format("Invalid auth type: %s for docker image: %s",
                                              dockerConfig.getAuth().getAuthType().toString(), imageName),
          WingsException.USER);
    }

    DockerUserNamePasswordDTO dockerUserNamePasswordDTO = (DockerUserNamePasswordDTO) secretDecryptionService.decrypt(
        dockerConfig.getAuth().getCredentials(), connectorDetails.getEncryptedDataDetails());

    String registryUrl = dockerConfig.getDockerRegistryUrl();
    String username = dockerUserNamePasswordDTO.getUsername();
    String password = String.valueOf(dockerUserNamePasswordDTO.getPasswordRef().getDecryptedValue());
    if (isEmpty(registryUrl) || isEmpty(username) || isEmpty(password)) {
      return null;
    }

    return jsonEncodeCredentials(registryUrl, username, password);
  }

  private String getJSONEncodedGCRCredentials(String imageName, ConnectorDetails connectorDetails) {
    // Image name is of format: HOST-NAME/PROJECT-ID/IMAGE. HOST-NAME is registry url.
    String[] imageParts = imageName.split(PATH_SEPARATOR);
    if (imageParts.length == 0 || !imageParts[0].endsWith(BASE_GCR_HOSTNAME)) {
      throw new InvalidArgumentsException(
          format("Invalid image: %s for GCR connector", imageName), WingsException.USER);
    }

    String registryUrl = imageParts[0];
    GcpConnectorDTO gcpConnectorConfig = (GcpConnectorDTO) connectorDetails.getConnectorConfig();
    if (gcpConnectorConfig.getCredential().getGcpCredentialType() != GcpCredentialType.MANUAL_CREDENTIALS) {
      throw new InvalidArgumentsException(
          format("Invalid auth type: %s for GCR image: %s",
              gcpConnectorConfig.getCredential().getGcpCredentialType().toString(), imageName),
          WingsException.USER);
    }

    GcpManualDetailsDTO credentialConfig = (GcpManualDetailsDTO) secretDecryptionService.decrypt(
        (GcpManualDetailsDTO) gcpConnectorConfig.getCredential().getConfig(),
        connectorDetails.getEncryptedDataDetails());
    String password = String.valueOf(credentialConfig.getSecretKeyRef().getDecryptedValue());
    String username = GCR_USERNAME;
    if (isEmpty(registryUrl) || isEmpty(username) || isEmpty(password)) {
      return null;
    }

    return jsonEncodeCredentials(registryUrl, username, password);
  }

  private String getJSONEncodedECRCredentials(String imageName, ConnectorDetails connectorDetails) {
    // Image name is of format: <account-ID>.dkr.ecr.<region>.amazonaws.com/image
    if (imageName.startsWith(HTTPS_URL)) {
      imageName = imageName.substring(HTTPS_URL.length());
    }

    String[] imageParts = imageName.split(PATH_SEPARATOR);
    if (imageParts.length == 0 || !imageParts[0].endsWith(BASE_ECR_HOSTNAME)) {
      throw new InvalidArgumentsException(
          format("Invalid image: %s for ECR connector", imageName), WingsException.USER);
    }

    String registry = imageParts[0];
    String[] registryParts = registry.split(DOT_SEPARATOR);
    if (registryParts.length != 6) {
      throw new InvalidArgumentsException(
          format("Invalid image: %s for ECR connector", imageName), WingsException.USER);
    }
    String account = registryParts[0];
    String region = registryParts[3];
    String registryUrl = format("%s.dkr.ecr.%s.amazonaws.com", account, region);

    AwsConnectorDTO awsConnectorConfig = (AwsConnectorDTO) connectorDetails.getConnectorConfig();
    final AwsConfig awsConfig = awsNgConfigMapper.mapAwsConfigWithDecryption(awsConnectorConfig.getCredential(),
        awsConnectorConfig.getCredential().getAwsCredentialType(), connectorDetails.getEncryptedDataDetails());
    try {
      String token = awsClient.getAmazonEcrAuthToken(awsConfig, account, region);
      // Token is base64 encoded username:password
      return new JSONObject().put(registryUrl, new JSONObject().put(AUTH, token)).toString();
    } catch (AmazonEC2Exception amazonEC2Exception) {
      handleAmazonServiceException(amazonEC2Exception);
    } catch (AmazonClientException amazonClientException) {
      handleAmazonClientException(amazonClientException);
    }

    return null;
  }

  private String jsonEncodeCredentials(String registryUrl, String username, String password) {
    return new JSONObject()
        .put(registryUrl, new JSONObject().put(USERNAME, username).put(PASSWORD, password))
        .toString();
  }
}
