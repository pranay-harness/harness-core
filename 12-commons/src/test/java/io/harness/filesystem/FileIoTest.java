package io.harness.filesystem;

import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;
import static io.harness.filesystem.FileIo.deleteDirectoryAndItsContentIfExists;
import static io.harness.filesystem.FileIo.deleteFileIfExists;
import static io.harness.filesystem.FileIo.waitForDirectoryToBeAccessibleOutOfProcess;
import static io.harness.filesystem.FileIo.writeFile;
import static java.nio.file.Files.lines;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.stream.Stream;

public class FileIoTest extends CategoryTest {
  private static String tempDirectory = System.getProperty("java.io.tmpdir");

  private static String getRandomTempDirectory() {
    return tempDirectory + "/" + UUID.randomUUID().toString();
  }

  @Test
  @Category(UnitTests.class)
  public void createDirectoryTest() throws IOException {
    final String directoryPath = getRandomTempDirectory();
    try {
      createDirectoryIfDoesNotExist(directoryPath);
      File testFile = new File(directoryPath);
      assertThat(testFile.exists()).isTrue();
      long lastModifiedTime = testFile.lastModified();

      createDirectoryIfDoesNotExist(directoryPath);
      File testFile1 = new File(directoryPath);
      assertEquals(lastModifiedTime, testFile1.lastModified());
    } finally {
      deleteDirectoryAndItsContentIfExists(directoryPath);
    }
  }

  @Test
  @Category(UnitTests.class)
  public void waitForDirectoryToBeAccessibleOutOfProcessPositiveTest() throws IOException {
    final String directoryPath = getRandomTempDirectory();
    try {
      createDirectoryIfDoesNotExist(directoryPath);
      assertThat(waitForDirectoryToBeAccessibleOutOfProcess(directoryPath, 3)).isTrue();

    } finally {
      deleteDirectoryAndItsContentIfExists(directoryPath);
    }
  }

  @Test
  @Category(UnitTests.class)
  public void waitForDirectoryToBeAccessibleOutOfProcessNegativeTest() {
    assertFalse(waitForDirectoryToBeAccessibleOutOfProcess(getRandomTempDirectory(), 3));
  }

  @Test
  @Category(UnitTests.class)
  public void deleteFileIfExistsTest() throws IOException {
    final String fileName = tempDirectory + "/testfile.txt";
    deleteFileIfExists(fileName);
    File testFile = new File(fileName);
    assertFalse(testFile.exists());
    try (FileOutputStream outputStream = new FileOutputStream(testFile)) {
      outputStream.write("RandomTextContent".getBytes());
    }
    assertThat(testFile.exists()).isTrue();
    deleteFileIfExists(fileName);
    assertFalse(testFile.exists());
  }

  @Test
  @Category(UnitTests.class)
  public void deleteDirectoryAndItsContentTest() throws IOException {
    final String directoryName = getRandomTempDirectory();
    final String fileName = "/testfile.txt";
    createDirectoryIfDoesNotExist(directoryName);
    File directory = new File(directoryName);
    File testFile = new File(directory, fileName);
    try (FileOutputStream outputStream = new FileOutputStream(testFile)) {
      outputStream.write("RandomTextContent".getBytes());
    }
    assertThat(testFile.exists()).isTrue();
    deleteDirectoryAndItsContentIfExists(directoryName);
    assertFalse(testFile.exists());
    assertFalse(directory.exists());
  }

  @Test
  @Category(UnitTests.class)
  public void writeFileTest() throws IOException {
    final String directoryName = getRandomTempDirectory();
    final String fileName = "/testfile.txt";
    final String text = "randomText";
    createDirectoryIfDoesNotExist(directoryName);
    File directory = new File(directoryName);
    File testFile = new File(directory, fileName);
    writeFile(testFile.getAbsolutePath(), text.getBytes());
    assertThat(testFile.exists()).isTrue();

    try (Stream<String> stream = lines(Paths.get(testFile.getAbsolutePath()))) {
      String readOutput = stream.findFirst().get();
      assertEquals(text, readOutput);
    }

    deleteDirectoryAndItsContentIfExists(directoryName);
  }
}
