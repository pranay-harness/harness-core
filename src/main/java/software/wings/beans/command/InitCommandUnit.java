package software.wings.beans.command;

import com.google.common.collect.Lists;

import com.github.reinert.jjschema.SchemaIgnore;
import org.apache.commons.jexl3.JxltEngine.Exception;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import software.wings.core.ssh.executors.AbstractSshExecutor.FileProvider;

import java.io.File;
import java.io.OutputStream;
import java.util.List;

/**
 * Created by peeyushaggarwal on 7/26/16.
 */
public class InitCommandUnit extends CommandUnit {
  private Command command;

  @SchemaIgnore private String preInitCommand;

  @SchemaIgnore private File launcherFile;

  // private Pair<String,FileProvider>

  @Override
  public void setup(CommandExecutionContext context) {
    preInitCommand = "mkdir -p " + context.getStagingPath() + " && mkdir -p " + context.getBackupPath()
        + " && mkdir -p " + context.getRuntimePath();
  }

  public String getPreInitCommand() {
    return preInitCommand;
  }

  public Pair<String, FileProvider> getLauncherFileInfo() {
    return ImmutablePair.of("", new FileProvider() {
      @Override
      public Pair<String, Long> getInfo() throws Exception {
        return null;
      }

      @Override
      public void downloadToStream(OutputStream outputStream) throws Exception {}
    });
  }

  public List<Pair<String, FileProvider>> getUnitFilesInfo() {
    return Lists.newArrayList();
  }
}
