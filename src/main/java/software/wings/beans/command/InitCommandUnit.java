package software.wings.beans.command;

import static com.google.common.collect.ImmutableMap.of;
import static freemarker.template.Configuration.VERSION_2_3_23;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import freemarker.cache.ClassTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import org.mongodb.morphia.annotations.Transient;
import software.wings.service.intfc.ServiceResourceService;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * Created by peeyushaggarwal on 7/26/16.
 */
@Attributes(description = "This command unit creates STATING_PATH, RUNTIME_PATH, BACKUP_PATH on the target host")
public class InitCommandUnit extends CommandUnit {
  @Transient private final Configuration cfg = new Configuration(VERSION_2_3_23);

  @Transient @Inject private ServiceResourceService serviceResourceService;

  @SchemaIgnore @Transient private Command command;

  @SchemaIgnore private String activityId;

  @Transient @SchemaIgnore private Map<String, String> envVariables = Maps.newHashMap();

  @Transient @SchemaIgnore private String preInitCommand;

  @Transient @SchemaIgnore private File launcherFile;

  @Override
  public void setup(CommandExecutionContext context) {
    cfg.setTemplateLoader(new ClassTemplateLoader(getClass(), "/commandtemplates"));
    preInitCommand = "mkdir -p " + context.getStagingPath() + " && mkdir -p " + context.getBackupPath()
        + " && mkdir -p " + context.getRuntimePath() + " && rm -rf " + context.getStagingPath() + "/*";
    envVariables.put("STAGING_PATH", context.getStagingPath());
    envVariables.put("RUNTIME_PATH", context.getRuntimePath());
    envVariables.put("BACKUP_PATH", context.getBackupPath());
    activityId = context.getActivityId();
  }

  public String getPreInitCommand() {
    return preInitCommand;
  }

  public String getLauncherFile() throws IOException, TemplateException {
    String launcherScript = System.getProperty("java.io.tmpdir") + "/wingslauncher" + activityId + ".sh";
    try (OutputStreamWriter fileWriter =
             new OutputStreamWriter(new FileOutputStream(launcherScript), StandardCharsets.UTF_8)) {
      cfg.getTemplate("execlauncher.ftl").process(of("envVariables", envVariables), fileWriter);
    }
    return launcherScript;
  }

  public List<String> getUnitFilesInfo() {
    return Lists.newArrayList();
  }

  public void setCommand(Command command) {
    this.command = command;
  }

  public static final class Builder {
    private String name;

    private Builder() {}

    public static Builder anInitCommandUnit() {
      return new Builder();
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder but() {
      return anInitCommandUnit().withName(name);
    }

    public InitCommandUnit build() {
      InitCommandUnit initCommandUnit = new InitCommandUnit();
      initCommandUnit.setName(name);
      return initCommandUnit;
    }
  }
}
