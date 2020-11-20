package io.harness.delegate.task.citasks.cik8handler.container;

import static io.harness.delegate.task.citasks.cik8handler.container.GitCloneContainerSpecBuilderTestHelper.gitCloneParamsWithInvalidAuth;
import static io.harness.delegate.task.citasks.cik8handler.container.GitCloneContainerSpecBuilderTestHelper.gitCloneParamsWithPassword;
import static io.harness.delegate.task.citasks.cik8handler.container.GitCloneContainerSpecBuilderTestHelper.gitCloneParamsWithSsh;
import static io.harness.delegate.task.citasks.cik8handler.container.GitCloneContainerSpecBuilderTestHelper.gitCloneParamsWithSshAndCommit;
import static io.harness.delegate.task.citasks.cik8handler.container.GitCloneContainerSpecBuilderTestHelper.gitCloneParamsWithUnsetBranch;
import static io.harness.delegate.task.citasks.cik8handler.container.GitCloneContainerSpecBuilderTestHelper.gitCloneParamsWithUnsetGitConfigParams;
import static io.harness.delegate.task.citasks.cik8handler.container.GitCloneContainerSpecBuilderTestHelper.gitCloneParamsWithUnsetGitRepoUrl;
import static io.harness.delegate.task.citasks.cik8handler.container.GitCloneContainerSpecBuilderTestHelper.gitCloneParamsWithUnsetVolume;
import static io.harness.delegate.task.citasks.cik8handler.container.GitCloneContainerSpecBuilderTestHelper.gitCloneParamsWithUnsetWorkDir;
import static io.harness.delegate.task.citasks.cik8handler.container.GitCloneContainerSpecBuilderTestHelper.gitCloneWithPwdExpectedResponse;
import static io.harness.delegate.task.citasks.cik8handler.container.GitCloneContainerSpecBuilderTestHelper.gitCloneWithSSHAndCommitExpectedResponse;
import static io.harness.delegate.task.citasks.cik8handler.container.GitCloneContainerSpecBuilderTestHelper.gitCloneWithSSHExpectedResponse;
import static io.harness.delegate.task.citasks.cik8handler.container.GitCloneContainerSpecBuilderTestHelper.gitCloneWithSSHExpectedVolumes;
import static io.harness.rule.OwnerRule.SHUBHAM;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNull;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Volume;
import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.task.citasks.cik8handler.params.GitCloneContainerParams;
import io.harness.exception.GeneralException;
import io.harness.exception.InvalidArgumentsException;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GitCloneContainerSpecBuilderTest extends CategoryTest {
  @InjectMocks private GitCloneContainerSpecBuilder gitCloneContainerSpecBuilder;
  private String stepExecVolumeName = "step-exec";
  private String stepExecWorkingDir = "workspace";
  String volumeMountPath = "/harness-" + stepExecVolumeName;

  private String gitPwdRepoUrl = "https://github.com/wings-software/portal.git";
  private String gitRepoInCmd = "github.com/wings-software/portal.git";
  private String gitSshRepoUrl = "git@github.com:wings-software/portal.git";

  private String gitBranch = "master";
  private String gitCommitId = "commit";
  private String gitSecretName = "hs-wings-software-portal-hs";

  private List<String> gitCtrCommands = Arrays.asList("/bin/sh", "-c", "--");

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
  }

  @Test()
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createGitCloneSpecWithEmptyConfig() {
    // Test 1. Empty git clone parameters.
    GitCloneContainerParams gitCloneContainerParams1 = GitCloneContainerParams.builder().build();
    assertNull(gitCloneContainerSpecBuilder.createGitCloneSpec(gitCloneContainerParams1));

    // Test 2. Unset git config.
    GitCloneContainerParams gitCloneContainerParams2 = gitCloneParamsWithUnsetGitConfigParams();
    assertNull(gitCloneContainerSpecBuilder.createGitCloneSpec(gitCloneContainerParams2));

    // Test 3. Unset git repo URL.
    GitCloneContainerParams gitCloneContainerParams3 = gitCloneParamsWithUnsetGitRepoUrl();
    assertThatThrownBy(() -> gitCloneContainerSpecBuilder.createGitCloneSpec(gitCloneContainerParams3))
        .isInstanceOf(GeneralException.class);
  }

  @Test(expected = GeneralException.class)
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createGitCloneSpecWithUnsetBranchException() {
    GitCloneContainerParams gitCloneContainerParams = gitCloneParamsWithUnsetBranch();
    gitCloneContainerSpecBuilder.createGitCloneSpec(gitCloneContainerParams);
  }

  @Test(expected = GeneralException.class)
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createGitCloneSpecWithUnsetWorkDirException() {
    GitCloneContainerParams gitCloneContainerParams = gitCloneParamsWithUnsetWorkDir();
    gitCloneContainerSpecBuilder.createGitCloneSpec(gitCloneContainerParams);
  }

  @Test(expected = GeneralException.class)
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createGitCloneSpecWithUnsetStepVolumeException() {
    GitCloneContainerParams gitCloneContainerParams = gitCloneParamsWithUnsetVolume();
    gitCloneContainerSpecBuilder.createGitCloneSpec(gitCloneContainerParams);
  }

  @Test(expected = InvalidArgumentsException.class)
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createGitCloneSpecWithInvalidAuthScemeException() {
    GitCloneContainerParams gitCloneContainerParams = gitCloneParamsWithInvalidAuth();
    gitCloneContainerSpecBuilder.createGitCloneSpec(gitCloneContainerParams);
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createGitCloneSpecWithPassword() {
    GitCloneContainerParams gitCloneContainerParams = gitCloneParamsWithPassword();
    Container expectedContainer = gitCloneWithPwdExpectedResponse();

    ContainerSpecBuilderResponse response = gitCloneContainerSpecBuilder.createGitCloneSpec(gitCloneContainerParams);
    assertEquals(expectedContainer, response.getContainerBuilder().build());
    assertEquals(null, response.getImageSecret());
    assertEquals(new ArrayList<>(), response.getVolumes());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createGitCloneSpecWithSSH() {
    GitCloneContainerParams gitCloneContainerParams = gitCloneParamsWithSsh();

    List<Volume> expectedVolumes = gitCloneWithSSHExpectedVolumes();
    Container expectedContainer = gitCloneWithSSHExpectedResponse();

    ContainerSpecBuilderResponse response = gitCloneContainerSpecBuilder.createGitCloneSpec(gitCloneContainerParams);
    assertEquals(expectedContainer, response.getContainerBuilder().build());
    assertEquals(null, response.getImageSecret());
    assertEquals(expectedVolumes, response.getVolumes());
  }

  @Test
  @Owner(developers = SHUBHAM)
  @Category(UnitTests.class)
  public void createGitCloneSpecWithSSHAndCommit() {
    GitCloneContainerParams gitCloneContainerParams = gitCloneParamsWithSshAndCommit();

    List<Volume> expectedVolumes = gitCloneWithSSHExpectedVolumes();
    Container expectedContainer = gitCloneWithSSHAndCommitExpectedResponse();

    ContainerSpecBuilderResponse response = gitCloneContainerSpecBuilder.createGitCloneSpec(gitCloneContainerParams);
    assertEquals(expectedContainer, response.getContainerBuilder().build());
    assertEquals(null, response.getImageSecret());
    assertEquals(expectedVolumes, response.getVolumes());
  }
}
