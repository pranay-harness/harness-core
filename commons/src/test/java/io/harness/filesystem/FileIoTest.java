package io.harness.filesystem;

import static io.harness.filesystem.FileIo.createDirectoryIfDoesNotExist;
import static io.harness.filesystem.FileIo.deleteDirectoryAndItsContentIfExists;
import static io.harness.filesystem.FileIo.deleteFileIfExists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.UUID;

public class FileIoTest {
  private static String tempDirectory = System.getProperty("java.io.tmpdir");

  private static String getRandomTempDirectory() {
    return tempDirectory + "/" + UUID.randomUUID().toString();
  }

  @Test
  public void createDirectoryTest() throws IOException {
    final String directoryPath = getRandomTempDirectory();
    try {
      createDirectoryIfDoesNotExist(directoryPath);
      File testFile = new File(directoryPath);
      assertTrue(testFile.exists());
      long lastModifiedTime = testFile.lastModified();

      createDirectoryIfDoesNotExist(directoryPath);
      File testFile1 = new File(directoryPath);
      assertEquals(lastModifiedTime, testFile1.lastModified());
    } finally {
      deleteDirectoryAndItsContentIfExists(directoryPath);
    }
  }

  @Test
  public void deleteFileIfExistsTest() throws IOException {
    final String fileName = tempDirectory + "/testfile.txt";
    deleteFileIfExists(fileName);
    File testFile = new File(fileName);
    assertFalse(testFile.exists());
    try (FileOutputStream outputStream = new FileOutputStream(testFile)) {
      outputStream.write("RandomTextContent".getBytes());
    }
    assertTrue(testFile.exists());
    deleteFileIfExists(fileName);
    assertFalse(testFile.exists());
  }

  @Test
  public void deleteDirectoryAndItsContentTest() throws IOException {
    final String directoryName = getRandomTempDirectory();
    final String fileName = "/testfile.txt";
    createDirectoryIfDoesNotExist(directoryName);
    File directory = new File(directoryName);
    File testFile = new File(directory, fileName);
    try (FileOutputStream outputStream = new FileOutputStream(testFile)) {
      outputStream.write("RandomTextContent".getBytes());
    }
    assertTrue(testFile.exists());
    deleteDirectoryAndItsContentIfExists(directoryName);
    assertFalse(testFile.exists());
    assertFalse(directory.exists());
  }
}
