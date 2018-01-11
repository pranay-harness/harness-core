package software.wings.helpers.ext.amazons3;

import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsConfig;
import software.wings.common.Constants;
import software.wings.delegatetasks.collect.artifacts.ArtifactCollectionTaskHelper;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.waitnotify.ListNotifyResponseData;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author rktummala 07/30/17
 */
public class AmazonS3ServiceImpl implements AmazonS3Service {
  private static final Logger logger = LoggerFactory.getLogger(AmazonS3ServiceImpl.class);
  @Inject AwsHelperService awsHelperService;
  @Inject private ArtifactCollectionTaskHelper artifactCollectionTaskHelper;
  private static final int MAX_FILES_TO_SHOW_IN_UI = 1000;
  private static final int FETCH_FILE_COUNT_IN_BUCKET = 500;

  @Override
  public Map<String, String> getBuckets(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails) {
    List<Bucket> bucketList = awsHelperService.listS3Buckets(awsConfig, encryptionDetails);
    return bucketList.stream().collect(Collectors.toMap(Bucket::getName, Bucket::getName, (a, b) -> b));
  }

  @Override
  public List<String> getArtifactPaths(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String bucketName) {
    ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request();
    listObjectsV2Request.withBucketName(bucketName).withMaxKeys(FETCH_FILE_COUNT_IN_BUCKET);
    ListObjectsV2Result result;
    List<String> objectKeyList = Lists.newArrayList();

    do {
      result = awsHelperService.listObjectsInS3(awsConfig, encryptionDetails, listObjectsV2Request);
      List<S3ObjectSummary> objectSummaryList = result.getObjectSummaries();
      // in descending order. The most recent one comes first
      Collections.sort(objectSummaryList, (o1, o2) -> o2.getLastModified().compareTo(o1.getLastModified()));

      List<String> objectKeyListForCurrentBatch = objectSummaryList.stream()
                                                      .filter(objectSummary -> !objectSummary.getKey().endsWith("/"))
                                                      .map(S3ObjectSummary::getKey)
                                                      .collect(Collectors.toList());
      objectKeyList.addAll(objectKeyListForCurrentBatch);
      listObjectsV2Request.setContinuationToken(result.getNextContinuationToken());
    } while (result.isTruncated() == true && objectKeyList.size() < MAX_FILES_TO_SHOW_IN_UI);

    return objectKeyList;
  }

  private String getPrefix(String artifactPath) {
    int index = artifactPath.indexOf('*');
    String prefix = null;
    if (index != -1) {
      prefix = artifactPath.substring(0, index);
    }
    return prefix;
  }

  @Override
  public List<BuildDetails> getArtifactsBuildDetails(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String bucketName, List<String> artifactPaths, boolean isExpression) {
    boolean versioningEnabledForBucket =
        awsHelperService.isVersioningEnabledForBucket(awsConfig, encryptionDetails, bucketName);
    List<BuildDetails> buildDetailsList = Lists.newArrayList();
    for (String artifactPath : artifactPaths) {
      List<BuildDetails> buildDetailsListForArtifactPath = getArtifactsBuildDetails(
          awsConfig, encryptionDetails, bucketName, artifactPath, isExpression, versioningEnabledForBucket);
      buildDetailsList.addAll(buildDetailsListForArtifactPath);
    }
    return buildDetailsList;
  }

  private List<BuildDetails> getArtifactsBuildDetails(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String bucketName, String artifactPath, boolean isExpression, boolean versioningEnabledForBucket) {
    List<BuildDetails> buildDetailsList = Lists.newArrayList();
    if (isExpression) {
      ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request();

      String prefix = getPrefix(artifactPath);
      if (prefix != null) {
        listObjectsV2Request.withPrefix(prefix);
      }

      listObjectsV2Request.withBucketName(bucketName).withMaxKeys(FETCH_FILE_COUNT_IN_BUCKET);
      List<String> objectKeyList = Lists.newArrayList();
      ListObjectsV2Result result;

      Pattern pattern = Pattern.compile(artifactPath.replace(".", "\\.").replace("?", ".?").replace("*", ".*?"));

      do {
        result = awsHelperService.listObjectsInS3(awsConfig, encryptionDetails, listObjectsV2Request);
        List<S3ObjectSummary> objectSummaryList = result.getObjectSummaries();
        // in descending order. The most recent one comes first
        Collections.sort(objectSummaryList, (o1, o2) -> o2.getLastModified().compareTo(o1.getLastModified()));

        List<String> objectKeyListForCurrentBatch =
            objectSummaryList.stream()
                .filter(objectSummary
                    -> !objectSummary.getKey().endsWith("/") && pattern.matcher(objectSummary.getKey()).find())
                .map(S3ObjectSummary::getKey)
                .collect(Collectors.toList());
        objectKeyList.addAll(objectKeyListForCurrentBatch);
        listObjectsV2Request.setContinuationToken(result.getNextContinuationToken());
      } while (result.isTruncated() == true);

      for (String key : objectKeyList) {
        BuildDetails artifactMetadata =
            getArtifactBuildDetails(awsConfig, encryptionDetails, bucketName, key, versioningEnabledForBucket);
        buildDetailsList.add(artifactMetadata);
      }

    } else {
      BuildDetails artifactMetadata =
          getArtifactBuildDetails(awsConfig, encryptionDetails, bucketName, artifactPath, versioningEnabledForBucket);
      buildDetailsList.add(artifactMetadata);
    }

    return buildDetailsList;
  }

  @Override
  public ListNotifyResponseData downloadArtifacts(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String bucketName, List<String> artifactPaths, String delegateId, String taskId, String accountId)
      throws IOException, URISyntaxException {
    ListNotifyResponseData res = new ListNotifyResponseData();

    for (String artifactPath : artifactPaths) {
      downloadArtifactsUsingFilter(
          awsConfig, encryptionDetails, bucketName, artifactPath, res, delegateId, taskId, accountId);
    }
    return res;
  }

  private void downloadArtifactsUsingFilter(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String bucketName, String artifactpathRegex, ListNotifyResponseData res, String delegateId, String taskId,
      String accountId) throws IOException, URISyntaxException {
    Pattern pattern = Pattern.compile(artifactpathRegex.replace(".", "\\.").replace("?", ".?").replace("*", ".*?"));

    ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request();
    String prefix = getPrefix(artifactpathRegex);
    if (prefix != null) {
      listObjectsV2Request.withPrefix(prefix);
    }

    listObjectsV2Request.withBucketName(bucketName).withMaxKeys(FETCH_FILE_COUNT_IN_BUCKET);

    List<String> objectKeyList = Lists.newArrayList();
    ListObjectsV2Result result;
    do {
      result = awsHelperService.listObjectsInS3(awsConfig, encryptionDetails, listObjectsV2Request);
      List<S3ObjectSummary> objectSummaryList = result.getObjectSummaries();
      // in descending order. The most recent one comes first
      Collections.sort(objectSummaryList, (o1, o2) -> o2.getLastModified().compareTo(o1.getLastModified()));

      List<String> objectKeyListForCurrentBatch =
          objectSummaryList.stream()
              .filter(objectSummary
                  -> !objectSummary.getKey().endsWith("/") && pattern.matcher(objectSummary.getKey()).find())
              .map(S3ObjectSummary::getKey)
              .collect(Collectors.toList());
      objectKeyList.addAll(objectKeyListForCurrentBatch);
      listObjectsV2Request.setContinuationToken(result.getNextContinuationToken());
    } while (result.isTruncated() == true);

    // We are not using stream here since addDataToResponse throws a bunch of exceptions and we want to throw them back
    // to the caller.
    for (String objectKey : objectKeyList) {
      Pair<String, InputStream> stringInputStreamPair =
          downloadArtifact(awsConfig, encryptionDetails, bucketName, objectKey);
      artifactCollectionTaskHelper.addDataToResponse(
          stringInputStreamPair, artifactpathRegex, res, delegateId, taskId, accountId);
    }
  }

  private Pair<String, InputStream> downloadArtifact(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String bucketName, String key) {
    S3Object object = awsHelperService.getObjectFromS3(awsConfig, encryptionDetails, bucketName, key);
    if (object != null) {
      return Pair.of(object.getKey(), object.getObjectContent());
    }
    return null;
  }

  @Override
  public BuildDetails getArtifactBuildDetails(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String bucketName, String key, boolean versioningEnabledForBucket) {
    Map<String, String> map = new HashMap<>();
    String versionId = null;
    String resourceUrl =
        new StringBuilder("https://s3.amazonaws.com/").append(bucketName).append("/").append(key).toString();
    if (versioningEnabledForBucket) {
      ObjectMetadata objectMetadata =
          awsHelperService.getObjectMetadataFromS3(awsConfig, encryptionDetails, bucketName, key);

      if (objectMetadata != null) {
        versionId = key + ":" + objectMetadata.getVersionId();
      }
    }
    if (versionId == null) {
      versionId = key;
    }
    map.put(Constants.URL, resourceUrl);
    map.put(Constants.BUILD_NO, versionId);
    map.put(Constants.BUCKET_NAME, bucketName);
    map.put(Constants.ARTIFACT_PATH, key);
    map.put(Constants.KEY, key);

    return aBuildDetails()
        .withNumber(versionId)
        .withRevision(versionId)
        .withArtifactPath(key)
        .withBuildParameters(map)
        .build();
  }
}
