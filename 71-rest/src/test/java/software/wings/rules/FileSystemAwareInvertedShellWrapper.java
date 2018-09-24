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

  /**
   * Instantiates a new File system aware inverted shell wrapper.
   *
   * @param shell the shell
   */
  public FileSystemAwareInvertedShellWrapper(InvertedShell shell) {
    this(shell, DEFAULT_BUFFER_SIZE);
  }

  /**
   * Instantiates a new File system aware inverted shell wrapper.
   *
   * @param shell      the shell
   * @param bufferSize the buffer size
   */
  public FileSystemAwareInvertedShellWrapper(InvertedShell shell, int bufferSize) {
    this(shell, null, true, bufferSize);
  }

  /**
   * Instantiates a new File system aware inverted shell wrapper.
   *
   * @param shell            the shell
   * @param executor         the executor
   * @param shutdownExecutor the shutdown executor
   * @param bufferSize       the buffer size
   */
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
