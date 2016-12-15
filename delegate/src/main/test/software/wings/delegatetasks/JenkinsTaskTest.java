package software.wings.delegatetasks;

/**
 * Created by rishi on 12/16/16.
 */
public class JenkinsTaskTest {
  //  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();
  //
  //  @InjectMocks private JenkinsTask jenkinsState = new JenkinsTask("jenkins");
  //

  //  @Before
  //  public void setUp() throws Exception {
  //    when(jenkinsFactory.create(anyString(), anyString(), anyString())).thenReturn(jenkins);
  //    when(jenkins.getBuild(any(QueueReference.class))).thenReturn(build);
  //    when(build.details()).thenReturn(buildWithDetails);
  //    when(buildWithDetails.isBuilding()).thenReturn(false);

  //  }

  //  @Test
  //  public void shouldExecuteSuccessfullyWhenBuildPasses() throws Exception {
  //    when(buildWithDetails.getResult()).thenReturn(BuildResult.SUCCESS);
  //    jenkinsState.execute(executionContext);
  //    verify(jenkinsFactory).create("http://jenkins", "username", "password");
  //    verify(jenkins).trigger("testjob", Collections.emptyMap());
  //    verify(jenkins).getBuild(any(QueueReference.class));
  //  }
  //
  //  @Test
  //  public void shouldFailWhenBuildFails() throws Exception {
  //    when(buildWithDetails.getResult()).thenReturn(BuildResult.FAILURE);
  //    jenkinsState.execute(executionContext);
  //    verify(jenkinsFactory).create("http://jenkins", "username", "password");
  //    verify(jenkins).trigger("testjob", Collections.emptyMap());
  //    verify(jenkins).getBuild(any(QueueReference.class));
  //  }
  //
  //  @Test
  //  public void shouldAssertArtifacts() throws Exception {
  //    jenkinsState.setFilePathsForAssertion(asList(new FilePathAssertionEntry("pom.xml", "${fileData}==\"OK\"",
  //    (FilePathAssertionEntry.Status) null))); when(buildWithDetails.getResult()).thenReturn(BuildResult.SUCCESS);
  //    ExecutionResponse executionResponse = jenkinsState.execute(executionContext);
  //    verify(jenkinsFactory).create("http://jenkins", "username", "password");
  //    verify(jenkins).trigger("testjob", Collections.emptyMap());
  //    verify(jenkins).getBuild(any(QueueReference.class));
  //  }
}
