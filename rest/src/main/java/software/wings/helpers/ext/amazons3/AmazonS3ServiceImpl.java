package software.wings.helpers.ext.amazons3;

import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ListBucketsRequest;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ListPartsRequest;
import com.amazonaws.services.s3.model.ListVersionsRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsConfig;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.common.Constants;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.impl.AwsHelperService;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javax.annotation.Nullable;

/**
 * @author rktummala 07/30/17
 */
public class AmazonS3ServiceImpl implements AmazonS3Service {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject AwsHelperService awsHelperService;

  @Override
  public Map<String, String> getBuckets(AwsConfig awsConfig) {
    AmazonS3Client amazonS3Client =
        awsHelperService.getAmazonS3Client(awsConfig.getAccessKey(), awsConfig.getSecretKey());
    List<Bucket> bucketList = amazonS3Client.listBuckets();
    return bucketList.stream().collect(Collectors.toMap(Bucket::getName, Bucket::getName, (a, b) -> b));
  }

  @Override
  public List<String> getArtifactPaths(AwsConfig awsConfig, String bucketName) {
    AmazonS3Client amazonS3Client =
        awsHelperService.getAmazonS3Client(awsConfig.getAccessKey(), awsConfig.getSecretKey());
    ListObjectsV2Request listObjectsV2Request = new ListObjectsV2Request();
    listObjectsV2Request.withBucketName(bucketName).withMaxKeys(500);
    ListObjectsV2Result result = amazonS3Client.listObjectsV2(listObjectsV2Request);
    return result.getObjectSummaries().stream().map(S3ObjectSummary::getKey).collect(Collectors.toList());
  }

  @Override
  public Pair<String, InputStream> downloadArtifact(AwsConfig awsConfig, String bucketName, String artifactPath) {
    AmazonS3Client amazonS3Client =
        awsHelperService.getAmazonS3Client(awsConfig.getAccessKey(), awsConfig.getSecretKey());
    S3Object object = amazonS3Client.getObject(bucketName, artifactPath);
    if (object != null) {
      return Pair.of(object.getKey(), object.getObjectContent());
    }

    return null;
  }

  @Override
  public BuildDetails getArtifactMetadata(
      AwsConfig awsConfig, ArtifactStreamAttributes artifactStreamAttributes, String appId) {
    AmazonS3Client amazonS3Client =
        awsHelperService.getAmazonS3Client(awsConfig.getAccessKey(), awsConfig.getSecretKey());
    String resourceUrl = amazonS3Client.getResourceUrl(
        artifactStreamAttributes.getJobName(), artifactStreamAttributes.getArtifactName());
    ObjectMetadata objectMetadata = amazonS3Client.getObjectMetadata(
        artifactStreamAttributes.getJobName(), artifactStreamAttributes.getArtifactName());
    BuildDetails buildDetails = null;

    if (objectMetadata != null) {
      String versionId = objectMetadata.getVersionId();
      // Not all objects in S3 have versioning enabled.
      // If versioning of object is not enabled, we treat it as version 0 so that artifact collector skips downloading
      // it again.
      if (versionId == null) {
        versionId = "0";
      }

      Map<String, String> map = new HashMap<>();
      map.put(Constants.BUILD_NO, versionId);
      map.put(Constants.URL, resourceUrl);

      buildDetails = aBuildDetails()
                         .withNumber(versionId)
                         .withRevision(versionId)
                         .withArtifactPath(artifactStreamAttributes.getArtifactName())
                         .withBuildParameters(map)
                         .build();
    }
    return buildDetails;
  }
}
