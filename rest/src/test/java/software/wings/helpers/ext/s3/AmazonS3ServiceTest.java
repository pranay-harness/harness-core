package software.wings.helpers.ext.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.junit.Test;
import org.mockito.InjectMocks;
import software.wings.WingsBaseTest;
import software.wings.beans.AwsConfig;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.delegatetasks.DelegateFile;
import software.wings.delegatetasks.DelegateFileManager;
import software.wings.helpers.ext.amazons3.AmazonS3Service;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.impl.AwsHelperService;
import software.wings.waitnotify.ListNotifyResponseData;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author rktummala on 09/30/17
 */
public class AmazonS3ServiceTest extends WingsBaseTest {
  @Inject @InjectMocks private AwsHelperService awsHelperService;
  @Inject private AmazonS3Service amazonS3Service;
  @Inject @InjectMocks private DelegateFileManager delegateFileManager;

  private static final AwsConfig awsConfig = AwsConfig.Builder.anAwsConfig()
                                                 .withAccessKey("access")
                                                 .withSecretKey("secret".toCharArray())
                                                 .withAccountId("accountId")
                                                 .build();

  @Test
  public void shouldGetBuckets() {
    when(awsHelperService.listS3Buckets(awsConfig)).thenReturn(Lists.newArrayList(new Bucket("bucket1")));
    Map<String, String> buckets = amazonS3Service.getBuckets(awsConfig);
    assertThat(buckets).hasSize(1).containsKeys("bucket1");
  }

  @Test
  public void shouldGetArtifactPaths() {
    ListObjectsV2Result listObjectsV2Result = new ListObjectsV2Result();
    listObjectsV2Result.setBucketName("bucket1");
    S3ObjectSummary objectSummary = new S3ObjectSummary();
    objectSummary.setKey("key1");
    objectSummary.setBucketName("bucket1");
    objectSummary.setLastModified(new Date());
    listObjectsV2Result.getObjectSummaries().add(objectSummary);
    when(awsHelperService.listObjectsInS3(any(AwsConfig.class), any())).thenReturn(listObjectsV2Result);
    List<String> artifactPaths = amazonS3Service.getArtifactPaths(awsConfig, "bucket1");
    assertThat(artifactPaths).hasSize(1).contains("key1");
  }

  @Test
  public void shouldDownloadArtifacts() throws IOException, URISyntaxException {
    File file = new File("test.txt");
    file.createNewFile();

    try {
      ListObjectsV2Result listObjectsV2Result = new ListObjectsV2Result();
      listObjectsV2Result.setBucketName("bucket1");
      S3ObjectSummary objectSummary = new S3ObjectSummary();
      objectSummary.setKey("key1");
      objectSummary.setBucketName("bucket1");
      objectSummary.setLastModified(new Date());
      listObjectsV2Result.getObjectSummaries().add(objectSummary);
      when(awsHelperService.listObjectsInS3(any(AwsConfig.class), any())).thenReturn(listObjectsV2Result);

      ObjectMetadata objectMetadata = new ObjectMetadata();
      objectMetadata.setLastModified(new Date());

      S3Object s3Object = new S3Object();
      s3Object.setBucketName("bucket1");
      s3Object.setKey("key1");
      s3Object.setObjectMetadata(objectMetadata);

      DelegateFile delegateFile = new DelegateFile();
      delegateFile.setFileId(UUID.randomUUID().toString());

      s3Object.setObjectContent(new FileInputStream(file));
      when(awsHelperService.getObjectFromS3(any(AwsConfig.class), any(), any())).thenReturn(s3Object);
      when(delegateFileManager.upload(any(), any())).thenReturn(delegateFile);

      ListNotifyResponseData listNotifyResponseData =
          amazonS3Service.downloadArtifacts(awsConfig, "bucket1", Lists.newArrayList("key1"), null, null, null);
      List<ArtifactFile> artifactFileList = (List<ArtifactFile>) listNotifyResponseData.getData();
      ArtifactFile artifactFile = new ArtifactFile();
      artifactFile.setName("key1");
      assertThat(artifactFileList).hasSize(1);
      assertThat(artifactFileList.get(0).getName().equals("key1"));
    } finally {
      file.delete();
    }
  }

  @Test
  public void shouldGetArtifactBuildDetails() throws IOException, URISyntaxException {
    ListObjectsV2Result listObjectsV2Result = new ListObjectsV2Result();
    listObjectsV2Result.setBucketName("bucket1");
    S3ObjectSummary objectSummary = new S3ObjectSummary();
    objectSummary.setKey("key1");
    objectSummary.setBucketName("bucket1");
    objectSummary.setLastModified(new Date());
    listObjectsV2Result.getObjectSummaries().add(objectSummary);
    when(awsHelperService.listObjectsInS3(any(AwsConfig.class), any())).thenReturn(listObjectsV2Result);

    ObjectMetadata objectMetadata = new ObjectMetadata();
    objectMetadata.setLastModified(new Date());

    when(awsHelperService.getObjectMetadataFromS3(any(AwsConfig.class), any(), any())).thenReturn(objectMetadata);

    BuildDetails artifactBuildDetails =
        amazonS3Service.getArtifactBuildDetails(awsConfig, "bucket1", "key1", false, false);
    assertThat(artifactBuildDetails.getArtifactPath().equals("key1"));
  }

  @Test
  public void shouldGetArtifactsBuildDetails() throws IOException, URISyntaxException {
    ListObjectsV2Result listObjectsV2Result = new ListObjectsV2Result();
    listObjectsV2Result.setBucketName("bucket1");
    S3ObjectSummary objectSummary = new S3ObjectSummary();
    objectSummary.setKey("key1");
    objectSummary.setBucketName("bucket1");
    objectSummary.setLastModified(new Date());
    listObjectsV2Result.getObjectSummaries().add(objectSummary);
    when(awsHelperService.listObjectsInS3(any(AwsConfig.class), any())).thenReturn(listObjectsV2Result);

    ObjectMetadata objectMetadata = new ObjectMetadata();
    objectMetadata.setLastModified(new Date());

    when(awsHelperService.getObjectMetadataFromS3(any(AwsConfig.class), any(), any())).thenReturn(objectMetadata);

    List<BuildDetails> artifactsBuildDetails =
        amazonS3Service.getArtifactsBuildDetails(awsConfig, "bucket1", Lists.newArrayList("key1"), false);
    assertThat(artifactsBuildDetails).hasSize(1);
    assertThat(artifactsBuildDetails.get(0).getArtifactPath().equals("key1"));
  }
}
