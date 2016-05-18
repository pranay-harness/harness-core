package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static software.wings.beans.FileMetadata.Builder.aFileMetadata;
import static software.wings.service.ExtendedFile.Builder.anExtendedFile;

import com.google.common.io.Files;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import software.wings.WingsBaseTest;
import software.wings.beans.FileMetadata;
import software.wings.rules.RealMongo;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import javax.inject.Inject;

/**
 * Created by peeyushaggarwal on 5/17/16.
 */
@RealMongo
public class FileServiceTest extends WingsBaseTest {
  @Inject private FileService fileService;

  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();

  private File tempFile;

  @Before
  public void setupFiles() throws IOException {
    tempFile = temporaryFolder.newFile();
    Files.write("Dummy".getBytes(), tempFile);
  }

  @Test
  public void shouldSaveFileWithMetadata() throws Exception {
    FileMetadata fileMetadata = aFileMetadata().withFileName("dummy.txt").withMimeType("text/plain").build();
    String fileId = fileService.saveFile(fileMetadata, new FileInputStream(tempFile), FileBucket.ARTIFACTS);
    assertThat(fileService.getGridFsFile(fileId, FileBucket.ARTIFACTS)).isNotNull();
  }

  @Test
  public void shouldThrowExceptionWhenFileNameIsNullWithFileMetadata() throws Exception {
    FileMetadata fileMetadata = aFileMetadata().withMimeType("text/plain").build();
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> fileService.saveFile(fileMetadata, new FileInputStream(tempFile), FileBucket.ARTIFACTS));
  }

  @Test
  public void shouldSaveBaseFile() throws Exception {
    assertThat(fileService.saveFile(
                   anExtendedFile().withName("dummy.txt").build(), new FileInputStream(tempFile), FileBucket.ARTIFACTS))
        .isNotNull();
  }

  @Test
  public void shouldThrowExceptionWhenFileNameIsNullWithBaseFile() throws Exception {
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(
            () -> fileService.saveFile(anExtendedFile().build(), new FileInputStream(tempFile), FileBucket.ARTIFACTS))
        .isNotNull();
  }
}
