package software.wings.service.impl;

import static software.wings.service.intfc.FileService.FileBucket.ARTIFACTS;

import com.google.common.collect.Lists;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.beans.ArtifactFile;
import software.wings.beans.ArtifactPathServiceEntry;
import software.wings.beans.ArtifactSource;
import software.wings.beans.FileMetadata;
import software.wings.beans.JenkinsArtifactSource;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.SettingAttribute;
import software.wings.helpers.ext.jenkins.Jenkins;
import software.wings.helpers.ext.jenkins.JenkinsFactory;
import software.wings.service.intfc.ArtifactCollectorService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.SettingsService;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

// TODO: Auto-generated Javadoc

/**
 * Created by peeyushaggarwal on 5/11/16.
 */
public class JenkinsArtifactCollectorServiceImpl implements ArtifactCollectorService {
  /**
   * The constant BUILD_NO.
   */
  public static final String BUILD_NO = "buildNo";
  /**
   * The Settings service.
   */
  @Inject SettingsService settingsService;
  @Inject private FileService fileService;
  @Inject private JenkinsFactory jenkinsFactory;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ArtifactCollectorService#collect(software.wings.beans.ArtifactSource,
   * java.util.Map)
   */
  @Override
  public List<ArtifactFile> collect(ArtifactSource artifactSource, Map<String, String> arguments) {
    JenkinsArtifactSource jenkinsArtifactSource = (JenkinsArtifactSource) artifactSource;
    List<ArtifactFile> artifactFiles = Lists.newArrayList();
    InputStream in = null;
    try {
      SettingAttribute settingAttribute = settingsService.get(jenkinsArtifactSource.getJenkinsSettingId());
      JenkinsConfig jenkinsConfig = (JenkinsConfig) settingAttribute.getValue();
      Jenkins jenkins = jenkinsFactory.create(
          jenkinsConfig.getJenkinsUrl(), jenkinsConfig.getUsername(), jenkinsConfig.getPassword());
      for (ArtifactPathServiceEntry artifactPathServiceEntry : jenkinsArtifactSource.getArtifactPathServices()) {
        Pair<String, InputStream> fileInfo = jenkins.downloadArtifact(jenkinsArtifactSource.getJobname(),
            arguments.get(BUILD_NO), artifactPathServiceEntry.getArtifactPathRegex());
        if (fileInfo == null) {
          throw new FileNotFoundException(
              "Unable to get artifact from jenkins for path " + artifactPathServiceEntry.getArtifactPathRegex());
        }
        in = fileInfo.getValue();
        FileMetadata fileMetadata = new FileMetadata();
        fileMetadata.setFileName(fileInfo.getKey());
        String uuid = fileService.saveFile(fileMetadata, in, ARTIFACTS);
        ArtifactFile artifactFile = new ArtifactFile();
        artifactFile.setFileUuid(uuid);
        artifactFile.setName(fileInfo.getKey());
        artifactFile.setServices(Lists.newArrayList(jenkinsArtifactSource.getServices()));
        artifactFiles.add(artifactFile);
      }
      return artifactFiles;
    } catch (Exception ex) {
      return null;
    } finally {
      IOUtils.closeQuietly(in);
    }
  }
}
