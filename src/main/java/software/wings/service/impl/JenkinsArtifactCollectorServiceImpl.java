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
import software.wings.helpers.ext.Jenkins;
import software.wings.helpers.ext.JenkinsFactory;
import software.wings.service.intfc.ArtifactCollectorService;
import software.wings.service.intfc.FileService;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 5/11/16.
 */
public class JenkinsArtifactCollectorServiceImpl implements ArtifactCollectorService {
  public static final String BUILD_NO = "buildNo";

  @Inject private FileService fileService;

  @Inject private JenkinsFactory jenkinsFactory;

  @Override
  public List<ArtifactFile> collect(ArtifactSource artifactSource, Map<String, String> arguments) {
    JenkinsArtifactSource jenkinsArtifactSource = (JenkinsArtifactSource) artifactSource;
    List<ArtifactFile> artifactFiles = Lists.newArrayList();
    InputStream in = null;
    try {
      Jenkins jenkins = jenkinsFactory.create(jenkinsArtifactSource.getJenkinsUrl(),
          jenkinsArtifactSource.getUsername(), jenkinsArtifactSource.getPassword());
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
