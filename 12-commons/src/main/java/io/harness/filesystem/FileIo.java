package io.harness.filesystem;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class FileIo {
  public static void createDirectoryIfDoesNotExist(final String directoryPath) throws IOException {
    Path path = Paths.get(directoryPath);
    try {
      if (!Files.exists(path)) {
        Files.createDirectories(path);
      }
    } catch (FileAlreadyExistsException e) {
      // Ignore.
    }
  }

  public static void deleteFileIfExists(final String filePath) throws IOException {
    Files.deleteIfExists(Paths.get(filePath));
  }

  public static void deleteDirectoryAndItsContentIfExists(final String directoryPath) throws IOException {
    Files.walkFileTree(Paths.get(directoryPath), new SimpleFileVisitor<Path>() {
      @Override
      public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Files.deleteIfExists(file);
        return FileVisitResult.CONTINUE;
      }

      @Override
      public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
        if (exc != null) {
          throw exc;
        }
        Files.deleteIfExists(dir);
        return FileVisitResult.CONTINUE;
      }
    });
  }

  public static void writeUtf8StringToFile(final String directoryPath, String content) throws IOException {
    Files.write(Paths.get(directoryPath), content.getBytes(StandardCharsets.UTF_8));
  }

  public static void writeFile(final String directoryPath, byte[] content) throws IOException {
    Files.write(Paths.get(directoryPath), content);
  }
}
