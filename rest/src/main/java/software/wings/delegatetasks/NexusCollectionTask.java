package software.wings.delegatetasks;

import static software.wings.beans.BambooConfig.Builder.aBambooConfig;
import static software.wings.delegatetasks.DelegateFile.Builder.aDelegateFile;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.inject.Inject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.BambooConfig;
import software.wings.beans.DelegateTask;
import software.wings.beans.artifact.ArtifactFile;
import software.wings.helpers.ext.bamboo.BambooService;
import software.wings.helpers.ext.nexus.NexusService;
import software.wings.waitnotify.ListNotifyResponseData;
import software.wings.waitnotify.NotifyResponseData;

/**
 * Created by srinivas on 4/4/17.
 */
public class NexusCollectionTask extends AbstractDelegateRunnableTask {
  private static final Logger logger = LoggerFactory.getLogger(NexusCollectionTask.class);

  @Inject private NexusService nexusService;
  @Inject private DelegateFileManager delegateFileManager;

  public NexusCollectionTask(String delegateId, DelegateTask delegateTask, Consumer<ListNotifyResponseData> postExecute,
      Supplier<Boolean> preExecute) {
    super(delegateId, delegateTask, postExecute, preExecute);
  }

  @Override
  public NotifyResponseData run(Object[] parameters) {
    return run((String) parameters[0], (String) parameters[1], (String) parameters[2], (String) parameters[3],
        (List<String>) parameters[4], (Map<String, String>) parameters[5]);
  }

  public ListNotifyResponseData run(String bambooUrl, String username, String password, String planKey,
      List<String> artifactPaths, Map<String, String> arguments) {
    return null;
  }
}
