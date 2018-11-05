package software.wings.service.impl;

import static software.wings.common.Constants.ARTIFACT_PATH;
import static software.wings.helpers.ext.jenkins.BuildDetails.Builder.aBuildDetails;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.hierynomus.msfscc.fileinformation.FileIdBothDirectoryInformation;
import com.hierynomus.smbj.SMBClient;
import com.hierynomus.smbj.SmbConfig;
import com.hierynomus.smbj.auth.AuthenticationContext;
import com.hierynomus.smbj.connection.Connection;
import com.hierynomus.smbj.session.Session;
import com.hierynomus.smbj.share.DiskShare;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Singleton
public class SmbHelperService {
  private static final Logger logger = LoggerFactory.getLogger(SmbHelperService.class);
  @Inject private EncryptionService encryptionService;

  public List<String> getSmbPaths(software.wings.beans.SmbConfig smbConfig, List<EncryptedDataDetail> encryptionDetails)
      throws IOException {
    encryptionService.decrypt(smbConfig, encryptionDetails);
    List<String> artifactPaths = new ArrayList<>();

    SMBClient client = new SMBClient(getSMBConnectionConfig());
    try (Connection connection = client.connect(getSMBConnectionHost(smbConfig.getSmbUrl()))) {
      AuthenticationContext ac = new AuthenticationContext(smbConfig.getUsername(), smbConfig.getPassword(), null);
      Session session = connection.authenticate(ac);

      // Connect to Shared folder
      String sharedFolderName = getSharedFolderName(smbConfig.getSmbUrl());
      try (DiskShare share = (DiskShare) session.connectShare(sharedFolderName)) {
        for (FileIdBothDirectoryInformation f : share.list("", "*.*")) {
          artifactPaths.add(f.getFileName());
        }
      }
    }

    logger.info("SMB server {} returned {} artifact paths : ", smbConfig.getSmbUrl(), artifactPaths.size());
    return artifactPaths;
  }

  public String getSMBConnectionHost(String smbUrl) {
    String smbHost = smbUrl;
    smbHost = smbHost.replaceFirst("^(smb?://)", "").split("/")[0];
    return smbHost;
  }

  public boolean isConnetableSMBServer(String smbUrl) {
    try {
      SMBClient client = new SMBClient(getSMBConnectionConfig());
      try (Connection connection = client.connect(getSMBConnectionHost(smbUrl))) {
        return true;
      }
    } catch (Exception ex) {
      logger.warn("SMB server {} could not be reached. Exception Message {}", smbUrl, ex.getMessage());
    }
    return false;
  }

  private SmbConfig getSMBConnectionConfig() {
    return SmbConfig.builder()
        .withTimeout(120, TimeUnit.SECONDS) // Timeout sets Read, Write, and Transact timeouts (default is 60 seconds)
        .withSoTimeout(180, TimeUnit.SECONDS) // Socket Timeout (default is 0 seconds, blocks forever)
        .build();
  }

  private String getSharedFolderName(String smbUrl) {
    String smbHost = smbUrl;
    return smbHost.replaceFirst("^(smb?://)", "").split("/")[1];
  }

  public List<BuildDetails> getArtifactDetails(software.wings.beans.SmbConfig smbConfig,
      List<EncryptedDataDetail> encryptionDetails, List<String> artifactPaths) throws IOException {
    List<BuildDetails> buildDetailsList = Lists.newArrayList();
    encryptionService.decrypt(smbConfig, encryptionDetails);
    SMBClient client = new SMBClient(getSMBConnectionConfig());
    try (Connection connection = client.connect(getSMBConnectionHost(smbConfig.getSmbUrl()))) {
      AuthenticationContext ac = new AuthenticationContext(smbConfig.getUsername(), smbConfig.getPassword(), null);
      Session session = connection.authenticate(ac);

      // Connect to Shared folder
      String sharedFolderName = getSharedFolderName(smbConfig.getSmbUrl());
      try (DiskShare share = (DiskShare) session.connectShare(sharedFolderName)) {
        // Get artifact details for each artifact path
        for (String artifactPath : artifactPaths) {
          List<FileIdBothDirectoryInformation> fileList = share.list("", artifactPath);
          List<BuildDetails> buildDetailsListForArtifactPath = Lists.newArrayList();

          for (FileIdBothDirectoryInformation f : fileList) {
            Map<String, String> map = new HashMap<>();
            map.put(ARTIFACT_PATH, artifactPath);
            map.put("fileName", f.getFileName());
            map.put("allocationSize", Long.toString(f.getAllocationSize()));
            map.put("fileAttributes", Long.toString(f.getFileAttributes()));

            buildDetailsListForArtifactPath.add(aBuildDetails()
                                                    .withNumber(f.getFileName())
                                                    .withArtifactPath(artifactPath)
                                                    .withBuildParameters(map)
                                                    .build());
          }

          buildDetailsList.addAll(buildDetailsListForArtifactPath);
        }
      }
    }

    logger.info("SMB server {} returned {} build details for artifact paths : ", smbConfig.getSmbUrl(),
        buildDetailsList.size());
    return buildDetailsList;
  }
}
