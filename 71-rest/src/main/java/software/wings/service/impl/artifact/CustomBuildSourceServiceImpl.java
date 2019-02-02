package software.wings.service.impl.artifact;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static software.wings.beans.DelegateTask.SyncTaskContext.Builder.aContext;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.artifact.CustomArtifactStream;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.helpers.ext.jenkins.BuildDetails;
import software.wings.service.impl.ServiceClassLocator;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.BuildService;
import software.wings.service.intfc.artifact.CustomBuildSourceService;
import software.wings.utils.Validator;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import javax.validation.executable.ValidateOnExecution;

@Singleton
@ValidateOnExecution
public class CustomBuildSourceServiceImpl implements CustomBuildSourceService {
  private static final Logger logger = LoggerFactory.getLogger(CustomBuildSourceServiceImpl.class);
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private ServiceClassLocator serviceLocator;
  @Inject private DelegateProxyFactory delegateProxyFactory;
  @Inject private ArtifactCollectionUtil artifactCollectionUtil;

  @Override
  public List<BuildDetails> getBuilds(String appId, String artifactStreamId) {
    logger.info("Retrieving the builds for Custom Repository artifactStreamId {}", artifactStreamId);
    ArtifactStream artifactStream = artifactStreamService.get(appId, artifactStreamId);
    Validator.notNullCheck("Artifact source does not exist", artifactStream, USER);

    CustomArtifactStream customArtifactStream = (CustomArtifactStream) artifactStream;

    // TODO: The rendering expression should be moved to delegate once the Framework is ready
    ArtifactStreamAttributes artifactStreamAttributes =
        artifactCollectionUtil.renderCustomArtifactScriptString(customArtifactStream);

    // Defaulting to the 60 secs
    long timeout = isEmpty(artifactStreamAttributes.getCustomScriptTimeout())
        ? Long.parseLong(CustomArtifactStream.DEFAULT_SCRIPT_TIME_OUT)
        : Long.parseLong(artifactStreamAttributes.getCustomScriptTimeout());
    List<String> tags = customArtifactStream.getTags();
    if (isNotEmpty(tags)) {
      // To remove if any empty tags in case saved for custom artifact stream
      tags = tags.stream().filter(s -> isNotEmpty(s)).distinct().collect(Collectors.toList());
    }

    SyncTaskContext syncTaskContext = aContext()
                                          .withAccountId(artifactStreamAttributes.getAccountId())
                                          .withAppId(appId)
                                          .withTimeout(Duration.ofSeconds(timeout).toMillis())
                                          .withTags(tags)
                                          .build();

    Class<? extends BuildService> buildServiceClass =
        serviceLocator.getBuildServiceClass(customArtifactStream.getArtifactStreamType());
    return delegateProxyFactory.get(buildServiceClass, syncTaskContext).getBuilds(artifactStreamAttributes);
  }
}
