package software.wings.integration.service;

import static io.harness.rule.OwnerRule.GEORGE;
import static io.harness.rule.OwnerRule.RAGHU;
import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static software.wings.utils.WingsTestConstants.FILE_ID;

import com.google.common.io.Files;
import com.google.inject.Inject;

import io.harness.category.element.IntegrationTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import software.wings.WingsBaseTest;
import software.wings.beans.BaseFile;
import software.wings.beans.FileMetadata;
import software.wings.rules.Integration;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

@Integration
public class FileServiceIntegrationTest extends WingsBaseTest {
  /**
   * The Temporary folder.
   */
  @Rule public TemporaryFolder temporaryFolder = new TemporaryFolder();
  @Inject private FileService fileService;
  private File tempFile;

  /**
   * Setup files.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Before
  public void setupFiles() throws IOException {
    tempFile = temporaryFolder.newFile();
    Files.write("Dummy".getBytes(), tempFile);
  }

  /**
   * Should save file with metadata.
   *
   * @throws Exception the exception
   */
  @Test
  @Owner(developers = UTKARSH)
  @Category(IntegrationTests.class)
  public void shouldSaveFileWithMetadata() throws Exception {
    FileMetadata fileMetadata = FileMetadata.builder()
                                    .fileName("dummy.txt")
                                    .mimeType("text/plain")
                                    .fileUuid(UUIDGenerator.generateUuid())
                                    .build();
    String fileId = fileService.saveFile(fileMetadata, new FileInputStream(tempFile), FileBucket.ARTIFACTS);
    assertThat(fileService.getFileMetadata(fileId, FileBucket.ARTIFACTS)).isNotNull();
  }

  /**
   * Should throw exception when file name is null with file metadata.
   *
   * @throws Exception the exception
   */
  @Test
  @Owner(developers = RAGHU)
  @Category(IntegrationTests.class)
  public void shouldThrowExceptionWhenFileNameIsNullWithFileMetadata() throws Exception {
    FileMetadata fileMetadata = FileMetadata.builder().mimeType("text/plain").build();
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> fileService.saveFile(fileMetadata, new FileInputStream(tempFile), FileBucket.ARTIFACTS));
  }

  /**
   * Should save base file.
   *
   * @throws Exception the exception
   */
  @Test
  @Owner(developers = GEORGE)
  @Category(IntegrationTests.class)
  public void shouldSaveBaseFile() throws Exception {
    final BaseFile baseFile = new BaseFile();
    baseFile.setName("dummy.txt");
    baseFile.setFileName("dummy.txt");

    assertThat(fileService.saveFile(baseFile, new FileInputStream(tempFile), FileBucket.ARTIFACTS)).isNotNull();
  }

  /**
   * Should save base file.
   *
   * @throws Exception the exception
   */
  @Test
  @Owner(developers = GEORGE)
  @Category(IntegrationTests.class)
  public void shouldUpdateEntityId() throws Exception {
    final BaseFile baseFile = new BaseFile();
    baseFile.setName("dummy.txt");
    baseFile.setFileName("dummy.txt");

    String fileId = fileService.saveFile(baseFile, new FileInputStream(tempFile), FileBucket.ARTIFACTS);
    assertThat(fileId).isNotNull();
    fileService.updateParentEntityIdAndVersion(null, FILE_ID, 1, fileId, null, FileBucket.ARTIFACTS);
    assertThat(fileService.getAllFileIds(FILE_ID, FileBucket.ARTIFACTS)).hasSize(1).contains(fileId);
  }

  /**
   * Should throw exception when file name is null with base file.
   *
   * @throws Exception the exception
   */
  @Test
  @Owner(developers = GEORGE)
  @Category(IntegrationTests.class)
  public void shouldThrowExceptionWhenFileNameIsNullWithBaseFile() throws Exception {
    final BaseFile baseFile = new BaseFile();
    assertThatExceptionOfType(IllegalArgumentException.class)
        .isThrownBy(() -> fileService.saveFile(baseFile, new FileInputStream(tempFile), FileBucket.ARTIFACTS))
        .isNotNull();
  }
}
