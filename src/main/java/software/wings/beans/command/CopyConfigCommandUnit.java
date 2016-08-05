package software.wings.beans.command;

import static org.eclipse.jetty.util.LazyList.isEmpty;

import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.ConfigFile;
import software.wings.beans.ServiceTemplate;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.stencils.DefaultValue;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by anubhaw on 7/14/16.
 */
public class CopyConfigCommandUnit extends CommandUnit {
  @Attributes(title = "Destination Parent Path")
  @DefaultValue("$WINGS_RUNTIME_PATH")
  private String destinationParentPath;

  @Inject @Transient private transient ServiceTemplateService serviceTemplateService;

  /**
   * Instantiates a new Scp command unit.
   */
  public CopyConfigCommandUnit() {
    super(CommandUnitType.COPY_CONFIGS);
  }

  @Override
  public ExecutionResult execute(CommandExecutionContext context) {
    ServiceTemplate serviceTemplate = context.getServiceInstance().getServiceTemplate();
    Map<String, List<ConfigFile>> computedConfigFiles = serviceTemplateService.computedConfigFiles(
        serviceTemplate.getAppId(), serviceTemplate.getEnvId(), serviceTemplate.getUuid());
    List<ConfigFile> configFiles = computedConfigFiles.get(context.getServiceInstance().getHost().getUuid());

    ExecutionResult result = ExecutionResult.SUCCESS;
    if (!isEmpty(configFiles)) {
      for (ConfigFile configFile : configFiles) {
        String path = destinationParentPath + "/" + configFile.getRelativePath();
        result = context.copyGridFsFiles(path, FileBucket.CONFIGS, Collections.singletonList(configFile.getFileUuid()))
                == ExecutionResult.FAILURE
            ? ExecutionResult.FAILURE
            : ExecutionResult.SUCCESS;
        if (ExecutionResult.FAILURE == result) {
          break;
        }
      }
    }
    return result;
  }

  public String getDestinationParentPath() {
    return destinationParentPath;
  }

  public void setDestinationParentPath(String destinationParentPath) {
    this.destinationParentPath = destinationParentPath;
  }
}
