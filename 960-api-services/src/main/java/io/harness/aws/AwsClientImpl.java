package io.harness.aws;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.StringUtils.defaultString;

import io.harness.exception.InvalidRequestException;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.EC2ContainerCredentialsProviderWrapper;
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider;
import com.amazonaws.auth.policy.Policy;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.codecommit.AWSCodeCommitClient;
import com.amazonaws.services.codecommit.AWSCodeCommitClientBuilder;
import com.amazonaws.services.codecommit.model.AWSCodeCommitException;
import com.amazonaws.services.codecommit.model.GetRepositoryRequest;
import com.amazonaws.services.codecommit.model.ListRepositoriesRequest;
import com.amazonaws.services.costandusagereport.AWSCostAndUsageReport;
import com.amazonaws.services.costandusagereport.AWSCostAndUsageReportClientBuilder;
import com.amazonaws.services.costandusagereport.model.DescribeReportDefinitionsRequest;
import com.amazonaws.services.costandusagereport.model.DescribeReportDefinitionsResult;
import com.amazonaws.services.costandusagereport.model.ReportDefinition;
import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ecr.AmazonECRClient;
import com.amazonaws.services.ecr.AmazonECRClientBuilder;
import com.amazonaws.services.ecr.model.GetAuthorizationTokenRequest;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClientBuilder;
import com.amazonaws.services.identitymanagement.model.EvaluationResult;
import com.amazonaws.services.identitymanagement.model.GetRolePolicyRequest;
import com.amazonaws.services.identitymanagement.model.GetRolePolicyResult;
import com.amazonaws.services.identitymanagement.model.ListRolePoliciesRequest;
import com.amazonaws.services.identitymanagement.model.ListRolePoliciesResult;
import com.amazonaws.services.identitymanagement.model.SimulatePrincipalPolicyRequest;
import com.amazonaws.services.identitymanagement.model.SimulatePrincipalPolicyResult;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.securitytoken.AWSSecurityTokenService;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.google.common.annotations.VisibleForTesting;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.hazelcast.util.CollectionUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

@Singleton
@Slf4j
public class AwsClientImpl implements AwsClient {
  @Inject protected AwsCallTracker tracker;

  private static final Regions DEFAULT_REGION = Regions.US_EAST_1;

  @Override
  public void validateAwsAccountCredential(AwsConfig awsConfig) {
    try {
      tracker.trackEC2Call("Get Ec2 client");
      getAmazonEc2Client(awsConfig).describeRegions();
    } catch (AmazonEC2Exception amazonEC2Exception) {
      if (amazonEC2Exception.getStatusCode() == 401) {
        if (!awsConfig.isEc2IamCredentials()) {
          if (isEmpty(awsConfig.getAwsAccessKeyCredential().getAccessKey())) {
            throw new InvalidRequestException("Access Key should not be empty");
          } else if (isEmpty(awsConfig.getAwsAccessKeyCredential().getSecretKey())) {
            throw new InvalidRequestException("Secret Key should not be empty");
          }
        }
      }
      throw amazonEC2Exception;
    }
  }

  @Override
  public void validateAwsCodeCommitCredential(AwsConfig awsConfig, String region, String repo) {
    try {
      tracker.trackEC2Call("Get CodeCommit client");
      AWSCodeCommitClient amazonCodeCommitClient = getAmazonCodeCommitClient(awsConfig, region);
      if (isNotEmpty(repo)) {
        amazonCodeCommitClient.getRepository(new GetRepositoryRequest().withRepositoryName(repo));
      } else {
        amazonCodeCommitClient.listRepositories(new ListRepositoriesRequest());
      }
    } catch (AWSCodeCommitException awsCodeCommitException) {
      if (awsCodeCommitException.getStatusCode() == 401) {
        if (!awsConfig.isEc2IamCredentials()) {
          if (isEmpty(awsConfig.getAwsAccessKeyCredential().getAccessKey())) {
            throw new InvalidRequestException("Access Key should not be empty");
          } else if (isEmpty(awsConfig.getAwsAccessKeyCredential().getSecretKey())) {
            throw new InvalidRequestException("Secret Key should not be empty");
          }
        }
      }
      throw awsCodeCommitException;
    }
  }

  @Override
  public String getAmazonEcrAuthToken(AwsConfig awsConfig, String account, String region) {
    try {
      AmazonECRClient amazonECRClient = getAmazonEcrClient(region, awsConfig);
      tracker.trackECRCall("Get Auth Token");
      return amazonECRClient
          .getAuthorizationToken(new GetAuthorizationTokenRequest().withRegistryIds(singletonList(account)))
          .getAuthorizationData()
          .get(0)
          .getAuthorizationToken();
    } catch (AmazonEC2Exception amazonEC2Exception) {
      if (amazonEC2Exception.getStatusCode() == 401) {
        if (!awsConfig.isEc2IamCredentials()) {
          if (isEmpty(awsConfig.getAwsAccessKeyCredential().getAccessKey())) {
            throw new InvalidRequestException("Access Key should not be empty");
          } else if (isEmpty(awsConfig.getAwsAccessKeyCredential().getSecretKey())) {
            throw new InvalidRequestException("Secret Key should not be empty");
          }
        }
      }
      throw amazonEC2Exception;
    }
  }

  @VisibleForTesting
  AmazonEC2Client getAmazonEc2Client(AwsConfig awsConfig) {
    AWSCredentialsProvider credentialsProvider = getCredentialProvider(awsConfig);
    return (AmazonEC2Client) AmazonEC2ClientBuilder.standard()
        .withRegion(DEFAULT_REGION)
        .withCredentials(credentialsProvider)
        .build();
  }

  @VisibleForTesting
  AWSCodeCommitClient getAmazonCodeCommitClient(AwsConfig awsConfig, String region) {
    AWSCredentialsProvider credentialsProvider = getCredentialProvider(awsConfig);
    return (AWSCodeCommitClient) AWSCodeCommitClientBuilder.standard()
        .withRegion(region)
        .withCredentials(credentialsProvider)
        .build();
  }

  @VisibleForTesting
  AmazonECRClient getAmazonEcrClient(String region, AwsConfig awsConfig) {
    AWSCredentialsProvider credentialsProvider = getCredentialProvider(awsConfig);
    return (AmazonECRClient) AmazonECRClientBuilder.standard()
        .withRegion(region)
        .withCredentials(credentialsProvider)
        .build();
  }

  protected AWSCredentialsProvider getCredentialProvider(AwsConfig awsConfig) {
    AWSCredentialsProvider credentialsProvider;
    if (awsConfig.isEc2IamCredentials()) {
      log.info("Instantiating EC2ContainerCredentialsProviderWrapper");
      credentialsProvider = new EC2ContainerCredentialsProviderWrapper();
    } else {
      credentialsProvider =
          constructStaticBasicAwsCredentials(defaultString(awsConfig.getAwsAccessKeyCredential().getAccessKey(), ""),
              awsConfig.getAwsAccessKeyCredential().getSecretKey() != null
                  ? new String(awsConfig.getAwsAccessKeyCredential().getSecretKey())
                  : "");
    }
    if (awsConfig.getCrossAccountAccess() != null) {
      CrossAccountAccess crossAccountAttributes = awsConfig.getCrossAccountAccess();
      // For the security token service we default to us-east-1.
      credentialsProvider = getAssumedCredentialsProvider(credentialsProvider, crossAccountAttributes);
    }
    return credentialsProvider;
  }

  public AWSCredentialsProvider getAssumedCredentialsProvider(
      AWSCredentialsProvider credentialsProvider, CrossAccountAccess crossAccountAccess) {
    return getAssumedCredentialsProvider(
        credentialsProvider, crossAccountAccess.getCrossAccountRoleArn(), crossAccountAccess.getExternalId());
  }

  @Override
  public AWSCredentialsProvider getAssumedCredentialsProvider(
      AWSCredentialsProvider credentialsProvider, String crossAccountRoleArn, @Nullable String externalId) {
    final AWSSecurityTokenService awsSecurityTokenService = constructAWSSecurityTokenService(credentialsProvider);
    return new STSAssumeRoleSessionCredentialsProvider.Builder(crossAccountRoleArn, UUID.randomUUID().toString())
        .withExternalId(externalId)
        .withStsClient(awsSecurityTokenService)
        .build();
  }

  public AmazonS3Client getAmazonS3Client(AWSCredentialsProvider credentialsProvider) {
    return (AmazonS3Client) AmazonS3ClientBuilder.standard()
        .withRegion(DEFAULT_REGION)
        .withForceGlobalBucketAccessEnabled(Boolean.TRUE)
        .withCredentials(credentialsProvider)
        .build();
  }

  @Override
  public Optional<ReportDefinition> getReportDefinition(
      AWSCredentialsProvider credentialsProvider, String curReportName) {
    AWSCostAndUsageReport awsCostAndUsageReportClient = getAwsCurClient(credentialsProvider);
    DescribeReportDefinitionsRequest describeReportDefinitionsRequest = new DescribeReportDefinitionsRequest();

    String nextToken = null;
    do {
      describeReportDefinitionsRequest.withNextToken(nextToken);
      DescribeReportDefinitionsResult describeReportDefinitionsResult =
          awsCostAndUsageReportClient.describeReportDefinitions(describeReportDefinitionsRequest);
      List<ReportDefinition> reportDefinitionsList = describeReportDefinitionsResult.getReportDefinitions();

      Optional<ReportDefinition> requiredReport =
          reportDefinitionsList.stream().filter(r -> r.getReportName().equals(curReportName)).findFirst();
      if (requiredReport.isPresent()) {
        return requiredReport;
      }

      nextToken = describeReportDefinitionsRequest.getNextToken();
    } while (nextToken != null);
    return Optional.empty();
  }

  protected AmazonIdentityManagement getAwsIAMClient(AWSCredentialsProvider credentialsProvider) {
    return AmazonIdentityManagementClientBuilder.standard()
        .withCredentials(credentialsProvider)
        .withRegion(DEFAULT_REGION)
        .build();
  }

  protected AWSCostAndUsageReport getAwsCurClient(AWSCredentialsProvider credentialsProvider) {
    return AWSCostAndUsageReportClientBuilder.standard()
        .withRegion(DEFAULT_REGION)
        .withCredentials(credentialsProvider)
        .build();
  }

  protected AWSSecurityTokenService constructAWSSecurityTokenService(AWSCredentialsProvider credentialsProvider) {
    return AWSSecurityTokenServiceClientBuilder.standard()
        .withRegion(DEFAULT_REGION)
        .withCredentials(credentialsProvider)
        .build();
  }

  @Override
  public AWSCredentialsProvider constructStaticBasicAwsCredentials(
      @NotNull String accessKey, @NotNull String secretKey) {
    return new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));
  }

  @Override
  public List<String> listRolePolicyNames(
      AWSCredentialsProvider awsCredentialsProvider, @NotNull final String roleName) {
    final AmazonIdentityManagement iam = getAwsIAMClient(awsCredentialsProvider);

    List<String> policyNames = new ArrayList<>();

    final ListRolePoliciesRequest request = new ListRolePoliciesRequest().withRoleName(roleName);
    final ListRolePoliciesResult response = iam.listRolePolicies(request);
    do {
      policyNames.addAll(response.getPolicyNames());
      request.setMarker(response.getMarker());
    } while (Boolean.TRUE.equals(response.getIsTruncated()));

    return policyNames;
  }

  /**
   * @param credentialsProvider the credential (can also be assumed role creds) used while simulating policies.
   * @param policySourceArn the crossRoleArn whose policies needs to be verified
   * @param actionNames the list of actions e.g., "rds:ModifyDBInstance", "rds:*", etc.
   * @param resourceArns the arn of resources e.g., "*", "arn:aws:s3:::customerBucketName". On null defaults to "*"
   * @return EvaluationResultList the results when each actions is performed on each resources.
   */
  @Override
  public List<EvaluationResult> simulatePrincipalPolicy(final AWSCredentialsProvider credentialsProvider,
      @NotNull String policySourceArn, @NotEmpty List<String> actionNames, @Nullable List<String> resourceArns) {
    final AmazonIdentityManagement iam = getAwsIAMClient(credentialsProvider);
    final SimulatePrincipalPolicyRequest request =
        new SimulatePrincipalPolicyRequest().withPolicySourceArn(policySourceArn).withActionNames(actionNames);
    if (CollectionUtil.isNotEmpty(resourceArns)) {
      request.withResourceArns(resourceArns);
    }

    final List<EvaluationResult> evaluationResultList = new ArrayList<>();
    final SimulatePrincipalPolicyResult response = iam.simulatePrincipalPolicy(request);
    do {
      evaluationResultList.addAll(response.getEvaluationResults());
      request.setMarker(response.getMarker());
    } while (Boolean.TRUE.equals(response.getIsTruncated()));
    return evaluationResultList;
  }

  @SneakyThrows
  @Override
  public Policy getRolePolicy(
      AWSCredentialsProvider awsCredentialsProvider, @NotNull final String roleName, @NotNull final String policyName) {
    final AmazonIdentityManagement iam = getAwsIAMClient(awsCredentialsProvider);
    final GetRolePolicyResult result =
        iam.getRolePolicy(new GetRolePolicyRequest().withPolicyName(policyName).withRoleName(roleName));

    final String policyDocumentAsEncodedJson = result.getPolicyDocument();
    return Policy.fromJson(java.net.URLDecoder.decode(policyDocumentAsEncodedJson, "UTF-8"));
  }

  @Override
  public ObjectListing getBucket(
      AWSCredentialsProvider credentialsProvider, @NotNull String s3BucketName, @Nullable String s3Prefix) {
    final AmazonS3Client amazonS3Client = getAmazonS3Client(credentialsProvider);
    return amazonS3Client.listObjects(s3BucketName, s3Prefix);
  }
}
