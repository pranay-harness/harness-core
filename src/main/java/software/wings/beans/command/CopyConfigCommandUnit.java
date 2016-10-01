package software.wings.beans.command;

import static org.eclipse.jetty.util.LazyList.isEmpty;

import com.google.common.base.MoreObjects;
import com.google.inject.Inject;

import com.github.reinert.jjschema.Attributes;
import org.apache.commons.lang3.tuple.Pair;
import org.mongodb.morphia.annotations.Transient;
import software.wings.beans.ConfigFile;
import software.wings.beans.ServiceTemplate;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.stencils.DefaultValue;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
    ServiceTemplate serviceTemplate = context.getServiceTemplate();
    Map<String, List<ConfigFile>> computedConfigFiles = serviceTemplateService.computedConfigFiles(
        serviceTemplate.getAppId(), serviceTemplate.getEnvId(), serviceTemplate.getUuid());
    List<ConfigFile> configFiles = computedConfigFiles.get(context.getHost().getUuid());

    ExecutionResult result = ExecutionResult.SUCCESS;
    if (!isEmpty(configFiles)) {
      for (ConfigFile configFile : configFiles) {
        File destFile = new File(configFile.getRelativeFilePath());
        String path = destinationParentPath + "/" + destFile.getParent();
        result = context.copyGridFsFiles(path, FileBucket.CONFIGS,
                     Collections.singletonList(Pair.of(configFile.getFileUuid(), destFile.getName())))
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

  /**
   * Gets destination parent path.
   *
   * @return the destination parent path
   */
  public String getDestinationParentPath() {
    return destinationParentPath;
  }

  /**
   * Sets destination parent path.
   *
   * @param destinationParentPath the destination parent path
   */
  public void setDestinationParentPath(String destinationParentPath) {
    this.destinationParentPath = destinationParentPath;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("destinationParentPath", destinationParentPath)
        .add("serviceTemplateService", serviceTemplateService)
        .toString();
  }

  @Override
  public int hashCode() {
    return Objects.hash(destinationParentPath);
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final CopyConfigCommandUnit other = (CopyConfigCommandUnit) obj;
    return Objects.equals(this.destinationParentPath, other.destinationParentPath);
  }
}
