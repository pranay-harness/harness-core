package software.wings.helpers.ext.gcs;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.INVALID_ARTIFACT_SERVER;
import static io.harness.exception.WingsException.USER;
import static java.util.stream.Collectors.toList;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import com.google.api.services.storage.Storage;
import com.google.api.services.storage.model.Bucket;
import com.google.api.services.storage.model.Buckets;
import com.google.api.services.storage.model.Objects;
import com.google.api.services.storage.model.StorageObject;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.ExceptionUtils;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import software.wings.beans.GcpConfig;
import software.wings.beans.artifact.Artifact.ArtifactMetadataKeys;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.impl.GcpHelperService;
import software.wings.service.intfc.security.EncryptionService;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Singleton
@Slf4j
public class GcsServiceImpl implements GcsService {
  private GcpHelperService gcpHelperService;
  private static int MAX_GCS_ARTIFACT_PATH_LIMIT = 1000;
  private static int MAX_GCS_BUILD_DETAILS_LIMIT = 100;
  private final SimpleDateFormat GCS_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
  private static final String LAST_UPDATED_AT = "lastUpdatedAt";

  private PriorityQueue<GCSPair> gcsBuildDetailsQueue = new PriorityQueue<>(MAX_GCS_BUILD_DETAILS_LIMIT,
      (a, b) -> (int) (Long.parseLong(b.getUpdatedTime()) - Long.parseLong(a.getUpdatedTime())));

  @Inject
  public GcsServiceImpl(GcpHelperService gcpHelperService) {
    this.gcpHelperService = gcpHelperService;
  }
  @Inject private EncryptionService encryptionService;

  @Override
  public List<String> getArtifactPaths(
      GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails, String bucketName) {
    List<GCSPair> gcsObjects = Lists.newArrayList();
    Objects listOfObjects;
    String nextPageToken = "";
    long maxResults = 1000;
    int curCount = 0;
    int maxToRetrive = 10000;

    try {
      Storage gcsStorageService = gcpHelperService.getGcsStorageService(gcpConfig, encryptionDetails);
      Storage.Objects.List listObjects = gcsStorageService.objects().list(bucketName);
      listObjects.setMaxResults(maxResults);
      do {
        listOfObjects = listObjects.execute();
        // Get objects for the bucket
        List<StorageObject> items;
        if (listOfObjects != null && listOfObjects.getItems() != null && listOfObjects.getItems().size() > 0) {
          items = listOfObjects.getItems();
          for (StorageObject storageObject : items) {
            gcsObjects.add(new GCSPair(storageObject.getName(), storageObject.getUpdated().toString()));
          }
        }

        if (listOfObjects != null) {
          nextPageToken = listOfObjects.getNextPageToken();
          if (isNotEmpty(nextPageToken)) {
            listObjects.setPageToken(nextPageToken);
          }
        }
        curCount += maxResults;
      } while (nextPageToken != null && curCount < maxToRetrive);
    } catch (Exception e) {
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER)
          .addParam("message", "Could not get artifact paths from Google Cloud Storage for bucket :" + bucketName);
    }

    // Sort artifact paths by updated time in reverse order
    gcsObjects.sort((o1, o2) -> o2.updatedTime.compareTo(o1.updatedTime));
    return gcsObjects.stream()
        .map(GCSPair::getObjectName)
        .limit(MAX_GCS_ARTIFACT_PATH_LIMIT)
        .collect(Collectors.toList());
  }

  @Override
  public List<BuildDetails> getArtifactsBuildDetails(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails,
      ArtifactStreamAttributes artifactStreamAttributes, List<String> artifactPaths, boolean isExpression, int limit) {
    String bucketName = artifactStreamAttributes.getJobName();
    try {
      boolean versioningEnabledForBucket = isVersioningEnabledForBucket(gcpConfig, encryptionDetails, bucketName);
      List<BuildDetails> buildDetailsList = Lists.newArrayList();

      for (String artifactPath : artifactPaths) {
        List<BuildDetails> buildDetailsListForArtifactPath = getArtifactsBuildDetails(
            gcpConfig, encryptionDetails, bucketName, artifactPath, isExpression, versioningEnabledForBucket, limit);
        buildDetailsList.addAll(buildDetailsListForArtifactPath);
      }
      return buildDetailsList;
    } catch (WingsException e) {
      throw new WingsException(INVALID_ARTIFACT_SERVER, USER).addParam("message", ExceptionUtils.getMessage(e));
    } catch (Exception e) {
      logger.error("Error occurred while retrieving artifacts from ", e);
      throw new WingsException(INVALID_ARTIFACT_SERVER, USER).addParam("message", ExceptionUtils.getMessage(e));
    }
  }

  @Override
  public List<BuildDetails> getArtifactsBuildDetails(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails,
      String bucketName, String artifactPath, boolean isExpression, boolean versioningEnabledForBucket, int limit) {
    List<BuildDetails> buildDetailsList = Lists.newArrayList();
    String nextPageToken = "";
    long maxResults = 1000;
    gcsBuildDetailsQueue.clear();

    if (isExpression) {
      try {
        Pattern pattern = Pattern.compile(artifactPath.replace(".", "\\.").replace("?", ".?").replace("*", ".*?"));
        Storage gcsStorageService = gcpHelperService.getGcsStorageService(gcpConfig, encryptionDetails);
        Storage.Objects.List listObjects = gcsStorageService.objects().list(bucketName);
        listObjects.setMaxResults(maxResults);

        do {
          Objects listOfObjects = listObjects.execute();

          // Get objects for the bucket
          if (listOfObjects != null && isNotEmpty(listOfObjects.getItems())) {
            fillObjectSummaries(pattern, listOfObjects.getItems());
          }

          // Set page token to get next set of objects
          if (listOfObjects != null) {
            nextPageToken = listOfObjects.getNextPageToken();
            if (isNotEmpty(nextPageToken)) {
              listObjects.setPageToken(nextPageToken);
            }
          }
        } while (nextPageToken != null);
      } catch (Exception e) {
        throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER)
            .addParam("message", "Could not get Build details from Google Cloud Storage for bucket :" + bucketName);
      }

      // Get build details for recent objects
      int curCount = 0;
      while (!gcsBuildDetailsQueue.isEmpty() && curCount <= MAX_GCS_BUILD_DETAILS_LIMIT) {
        GCSPair pair = gcsBuildDetailsQueue.poll();
        if (pair != null) {
          BuildDetails artifactMetadata = getArtifactBuildDetails(
              gcpConfig, encryptionDetails, bucketName, pair.getObjectName(), versioningEnabledForBucket);
          buildDetailsList.add(artifactMetadata);
        }
        curCount++;
      }
    } else {
      BuildDetails artifactMetadata =
          getArtifactBuildDetails(gcpConfig, encryptionDetails, bucketName, artifactPath, versioningEnabledForBucket);
      buildDetailsList.add(artifactMetadata);
    }

    buildDetailsList = buildDetailsList.stream().limit(MAX_GCS_BUILD_DETAILS_LIMIT).collect(Collectors.toList());
    return buildDetailsList;
  }

  private void fillObjectSummaries(Pattern pattern, List<StorageObject> storageObjectList) {
    storageObjectList =
        storageObjectList.stream()
            .filter(storageObject
                -> !storageObject.getName().endsWith("/") && pattern.matcher(storageObject.getName()).find())
            .collect(toList());

    List<StorageObject> newObjects = Lists.newArrayList();
    try {
      for (StorageObject so : storageObjectList) {
        newObjects.add(so);
        gcsBuildDetailsQueue.add(
            new GCSPair(so.getName(), Long.toString(GCS_DATE_FORMAT.parse(so.getUpdated().toString()).getTime())));
      }
    } catch (Exception e) {
      logger.error("Exception occurred while parsing GCS objects", USER);
    }
  }

  @Override
  public BuildDetails getArtifactBuildDetails(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails,
      String bucketName, String objName, boolean versioningEnabledForBucket) {
    try {
      String versionId;
      Storage gcsStorageService = gcpHelperService.getGcsStorageService(gcpConfig, encryptionDetails);
      Storage.Objects.Get request = gcsStorageService.objects().get(bucketName, objName);
      String lastUpdatedAt = request.execute().getUpdated().toString();

      if (versioningEnabledForBucket) {
        versionId = objName + ":" + request.execute().getGeneration().toString();

      } else {
        versionId = objName;
      }

      Map<String, String> map = new HashMap<>();
      map.put(ArtifactMetadataKeys.url, "https://storage.cloud.google.com/" + bucketName + "/" + objName);
      map.put(ArtifactMetadataKeys.buildNo, versionId);
      map.put(ArtifactMetadataKeys.bucketName, bucketName);
      map.put(ArtifactMetadataKeys.artifactPath, objName);
      map.put(ArtifactMetadataKeys.key, objName);
      map.put(LAST_UPDATED_AT, lastUpdatedAt);

      return aBuildDetails()
          .withNumber(versionId)
          .withRevision(versionId)
          .withArtifactPath(objName)
          .withBuildParameters(map)
          .withUiDisplayName("Build# " + versionId)
          .build();
    } catch (Exception e) {
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, USER)
          .addParam("message",
              "Could not get Build details from Google Cloud Storage for object :" + bucketName + "/" + objName);
    }
  }

  public boolean isVersioningEnabledForBucket(
      GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails, String bucketName) {
    boolean versioningEnabled = false;
    try {
      encryptionService.decrypt(gcpConfig, encryptionDetails);
      // Get versioning info for given bucket
      Storage gcsStorageService = gcpHelperService.getGcsStorageService(gcpConfig, encryptionDetails);
      Storage.Buckets.Get request = gcsStorageService.buckets().get(bucketName);

      if (request.execute().getVersioning() != null) {
        versioningEnabled = request.execute().getVersioning().getEnabled();
      }
    } catch (Exception e) {
      throw new WingsException(INVALID_ARTIFACT_SERVER, USER)
          .addParam("message", "Could not get versioning information for GCS bucket. " + ExceptionUtils.getMessage(e));
    }
    return versioningEnabled;
  }

  @Override
  public String getProject(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptedDataDetails) {
    if (isNotEmpty(encryptedDataDetails)) {
      encryptionService.decrypt(gcpConfig, encryptedDataDetails);
    }
    return new JSONObject(new String(gcpConfig.getServiceAccountKeyFileContent())).get("project_id").toString();
  }

  @Override
  public Map<String, String> listBuckets(
      GcpConfig gcpConfig, String projectId, List<EncryptedDataDetail> encryptedDataDetails) {
    Storage.Buckets bucketsObj;
    Buckets listOfBuckets;
    Map<String, String> bucketList = new HashMap<>();
    if (isNotEmpty(encryptedDataDetails)) {
      encryptionService.decrypt(gcpConfig, encryptedDataDetails);
    }

    // List buckets for current project in service account if project is empty
    if (isEmpty(projectId)) {
      projectId = new JSONObject(new String(gcpConfig.getServiceAccountKeyFileContent())).get("project_id").toString();
    }

    try {
      Storage gcsStorageService = gcpHelperService.getGcsStorageService(gcpConfig, encryptedDataDetails);
      bucketsObj = gcsStorageService.buckets();
      Storage.Buckets.List request = bucketsObj.list(projectId);
      listOfBuckets = request.execute();
    } catch (Exception e) {
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER, USER)
          .addParam("message", "Could not list Buckets from Google Cloud Storage");
    }

    // Get buckets for the project
    List<Bucket> items;
    if (listOfBuckets != null && listOfBuckets.getItems().size() > 0) {
      items = listOfBuckets.getItems();
      for (Bucket buck : items) {
        bucketList.put(buck.getName(), buck.getId());
      }
    }
    return bucketList;
  }
}
