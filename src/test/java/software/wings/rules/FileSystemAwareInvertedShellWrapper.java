package software.wings.rules;

import org.apache.sshd.common.file.FileSystemAware;
import org.apache.sshd.server.shell.InvertedShell;
import org.apache.sshd.server.shell.InvertedShellWrapper;

import java.nio.file.FileSystem;
import java.util.concurrent.Executor;

/**
 * Created by peeyushaggarwal on 7/27/16.
 */
public class FileSystemAwareInvertedShellWrapper extends InvertedShellWrapper implements FileSystemAware {
  private FileSystemAwareProcessShell fileSystemAwareProcessShell;
  public FileSystemAwareInvertedShellWrapper(InvertedShell shell) {
    this(shell, DEFAULT_BUFFER_SIZE);
  }

  public FileSystemAwareInvertedShellWrapper(InvertedShell shell, int bufferSize) {
    this(shell, null, true, bufferSize);
  }

  public FileSystemAwareInvertedShellWrapper(
      InvertedShell shell, Executor executor, boolean shutdownExecutor, int bufferSize) {
    super(shell, executor, shutdownExecutor, bufferSize);
    fileSystemAwareProcessShell = (FileSystemAwareProcessShell) shell;
  }

  @Override
  public void setFileSystem(FileSystem fileSystem) {
    fileSystemAwareProcessShell.setRoot(fileSystem);
  }
}
