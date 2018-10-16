package software.wings.helpers.ext.amazons3;

import static io.harness.eraro.ErrorCode.INVALID_ARTIFACT_SERVER;
import static io.harness.exception.WingsException.USER;
import static java.util.Collections.sort;
import static java.util.stream.Collectors.toList;
import static software.wings.common.Constants.ARTIFACT_FILE_SIZE;
import static software.wings.common.Constants.ARTIFACT_PATH;
import static software.wings.common.Constants.BUCKET_NAME;
import static software.wings.common.Constants.BUILD_NO;
import static software.wings.common.Constants.KEY;
import static software.wings.common.Constants.URL;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsConfig;
import software.wings.delegatetasks.collect.artifacts.ArtifactCollectionTaskHelper;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.AwsHelperService;
import software.wings.utils.Misc;
import software.wings.waitnotify.ListNotifyResponseData;

import java.io.IOException;
import java.io.InputStream;
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
      sortDescending(objectSummaryList);

      List<String> objectKeyListForCurrentBatch = objectSummaryList.stream()
                                                      .filter(objectSummary -> !objectSummary.getKey().endsWith("/"))
                                                      .map(S3ObjectSummary::getKey)
                                                      .collect(toList());
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
    try {
      boolean versioningEnabledForBucket =
          awsHelperService.isVersioningEnabledForBucket(awsConfig, encryptionDetails, bucketName);
      List<BuildDetails> buildDetailsList = Lists.newArrayList();
      for (String artifactPath : artifactPaths) {
        List<BuildDetails> buildDetailsListForArtifactPath = getArtifactsBuildDetails(
            awsConfig, encryptionDetails, bucketName, artifactPath, isExpression, versioningEnabledForBucket);
        buildDetailsList.addAll(buildDetailsListForArtifactPath);
      }
      return buildDetailsList;
    } catch (Exception e) {
      throw new WingsException(INVALID_ARTIFACT_SERVER, USER, e).addParam("message", Misc.getMessage(e));
    }
  }

  private List<BuildDetails> getArtifactsBuildDetails(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String bucketName, String artifactPath, boolean isExpression, boolean versioningEnabledForBucket) {
    List<BuildDetails> buildDetailsList = Lists.newArrayList();
    if (isExpression) {
      ListObjectsV2Request listObjectsV2Request = getListObjectsV2Request(bucketName, artifactPath);
      ListObjectsV2Result result;
      Pattern pattern = Pattern.compile(artifactPath.replace(".", "\\.").replace("?", ".?").replace("*", ".*?"));

      do {
        result = awsHelperService.listObjectsInS3(awsConfig, encryptionDetails, listObjectsV2Request);
        List<S3ObjectSummary> objectSummaryList = result.getObjectSummaries();
        sortDescending(objectSummaryList);

        List<BuildDetails> pageBuildDetails =
            getObjectSummaries(pattern, objectSummaryList, awsConfig, encryptionDetails, versioningEnabledForBucket);
        buildDetailsList.addAll(pageBuildDetails);
        listObjectsV2Request.setContinuationToken(result.getNextContinuationToken());
      } while (result.isTruncated() == true);
    } else {
      long size = 0;
      ObjectMetadata objectMetadata =
          awsHelperService.getObjectMetadataFromS3(awsConfig, encryptionDetails, bucketName, artifactPath);
      if (objectMetadata != null) {
        size = objectMetadata.getContentLength();
      }

      BuildDetails artifactMetadata = getArtifactBuildDetails(
          awsConfig, encryptionDetails, bucketName, artifactPath, versioningEnabledForBucket, size);
      buildDetailsList.add(artifactMetadata);
    }

    return buildDetailsList;
  }

  private void sortDescending(List<S3ObjectSummary> objectSummaryList) {
    // in descending order. The most recent one comes first
    sort(objectSummaryList, (o1, o2) -> o2.getLastModified().compareTo(o1.getLastModified()));
  }

  private List<String> getObjectSummaries(Pattern pattern, List<S3ObjectSummary> objectSummaryList) {
    return objectSummaryList.stream()
        .filter(
            objectSummary -> !objectSummary.getKey().endsWith("/") && pattern.matcher(objectSummary.getKey()).find())
        .map(S3ObjectSummary::getKey)
        .collect(toList());
  }

  private List<BuildDetails> getObjectSummaries(Pattern pattern, List<S3ObjectSummary> objectSummaryList,
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptedDataDetails, boolean versioningEnabled) {
    return objectSummaryList.stream()
        .filter(
            objectSummary -> !objectSummary.getKey().endsWith("/") && pattern.matcher(objectSummary.getKey()).find())
        .map(objectSummary
            -> getArtifactBuildDetails(awsConfig, encryptedDataDetails, objectSummary.getBucketName(),
                objectSummary.getKey(), versioningEnabled, objectSummary.getSize()))
        .collect(toList());
  }

  @Override
  public ListNotifyResponseData downloadArtifacts(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String bucketName, List<String> artifactPaths, String delegateId, String taskId, String accountId)
      throws IOException {
    ListNotifyResponseData res = new ListNotifyResponseData();

    for (String artifactPath : artifactPaths) {
      downloadArtifactsUsingFilter(
          awsConfig, encryptionDetails, bucketName, artifactPath, res, delegateId, taskId, accountId);
    }
    return res;
  }

  private void downloadArtifactsUsingFilter(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String bucketName, String artifactpathRegex, ListNotifyResponseData res, String delegateId, String taskId,
      String accountId) throws IOException {
    Pattern pattern = Pattern.compile(artifactpathRegex.replace(".", "\\.").replace("?", ".?").replace("*", ".*?"));

    ListObjectsV2Request listObjectsV2Request = getListObjectsV2Request(bucketName, artifactpathRegex);

    List<String> objectKeyList = Lists.newArrayList();
    ListObjectsV2Result result;
    do {
      result = awsHelperService.listObjectsInS3(awsConfig, encryptionDetails, listObjectsV2Request);
      List<S3ObjectSummary> objectSummaryList = result.getObjectSummaries();
      // in descending order. The most recent one comes first
      objectSummaryList.sort((o1, o2) -> o2.getLastModified().compareTo(o1.getLastModified()));

      List<String> objectKeyListForCurrentBatch = getObjectSummaries(pattern, objectSummaryList);
      objectKeyList.addAll(objectKeyListForCurrentBatch);
      listObjectsV2Request.setContinuationToken(result.getNextContinuationToken());
    } while (result.isTruncated());

    // We are not using stream here since addDataToResponse throws a bunch of exceptions and we want to throw them back
    // to the caller.
    for (String objectKey : objectKeyList) {
      Pair<String, InputStream> stringInputStreamPair =
          downloadArtifact(awsConfig, encryptionDetails, bucketName, objectKey);
      artifactCollectionTaskHelper.addDataToResponse(
          stringInputStreamPair, artifactpathRegex, res, delegateId, taskId, accountId);
    }
  }

  private ListObjectsV2Request getListObjectsV2Request(String bucketName, String artifactpathRegex) {
    ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request();
    String prefix = getPrefix(artifactpathRegex);
    if (prefix != null) {
      listObjectsV2Request.withPrefix(prefix);
    }

    listObjectsV2Request.withBucketName(bucketName).withMaxKeys(FETCH_FILE_COUNT_IN_BUCKET);
    return listObjectsV2Request;
  }

  public Pair<String, InputStream> downloadArtifact(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String bucketName, String key) {
    S3Object object = awsHelperService.getObjectFromS3(awsConfig, encryptionDetails, bucketName, key);
    if (object != null) {
      return Pair.of(object.getKey(), object.getObjectContent());
    }
    return null;
  }

  @Override
  public BuildDetails getArtifactBuildDetails(AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails,
      String bucketName, String key, boolean versioningEnabledForBucket, Long artifactFileSize) {
    String versionId = null;
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
    Map<String, String> map = new HashMap<>();
    map.put(URL, "https://s3.amazonaws.com/" + bucketName + "/" + key);
    map.put(BUILD_NO, versionId);
    map.put(BUCKET_NAME, bucketName);
    map.put(ARTIFACT_PATH, key);
    map.put(KEY, key);
    map.put(ARTIFACT_FILE_SIZE, String.valueOf(artifactFileSize));

    return aBuildDetails()
        .withNumber(versionId)
        .withRevision(versionId)
        .withArtifactPath(key)
        .withArtifactFileSize(String.valueOf(artifactFileSize))
        .withBuildParameters(map)
        .build();
  }

  public Long getFileSize(
      AwsConfig awsConfig, List<EncryptedDataDetail> encryptionDetails, String bucketName, String key) {
    ObjectMetadata objectMetadata =
        awsHelperService.getObjectMetadataFromS3(awsConfig, encryptionDetails, bucketName, key);
    if (objectMetadata == null) {
      throw new WingsException(ErrorCode.ARTIFACT_SERVER_ERROR);
    }
    return objectMetadata.getContentLength();
  }
}
