package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.wings.beans.ArtifactPathServiceEntry.Builder.anArtifactPathServiceEntry;
import static software.wings.beans.JenkinsArtifactSource.Builder.aJenkinsArtifactSource;
import static software.wings.beans.JenkinsConfig.Builder.aJenkinsConfig;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.service.intfc.FileService.FileBucket.ARTIFACTS;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Verifier;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.ArtifactFile;
import software.wings.beans.ArtifactSource.ArtifactType;
import software.wings.beans.FileMetadata;
import software.wings.beans.Service;
import software.wings.helpers.ext.jenkins.Jenkins;
import software.wings.helpers.ext.jenkins.JenkinsFactory;
import software.wings.service.impl.JenkinsArtifactCollectorServiceImpl;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.SettingsService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 5/12/16.
 */
public class JenkinsArtifactCollectorServiceImplTest extends WingsBaseTest {
  /**
   * The constant SERVICE.
   */
  public static final Service SERVICE = aService().withUuid("SERVICE_ID").build();
  /**
   * The constant JENKINS_ARTIFACT_SOURCE.
   */
  public static final software.wings.beans.JenkinsArtifactSource JENKINS_ARTIFACT_SOURCE =
      aJenkinsArtifactSource()
          .withSourceName("job1")
          .withJobname("job1")
          .withArtifactType(ArtifactType.WAR)
          .withArtifactPathServices(Lists.newArrayList(anArtifactPathServiceEntry()
                                                           .withArtifactPathRegex("build/svr-*.war")
                                                           .withServices(Lists.newArrayList(SERVICE))
                                                           .build()))
          .build();
  /**
   * The constant FILE_ID.
   */
  public static final String FILE_ID = "FILE_ID";
  /**
   * The File metadata argument captor.
   */
  @Captor ArgumentCaptor<FileMetadata> fileMetadataArgumentCaptor;
  /**
   * The File bucket argument captor.
   */
  @Captor ArgumentCaptor<FileBucket> fileBucketArgumentCaptor;
  @Mock private JenkinsFactory jenkinsFactory;
  @Mock private Jenkins jenkins;
  @Mock private FileService fileService;
  /**
   * The Verifier.
   */
  @Rule
  public Verifier verifier = new Verifier() {
    @Override
    protected void verify() throws Throwable {
      verifyNoMoreInteractions(jenkins, fileService, jenkinsFactory);
    }
  };
  @Mock private SettingsService settingsService;
  @InjectMocks @Inject private JenkinsArtifactCollectorServiceImpl jenkinsArtifactCollectorService;

  /**
   * setup all mocks for test.
   *
   * @throws IOException        Signals that an I/O exception has occurred.
   * @throws URISyntaxException the URI syntax exception
   */
  @Before
  public void setupMocks() throws IOException, URISyntaxException {
    when(jenkinsFactory.create(anyString(), anyString(), anyString())).thenReturn(jenkins);
    when(jenkins.downloadArtifact(anyString(), anyString(), anyString()))
        .thenReturn(ImmutablePair.of("svr-1234.war", new ByteArrayInputStream("Dummy".getBytes())));
    when(fileService.saveFile(any(FileMetadata.class), any(InputStream.class), any(FileBucket.class)))
        .thenReturn(FILE_ID);
    when(settingsService.get(anyString()))
        .thenReturn(aSettingAttribute()
                        .withValue(aJenkinsConfig()
                                       .withJenkinsUrl("http://jenkins")
                                       .withUsername("username")
                                       .withPassword("password")
                                       .build())
                        .build());
  }

  /**
   * Should collect artifact from jenkins.
   *
   * @throws Exception the exception
   */
  @Test
  public void shouldCollectArtifactFromJenkins() throws Exception {
    assertThat(jenkinsArtifactCollectorService.collect(
                   JENKINS_ARTIFACT_SOURCE, ImmutableMap.of(JenkinsArtifactCollectorServiceImpl.BUILD_NO, "50")))
        .isNotNull()
        .extracting(ArtifactFile::getFileUuid)
        .containsExactly(FILE_ID);
    verify(jenkinsFactory).create(anyString(), anyString(), anyString());
    verify(jenkins).downloadArtifact(anyString(), anyString(), anyString());
    verify(fileService)
        .saveFile(fileMetadataArgumentCaptor.capture(), any(InputStream.class), fileBucketArgumentCaptor.capture());

    assertThat(fileMetadataArgumentCaptor.getValue())
        .extracting(FileMetadata::getFileName)
        .containsExactly("svr-1234.war");
    assertThat(fileBucketArgumentCaptor.getValue())
        .extracting(FileBucket::getName)
        .containsExactly(ARTIFACTS.getName());
  }

  /**
   * Should fail to collect when file is null.
   *
   * @throws Exception the exception
   */
  @Test
  public void shouldFailToCollectWhenFileIsNull() throws Exception {
    reset(jenkins);
    assertThat(jenkinsArtifactCollectorService.collect(
                   JENKINS_ARTIFACT_SOURCE, ImmutableMap.of(JenkinsArtifactCollectorServiceImpl.BUILD_NO, "50")))
        .isNull();
    verify(jenkinsFactory).create(anyString(), anyString(), anyString());
    verify(jenkins).downloadArtifact(anyString(), anyString(), anyString());
  }
}
