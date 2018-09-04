package software.wings.service.impl.yaml;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Inject;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.assertj.core.util.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import software.wings.beans.AwsInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.artifact.DockerArtifactStream;
import software.wings.beans.yaml.Change.ChangeType;
import software.wings.beans.yaml.GitFileChange;
import software.wings.service.impl.yaml.handler.YamlHandlerFactory;
import software.wings.service.intfc.yaml.EntityUpdateService;
import software.wings.service.intfc.yaml.YamlChangeSetService;
import software.wings.service.intfc.yaml.YamlDirectoryService;
import software.wings.yaml.gitSync.YamlGitConfig;
import software.wings.yaml.gitSync.YamlGitConfig.SyncMode;

import java.util.List;

public class YamlChangeSetHelperTest {
  public static final String ACCOUNTID = "000111";
  public static final String OLD = "old";
  public static final String NEW = "new";
  private YamlGitConfig yamlGitConfig;
  @Mock private YamlChangeSetService yamlChangeSetService;
  @Mock private EntityUpdateService entityUpdateService;
  @Mock private YamlHandlerFactory yamlHandlerFactory;
  @Mock private YamlDirectoryService yamlDirectoryService;
  @InjectMocks @Inject private YamlChangeSetHelper yamlChangeSetHelper;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    yamlGitConfig = YamlGitConfig.builder()
                        .accountId(ACCOUNTID)
                        .branchName("master")
                        .syncMode(SyncMode.BOTH)
                        .url("git.com")
                        .username("username")
                        .encryptedPassword("xxxxxx")
                        .webhookToken("token")
                        .build();
  }

  @Test
  public void testRenameYamlChangeForInfraMapping() throws Exception {
    GitFileChange gitFileChangeForDelete = GitFileChange.Builder.aGitFileChange()
                                               .withChangeType(ChangeType.DELETE)
                                               .withAccountId(ACCOUNTID)
                                               .withFileContent(OLD)
                                               .build();
    GitFileChange gitFileChangeForADD = GitFileChange.Builder.aGitFileChange()
                                            .withChangeType(ChangeType.ADD)
                                            .withAccountId(ACCOUNTID)
                                            .withFileContent(NEW)
                                            .build();

    // Validate for InfrastructureMapping
    when(entityUpdateService.obtainEntityGitSyncFileChangeSet(anyString(), any(), any()))
        .thenReturn(Lists.newArrayList(gitFileChangeForDelete))
        .thenReturn(Lists.newArrayList(gitFileChangeForADD));
    doNothing().when(yamlChangeSetService).saveChangeSet(any(), any());
    when(yamlDirectoryService.weNeedToPushChanges(any())).thenReturn(YamlGitConfig.builder().build());
    InfrastructureMapping oldValue =
        AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping().withName(OLD).build();
    InfrastructureMapping newValue =
        AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping().withName(NEW).build();

    MethodUtils.invokeMethod(
        yamlChangeSetHelper, true, "entityUpdateYamlChange", new Object[] {ACCOUNTID, oldValue, newValue, true});

    ArgumentCaptor<List> gitFileChangesCaptor = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<YamlGitConfig> yamlGitConfigCaptor = ArgumentCaptor.forClass(YamlGitConfig.class);
    verify(yamlChangeSetService).saveChangeSet(yamlGitConfigCaptor.capture(), gitFileChangesCaptor.capture());

    assertEquals(2, gitFileChangesCaptor.getValue().size());
    gitFileChangeForDelete = (GitFileChange) gitFileChangesCaptor.getValue().get(0);
    assertEquals(ACCOUNTID, gitFileChangeForDelete.getAccountId());
    assertEquals(ChangeType.DELETE, gitFileChangeForDelete.getChangeType());
    assertEquals(OLD, gitFileChangeForDelete.getFileContent());

    gitFileChangeForADD = (GitFileChange) gitFileChangesCaptor.getValue().get(1);
    assertEquals(ACCOUNTID, gitFileChangeForADD.getAccountId());
    assertEquals(ChangeType.ADD, gitFileChangeForADD.getChangeType());
    assertEquals(NEW, gitFileChangeForADD.getFileContent());
  }

  @Test
  public void testUpdateYamlChangeForInfraMapping() throws Exception {
    GitFileChange gitFileChangeForModify = GitFileChange.Builder.aGitFileChange()
                                               .withChangeType(ChangeType.MODIFY)
                                               .withAccountId(ACCOUNTID)
                                               .withFileContent(OLD)
                                               .build();

    // Validate for InfrastructureMapping
    when(entityUpdateService.obtainEntityGitSyncFileChangeSet(anyString(), any(), any()))
        .thenReturn(Lists.newArrayList(gitFileChangeForModify));

    doNothing().when(yamlChangeSetService).saveChangeSet(any(), any());
    when(yamlDirectoryService.weNeedToPushChanges(any())).thenReturn(YamlGitConfig.builder().build());
    InfrastructureMapping oldValue =
        AwsInfrastructureMapping.Builder.anAwsInfrastructureMapping().withName(OLD).build();

    MethodUtils.invokeMethod(
        yamlChangeSetHelper, true, "entityUpdateYamlChange", new Object[] {ACCOUNTID, oldValue, oldValue, false});

    ArgumentCaptor<List> fileChangesCaptor = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<YamlGitConfig> gitConfigCaptor = ArgumentCaptor.forClass(YamlGitConfig.class);
    verify(yamlChangeSetService).saveChangeSet(gitConfigCaptor.capture(), fileChangesCaptor.capture());

    assertEquals(1, fileChangesCaptor.getValue().size());
    gitFileChangeForModify = (GitFileChange) fileChangesCaptor.getValue().get(0);
    assertEquals(ACCOUNTID, gitFileChangeForModify.getAccountId());
    assertEquals(ChangeType.MODIFY, gitFileChangeForModify.getChangeType());
    assertEquals(OLD, gitFileChangeForModify.getFileContent());
  }

  @Test
  public void testRenameYamlChangeForArtifactStream() throws Exception {
    GitFileChange gitFileChangeForDelete = GitFileChange.Builder.aGitFileChange()
                                               .withChangeType(ChangeType.DELETE)
                                               .withAccountId(ACCOUNTID)
                                               .withFileContent(OLD)
                                               .build();
    GitFileChange gitFileChangeForADD = GitFileChange.Builder.aGitFileChange()
                                            .withChangeType(ChangeType.ADD)
                                            .withAccountId(ACCOUNTID)
                                            .withFileContent(NEW)
                                            .build();

    // Validate for Artifact Stream
    when(entityUpdateService.obtainEntityGitSyncFileChangeSet(anyString(), any(), any()))
        .thenReturn(Lists.newArrayList(gitFileChangeForDelete))
        .thenReturn(Lists.newArrayList(gitFileChangeForADD));
    when(yamlDirectoryService.weNeedToPushChanges(any())).thenReturn(YamlGitConfig.builder().build());
    doNothing().when(yamlChangeSetService).saveChangeSet(any(), any());
    ArtifactStream oldValue = new DockerArtifactStream();
    oldValue.setName(OLD);
    ArtifactStream newValue = new DockerArtifactStream();
    oldValue.setName(NEW);

    MethodUtils.invokeMethod(
        yamlChangeSetHelper, true, "entityUpdateYamlChange", new Object[] {ACCOUNTID, oldValue, newValue, true});

    ArgumentCaptor<List> gitFileChangesCaptorForAS = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<YamlGitConfig> yamlGitConfigCaptorForAS = ArgumentCaptor.forClass(YamlGitConfig.class);
    verify(yamlChangeSetService).saveChangeSet(yamlGitConfigCaptorForAS.capture(), gitFileChangesCaptorForAS.capture());

    assertEquals(2, gitFileChangesCaptorForAS.getValue().size());
    gitFileChangeForDelete = (GitFileChange) gitFileChangesCaptorForAS.getValue().get(0);
    assertEquals(ACCOUNTID, gitFileChangeForDelete.getAccountId());
    assertEquals(ChangeType.DELETE, gitFileChangeForDelete.getChangeType());
    assertEquals(OLD, gitFileChangeForDelete.getFileContent());

    gitFileChangeForADD = (GitFileChange) gitFileChangesCaptorForAS.getValue().get(1);
    assertEquals(ACCOUNTID, gitFileChangeForADD.getAccountId());
    assertEquals(ChangeType.ADD, gitFileChangeForADD.getChangeType());
    assertEquals(NEW, gitFileChangeForADD.getFileContent());
  }

  @Test
  public void testUpdateYamlChangeForArtifactStream() throws Exception {
    GitFileChange gitFileChangeForModify = GitFileChange.Builder.aGitFileChange()
                                               .withChangeType(ChangeType.MODIFY)
                                               .withAccountId(ACCOUNTID)
                                               .withFileContent(OLD)
                                               .build();

    // Validate for Artifact Stream
    when(entityUpdateService.obtainEntityGitSyncFileChangeSet(anyString(), any(), any()))
        .thenReturn(Lists.newArrayList(gitFileChangeForModify));
    when(yamlDirectoryService.weNeedToPushChanges(any())).thenReturn(YamlGitConfig.builder().build());
    doNothing().when(yamlChangeSetService).saveChangeSet(any(), any());
    ArtifactStream oldValue = new DockerArtifactStream();
    oldValue.setName(OLD);
    MethodUtils.invokeMethod(
        yamlChangeSetHelper, true, "entityUpdateYamlChange", new Object[] {ACCOUNTID, oldValue, oldValue, false});

    ArgumentCaptor<List> fileChangesCaptorForAS = ArgumentCaptor.forClass(List.class);
    ArgumentCaptor<YamlGitConfig> gitConfigCaptorForAS = ArgumentCaptor.forClass(YamlGitConfig.class);
    verify(yamlChangeSetService).saveChangeSet(gitConfigCaptorForAS.capture(), fileChangesCaptorForAS.capture());

    assertEquals(1, fileChangesCaptorForAS.getValue().size());
    gitFileChangeForModify = (GitFileChange) fileChangesCaptorForAS.getValue().get(0);
    assertEquals(ACCOUNTID, gitFileChangeForModify.getAccountId());
    assertEquals(ChangeType.MODIFY, gitFileChangeForModify.getChangeType());
    assertEquals(OLD, gitFileChangeForModify.getFileContent());
  }
}
