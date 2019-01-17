package io.harness.filesystem;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.SYNC;
import static java.nio.file.StandardOpenOption.WRITE;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.TimeUnit;

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
    if (!Files.exists(Paths.get(directoryPath))) {
      return;
    }

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
    Files.write(Paths.get(directoryPath), content.getBytes(StandardCharsets.UTF_8), CREATE, WRITE, SYNC);
  }

  public static void writeFile(final String directoryPath, byte[] content) throws IOException {
    Files.write(Paths.get(directoryPath), content, CREATE, WRITE, SYNC);
  }

  public static boolean acquireLock(File file) {
    File lockFile = new File(file.getPath() + ".lock");
    final long finishAt = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(5);
    boolean wasInterrupted = false;
    try {
      while (lockFile.exists()) {
        final long remaining = finishAt - System.currentTimeMillis();
        if (remaining < 0) {
          break;
        }
        try {
          Thread.sleep(Math.min(100, remaining));
        } catch (InterruptedException e) {
          wasInterrupted = true;
          return false;
        }
      }
      FileUtils.touch(lockFile);
      return true;
    } catch (Exception e) {
      return false;
    } finally {
      if (wasInterrupted) {
        Thread.currentThread().interrupt();
      }
    }
  }

  public static boolean releaseLock(File file) {
    File lockFile = new File(file.getPath() + ".lock");
    try {
      if (lockFile.exists()) {
        FileUtils.forceDelete(lockFile);
      }
      return true;
    } catch (Exception e) {
      return false;
    }
  }

  public static boolean isLocked(File file) {
    File lockFile = new File(file.getPath() + ".lock");
    return lockFile.exists();
  }
}
