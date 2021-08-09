package io.harness.pms.gitsync;

import static io.harness.annotations.dev.HarnessTeam.PIPELINE;
import static io.harness.rule.OwnerRule.NAMAN;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.PipelineServiceTestBase;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.context.GlobalContext;
import io.harness.gitsync.interceptor.GitEntityInfo;
import io.harness.gitsync.interceptor.GitSyncBranchContext;
import io.harness.gitsync.sdk.EntityGitDetails;
import io.harness.manage.GlobalContextManager;
import io.harness.rule.Owner;
import io.harness.serializer.KryoSerializer;

import com.google.inject.Inject;
import com.google.protobuf.ByteString;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@OwnedBy(PIPELINE)
public class PmsGitSyncHelperTest extends PipelineServiceTestBase {
  @Inject PmsGitSyncHelper pmsGitSyncHelper;
  @Inject KryoSerializer kryoSerializer;

  GlobalContext context;
  ByteString contextBytes;

  String pipelineBranch = "master";
  String pipelineRepoID = "be-repo-1";

  @Before
  public void setup() {
    GitSyncBranchContext gitSyncBranchContext =
        GitSyncBranchContext.builder()
            .gitBranchInfo(GitEntityInfo.builder().branch(pipelineBranch).yamlGitConfigId(pipelineRepoID).build())
            .build();
    context = new GlobalContext();
    context.setGlobalContextRecord(gitSyncBranchContext);
    GlobalContextManager.set(context);
    contextBytes = ByteString.copyFrom(kryoSerializer.asDeflatedBytes(gitSyncBranchContext));
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetEntityGitDetailsFromBytes() {
    EntityGitDetails entityGitDetails = pmsGitSyncHelper.getEntityGitDetailsFromBytes(contextBytes);
    assertThat(entityGitDetails.getBranch()).isEqualTo(pipelineBranch);
    assertThat(entityGitDetails.getRepoIdentifier()).isEqualTo(pipelineRepoID);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testGetGitSyncBranchContextBytesThreadLocal() {
    ByteString gitSyncBranchContextBytesThreadLocal = pmsGitSyncHelper.getGitSyncBranchContextBytesThreadLocal();
    assertThat(gitSyncBranchContextBytesThreadLocal).isNotEmpty();
    assertThat(gitSyncBranchContextBytesThreadLocal).isEqualTo(contextBytes);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testDeserializeGitSyncBranchContext() {
    GitSyncBranchContext gitSyncBranchContext = pmsGitSyncHelper.deserializeGitSyncBranchContext(contextBytes);
    assertThat(gitSyncBranchContext.getGitBranchInfo().getBranch()).isEqualTo(pipelineBranch);
    assertThat(gitSyncBranchContext.getGitBranchInfo().getYamlGitConfigId()).isEqualTo(pipelineRepoID);
  }

  @Test
  @Owner(developers = NAMAN)
  @Category(UnitTests.class)
  public void testSerializeGitSyncBranchContext() {
    ByteString byteString =
        pmsGitSyncHelper.serializeGitSyncBranchContext(context.get(GitSyncBranchContext.NG_GIT_SYNC_CONTEXT));
    assertThat(byteString).isEqualTo(contextBytes);
  }
}