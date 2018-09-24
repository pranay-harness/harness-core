package software.wings.rules;

import static java.lang.String.format;
import static java.util.Arrays.asList;

import org.apache.sshd.common.channel.PtyMode;
import org.apache.sshd.common.util.GenericUtils;
import org.apache.sshd.common.util.ValidateUtils;
import org.apache.sshd.common.util.io.IoUtils;
import org.apache.sshd.common.util.logging.AbstractLoggingBean;
import org.apache.sshd.server.Environment;
import org.apache.sshd.server.channel.PuttyRequestHandler;
import org.apache.sshd.server.session.ServerSession;
import org.apache.sshd.server.session.ServerSessionHolder;
import org.apache.sshd.server.shell.InvertedShell;
import org.apache.sshd.server.shell.TtyFilterInputStream;
import org.apache.sshd.server.shell.TtyFilterOutputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.FileSystem;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by peeyushaggarwal on 7/27/16.
 */
public class FileSystemAwareProcessShell extends AbstractLoggingBean implements InvertedShell, ServerSessionHolder {
  private final List<String> command;
  private String cmdValue;
  private ServerSession session;
  private Process process;
  private TtyFilterOutputStream in;
  private TtyFilterInputStream out;
  private TtyFilterInputStream err;
  private FileSystem root;

  /**
   * Instantiates a new File system aware process shell.
   *
   * @param command the command
   */
  public FileSystemAwareProcessShell(String... command) {
    this(GenericUtils.isEmpty(command) ? Collections.<String>emptyList() : asList(command));
  }

  /**
   * Instantiates a new File system aware process shell.
   *
   * @param command the command
   */
  public FileSystemAwareProcessShell(Collection<String> command) {
    this.command = new ArrayList<>(ValidateUtils.checkNotNullAndNotEmpty(command, "No process shell command(s)"));
    this.cmdValue = GenericUtils.join(command, ' ');
  }

  /**
   * Sets root.
   *
   * @param root the root
   */
  public void setRoot(FileSystem root) {
    this.root = root;
  }

  @Override
  public ServerSession getServerSession() {
    return session;
  }

  @Override
  public void setSession(ServerSession session) {
    this.session = ValidateUtils.checkNotNull(session, "No server session");
    ValidateUtils.checkTrue(process == null, "Session set after process started");
  }

  @Override
  public void start(Environment env) throws IOException {
    Map<String, String> varsMap = resolveShellEnvironment(env.getEnv());
    for (int i = 0; i < command.size(); i++) {
      String cmd = command.get(i);
      if ("$USER".equals(cmd)) {
        cmd = varsMap.get("USER");
        command.set(i, cmd);
        cmdValue = GenericUtils.join(command, ' ');
      }
    }

    ProcessBuilder builder = new ProcessBuilder(command);
    if (GenericUtils.size(varsMap) > 0) {
      try {
        Map<String, String> procEnv = builder.environment();
        procEnv.putAll(varsMap);
      } catch (Exception e) {
        log.warn(
            format("start() - Failed (%s) to set environment for command=%s", e.getClass().getSimpleName(), cmdValue),
            e);
        if (log.isDebugEnabled()) {
          log.debug("start(" + cmdValue + ") failure details: " + e.getMessage(), e);
          for (StackTraceElement elem : e.getStackTrace()) {
            log.debug("Trace: {}", elem);
          }
        }
      }
    }

    if (log.isDebugEnabled()) {
      log.debug("Starting shell with command: '{}' and env: {}", builder.command(), builder.environment());
    }

    if (root != null) {
      builder.directory(new File(root.toString()));
    }
    process = builder.start();

    Map<PtyMode, ?> modes = resolveShellTtyOptions(env.getPtyModes());
    out = new TtyFilterInputStream(process.getInputStream(), modes);
    err = new TtyFilterInputStream(process.getErrorStream(), modes);
    in = new TtyFilterOutputStream(process.getOutputStream(), err, modes);
  }

  /**
   * Resolve shell environment map.
   *
   * @param env the env
   * @return the map
   */
  protected Map<String, String> resolveShellEnvironment(Map<String, String> env) {
    return env;
  }

  /**
   * Resolve shell tty options map.
   *
   * @param modes the modes
   * @return the map
   */
  // for some reason these modes provide best results BOTH with Linux SSH client and PUTTY
  protected Map<PtyMode, Integer> resolveShellTtyOptions(Map<PtyMode, Integer> modes) {
    if (PuttyRequestHandler.isPuttyClient(getServerSession())) {
      return PuttyRequestHandler.resolveShellTtyOptions(modes);
    } else {
      return modes;
    }
  }

  @Override
  public OutputStream getInputStream() {
    return in;
  }

  @Override
  public InputStream getOutputStream() {
    return out;
  }

  @Override
  public InputStream getErrorStream() {
    return err;
  }

  @Override
  public boolean isAlive() {
    // TODO in JDK-8 call process.isAlive()
    try {
      process.exitValue();
      return false;
    } catch (IllegalThreadStateException e) {
      return true;
    }
  }

  @Override
  public int exitValue() {
    // TODO in JDK-8 call process.isAlive()
    if (isAlive()) {
      try {
        return process.waitFor();
      } catch (InterruptedException e) {
        throw new RuntimeException(e);
      }
    } else {
      return process.exitValue();
    }
  }

  @Override
  public void destroy() {
    // NOTE !!! DO NOT NULL-IFY THE PROCESS SINCE "exitValue" is called subsequently
    if (process != null) {
      if (log.isDebugEnabled()) {
        log.debug("Destroy process for " + cmdValue);
      }
      process.destroy();
    }

    IOException e = IoUtils.closeQuietly(getInputStream(), getOutputStream(), getErrorStream());
    if (e != null) {
      if (log.isDebugEnabled()) {
        log.debug(e.getClass().getSimpleName() + " while destroy streams of '" + this + "': " + e.getMessage());
      }

      if (log.isTraceEnabled()) {
        Throwable[] suppressed = e.getSuppressed();
        if (GenericUtils.length(suppressed) > 0) {
          for (Throwable t : suppressed) {
            log.trace("Suppressed " + t.getClass().getSimpleName() + ") while destroy streams of '" + this
                + "': " + t.getMessage());
          }
        }
      }
    }
  }

  @Override
  public String toString() {
    return GenericUtils.isEmpty(cmdValue) ? super.toString() : cmdValue;
  }
}
