package software.wings.helpers.ext.gcs;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.eraro.ErrorCode.INVALID_ARTIFACT_SERVER;
import static io.harness.exception.WingsException.ADMIN;
import static io.harness.exception.WingsException.USER;
import static java.util.stream.Collectors.toList;
import static software.wings.common.Constants.ARTIFACT_PATH;
import static software.wings.common.Constants.BUCKET_NAME;
import static software.wings.common.Constants.BUILD_NO;
import static software.wings.common.Constants.KEY;
import static software.wings.common.Constants.URL;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import com.google.api.services.storage.Storage;
import com.google.api.services.storage.model.Bucket;
import com.google.api.services.storage.model.Buckets;
import com.google.api.services.storage.model.Objects;
import com.google.api.services.storage.model.StorageObject;
import com.google.common.collect.Lists;
import com.google.inject.Inject;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.GcpConfig;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.impl.GcpHelperService;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.utils.Misc;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class GcsServiceImpl implements GcsService {
  private static final Logger logger = LoggerFactory.getLogger(software.wings.helpers.ext.gcs.GcsServiceImpl.class);
  private GcpHelperService gcpHelperService;

  @Inject
  public GcsServiceImpl(GcpHelperService gcpHelperService) {
    this.gcpHelperService = gcpHelperService;
  }
  @Inject private EncryptionService encryptionService;

  @Override
  public List<String> getArtifactPaths(
      GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails, String bucketName) {
    List<String> objectNameList = Lists.newArrayList();
    Storage.Objects objs;
    Objects listOfObjects;
    try {
      Storage gcsStorageService = gcpHelperService.getGcsStorageService(gcpConfig, encryptionDetails);
      objs = gcsStorageService.objects();
      listOfObjects = objs.list(bucketName).execute();
    } catch (Exception e) {
      throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER)
          .addParam("message", "Could not get artifact paths from Google Cloud Storage for bucket :" + bucketName);
    }

    // Get objects for the bucket
    List<StorageObject> items;
    if (listOfObjects != null && listOfObjects.getItems() != null && listOfObjects.getItems().size() > 0) {
      items = listOfObjects.getItems();
      for (StorageObject storageObject : items) {
        objectNameList.add(storageObject.getName());
      }
    }
    return objectNameList;
  }

  @Override
  public List<BuildDetails> getArtifactsBuildDetails(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails,
      String bucketName, List<String> artifactPaths, boolean isExpression) {
    try {
      boolean versioningEnabledForBucket = isVersioningEnabledForBucket(gcpConfig, encryptionDetails, bucketName);
      List<BuildDetails> buildDetailsList = Lists.newArrayList();

      for (String artifactPath : artifactPaths) {
        List<BuildDetails> buildDetailsListForArtifactPath = getArtifactsBuildDetails(
            gcpConfig, encryptionDetails, bucketName, artifactPath, isExpression, versioningEnabledForBucket);
        buildDetailsList.addAll(buildDetailsListForArtifactPath);
      }
      return buildDetailsList;
    } catch (WingsException e) {
      throw new WingsException(INVALID_ARTIFACT_SERVER, ADMIN).addParam("message", Misc.getMessage(e));
    } catch (Exception e) {
      logger.error("Error occurred while retrieving artifacts from ", e);
      throw new WingsException(INVALID_ARTIFACT_SERVER, ADMIN).addParam("message", Misc.getMessage(e));
    }
  }

  @Override
  public List<BuildDetails> getArtifactsBuildDetails(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails,
      String bucketName, String artifactPath, boolean isExpression, boolean versioningEnabledForBucket) {
    List<BuildDetails> buildDetailsList = Lists.newArrayList();
    if (isExpression) {
      try {
        Pattern pattern = Pattern.compile(artifactPath.replace(".", "\\.").replace("?", ".?").replace("*", ".*?"));
        Storage gcsStorageService = gcpHelperService.getGcsStorageService(gcpConfig, encryptionDetails);
        Objects listOfObjects = gcsStorageService.objects().list(bucketName).execute();

        // Get all objects for the bucket
        if (listOfObjects != null && isNotEmpty(listOfObjects.getItems())) {
          List<String> objectKeyList = getObjectSummaries(pattern, listOfObjects.getItems());
          for (String obj : objectKeyList) {
            BuildDetails artifactMetadata =
                getArtifactBuildDetails(gcpConfig, encryptionDetails, bucketName, obj, versioningEnabledForBucket);
            buildDetailsList.add(artifactMetadata);
          }
        }
      } catch (Exception e) {
        throw new WingsException(ErrorCode.INVALID_ARTIFACT_SERVER)
            .addParam("message", "Could not get Build details from Google Cloud Storage for bucket :" + bucketName);
      }
    } else {
      BuildDetails artifactMetadata =
          getArtifactBuildDetails(gcpConfig, encryptionDetails, bucketName, artifactPath, versioningEnabledForBucket);
      buildDetailsList.add(artifactMetadata);
    }

    return buildDetailsList;
  }

  private List<String> getObjectSummaries(Pattern pattern, List<StorageObject> storageObjectList) {
    return storageObjectList.stream()
        .filter(
            storageObject -> !storageObject.getName().endsWith("/") && pattern.matcher(storageObject.getName()).find())
        .map(StorageObject::getName)
        .collect(toList());
  }

  @Override
  public BuildDetails getArtifactBuildDetails(GcpConfig gcpConfig, List<EncryptedDataDetail> encryptionDetails,
      String bucketName, String objName, boolean versioningEnabledForBucket) {
    try {
      String versionId;
      if (versioningEnabledForBucket) {
        Storage gcsStorageService = gcpHelperService.getGcsStorageService(gcpConfig, encryptionDetails);
        Storage.Objects.Get request = gcsStorageService.objects().get(bucketName, objName);
        versionId = objName + ":" + request.execute().getGeneration().toString();
      } else {
        versionId = objName;
      }

      Map<String, String> map = new HashMap<>();
      map.put(URL, "https://storage.cloud.google.com/" + bucketName + "/" + objName);
      map.put(BUILD_NO, versionId);
      map.put(BUCKET_NAME, bucketName);
      map.put(ARTIFACT_PATH, objName);
      map.put(KEY, objName);

      return aBuildDetails()
          .withNumber(versionId)
          .withRevision(versionId)
          .withArtifactPath(objName)
          .withBuildParameters(map)
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
          .addParam("message", "Could not get versioning information for GCS bucket. " + Misc.getMessage(e));
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
