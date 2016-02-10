package software.wings.core.executors;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static software.wings.utils.Misc.quietSleep;

/**
 * Created by anubhaw on 2/8/16.
 */
public class SSHPwdAuthExecutor extends AbstractExecutor {
  @Override
  public void preInit() {}

  @Override
  public void postInit() {}

  @Override
  public void preExecute() {}

  @Override
  public void postExecute() {}
}
