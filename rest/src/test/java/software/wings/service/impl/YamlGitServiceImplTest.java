package software.wings.service.impl;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.spy;

import org.apache.commons.lang3.reflect.MethodUtils;
import org.junit.Test;
import software.wings.beans.yaml.GitFileChange;
import software.wings.exception.WingsException;
import software.wings.service.impl.yaml.YamlGitServiceImpl;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

public class YamlGitServiceImplTest {
  @Test
  public void testCheckForValidNameSyntax() throws Exception {
    List<GitFileChange> gitFileChanges = new ArrayList<>();
    gitFileChanges.add(GitFileChange.Builder.aGitFileChange().withFilePath("Setup/Defaults.yaml").build());
    gitFileChanges.add(
        GitFileChange.Builder.aGitFileChange().withFilePath("Setup/Applications/App1/Index.yaml").build());
    gitFileChanges.add(
        GitFileChange.Builder.aGitFileChange().withFilePath("Setup/Artifact Servers/jenkins.yaml").build());
    gitFileChanges.add(
        GitFileChange.Builder.aGitFileChange().withFilePath("Setup/Verification Providers/NewRelic.yaml").build());
    gitFileChanges.add(GitFileChange.Builder.aGitFileChange()
                           .withFilePath("Setup/Applications/app1/Services/service1/Index.yaml")
                           .build());
    gitFileChanges.add(GitFileChange.Builder.aGitFileChange().withFilePath("Setup/Applications").build());
    gitFileChanges.add(GitFileChange.Builder.aGitFileChange().withFilePath("Setup/Applications/app1/Services").build());
    gitFileChanges.add(GitFileChange.Builder.aGitFileChange().withFilePath("Setup/Verification Providers").build());

    YamlGitServiceImpl yamlGitService = spy(YamlGitServiceImpl.class);
    MethodUtils.invokeMethod(yamlGitService, true, "checkForValidNameSyntax", gitFileChanges);

    gitFileChanges.add(GitFileChange.Builder.aGitFileChange()
                           .withFilePath("Setup/Applications/app1/Services/service/1/Index.yaml")
                           .build());
    try {
      MethodUtils.invokeMethod(yamlGitService, true, "checkForValidNameSyntax", gitFileChanges);
      assertTrue(false);
    } catch (InvocationTargetException ex) {
      assertTrue(ex.getTargetException() instanceof WingsException);
      assertTrue(ex.getTargetException().getMessage().contains(
          "Invalid entity name, entity can not contain / in the name. Caused invalid file path:"));
    }
  }
}
