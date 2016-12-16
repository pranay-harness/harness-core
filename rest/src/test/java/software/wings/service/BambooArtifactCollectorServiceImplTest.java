package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.beans.artifact.ArtifactPathServiceEntry.Builder.anArtifactPathServiceEntry;
import static software.wings.service.intfc.FileService.FileBucket.ARTIFACTS;
import static software.wings.utils.WingsTestConstants.FILE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.BambooConfig;
import software.wings.beans.FileMetadata;
import software.wings.beans.Service;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.artifact.BambooArtifactStream;
import software.wings.helpers.ext.bamboo.BambooService;
import software.wings.service.impl.BambooArtifactCollectorServiceImpl;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.SettingsService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Arrays;
import javax.inject.Inject;

/**
 * Created by anubhaw on 12/15/16.
 */
public class BambooArtifactCollectorServiceImplTest extends WingsBaseTest {
  public static final Service SERVICE = aService().withUuid(SERVICE_ID).build();

  public static final BambooArtifactStream BAMBOO_ARTIFACT_STREAM =
      BambooArtifactStream.Builder.aBambooArtifactStream()
          .withSourceName("job1")
          .withJobname("job1")
          .withArtifactPathServices(Lists.newArrayList(anArtifactPathServiceEntry()
                                                           .withArtifactPathRegex("build/svr-*.war")
                                                           .withServiceIds(Arrays.asList(SERVICE_ID))
                                                           .build()))
          .build();
  @Captor ArgumentCaptor<FileMetadata> fileMetadataArgumentCaptor;
  @Captor ArgumentCaptor<FileBucket> fileBucketArgumentCaptor;

  @Mock private FileService fileService;
  @Mock private SettingsService settingsService;
  @Mock private BambooService bambooService;

  @InjectMocks @Inject private BambooArtifactCollectorServiceImpl bambooArtifactCollectorService;

  @Before
  public void setupMocks() throws IOException, URISyntaxException {
    when(fileService.saveFile(any(FileMetadata.class), any(InputStream.class), any(FileBucket.class)))
        .thenReturn(FILE_ID);
    when(settingsService.get(anyString()))
        .thenReturn(aSettingAttribute()
                        .withValue(BambooConfig.Builder.aBambooConfig()
                                       .withBamboosUrl("http://bamboo")
                                       .withUsername("username")
                                       .withPassword("password")
                                       .build())
                        .build());
  }

  @Test
  public void shouldCollectArtifact() {
    when(bambooService.downloadArtifact(any(BambooConfig.class), anyString(), anyString(), anyString()))
        .thenReturn(ImmutablePair.of("todolist.war", new ByteArrayInputStream("123".getBytes())));

    assertThat(bambooArtifactCollectorService.collect(
                   BAMBOO_ARTIFACT_STREAM, ImmutableMap.of(BambooArtifactCollectorServiceImpl.BUILD_NO, "50")))
        .isNotNull()
        .extracting(ArtifactFile::getFileUuid)
        .containsExactly(FILE_ID);
    verify(bambooService).downloadArtifact(any(BambooConfig.class), anyString(), anyString(), anyString());
    verify(fileService)
        .saveFile(fileMetadataArgumentCaptor.capture(), any(InputStream.class), fileBucketArgumentCaptor.capture());

    assertThat(fileMetadataArgumentCaptor.getValue())
        .extracting(FileMetadata::getFileName)
        .containsExactly("todolist.war");
    assertThat(fileBucketArgumentCaptor.getValue())
        .extracting(FileBucket::getName)
        .containsExactly(ARTIFACTS.getName());
  }

  @Test
  public void shouldFailToCollectWhenFileIsNull() throws Exception {
    assertThat(bambooArtifactCollectorService.collect(
                   BAMBOO_ARTIFACT_STREAM, ImmutableMap.of(BambooArtifactCollectorServiceImpl.BUILD_NO, "50")))
        .isNull();
    verify(bambooService).downloadArtifact(any(BambooConfig.class), anyString(), anyString(), anyString());
  }
}
