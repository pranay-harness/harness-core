package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.AppContainer.AppContainerBuilder.anAppContainer;
import static software.wings.beans.ArtifactSource.ArtifactType.JAR;
import static software.wings.beans.ArtifactSource.ArtifactType.WAR;
import static software.wings.beans.Command.Builder.aCommand;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.ExecCommandUnit.Builder.anExecCommandUnit;
import static software.wings.beans.Graph.Builder.aGraph;
import static software.wings.beans.Graph.Link.Builder.aLink;
import static software.wings.beans.Graph.Node.Builder.aNode;
import static software.wings.beans.Graph.ORIGIN_STATE;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.Service.Builder.aService;
import static software.wings.utils.MorphiaMatcher.sameQueryAs;
import static software.wings.utils.MorphiaMatcher.sameUpdateOperationsAs;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Verifier;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.Application;
import software.wings.beans.Command;
import software.wings.beans.ConfigFile;
import software.wings.beans.Graph;
import software.wings.beans.SearchFilter;
import software.wings.beans.Service;
import software.wings.beans.Service.Builder;
import software.wings.dl.PageRequest;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.ServiceResourceService;

import java.util.ArrayList;
import java.util.Map;

// TODO: Auto-generated Javadoc

/**
 * Created by anubhaw on 5/4/16.
 */
public class ServiceResourceServiceTest extends WingsBaseTest {
  private static final Command.Builder commandBuilder = aCommand().withName("START").addCommandUnits(
      anExecCommandUnit().withCommandPath("/home/xxx/tomcat").withCommandString("bin/startup.sh").build());
  private static final Builder builder = aService()
                                             .withUuid(SERVICE_ID)
                                             .withAppId(APP_ID)
                                             .withName("SERVICE_NAME")
                                             .withDescription("SERVICE_DESC")
                                             .withArtifactType(JAR)
                                             .withAppContainer(anAppContainer().withUuid("APP_CONTAINER_ID").build());

  @Inject @Named("primaryDatastore") private Datastore datastore;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private ConfigService configService;
  /**
   * The Verifier.
   */
  @Rule
  public Verifier verifier = new Verifier() {
    @Override
    protected void verify() throws Throwable {
      verifyNoMoreInteractions(configService, wingsPersistence);
    }
  };
  @Inject @InjectMocks private ServiceResourceService srs;

  /**
   * Sets the up.
   *
   * @throws Exception the exception
   */
  @Before
  public void setUp() throws Exception {
    when(wingsPersistence.saveAndGet(eq(Service.class), any(Service.class))).thenReturn(builder.but().build());
    when(wingsPersistence.get(eq(Service.class), anyString(), anyString())).thenReturn(builder.but().build());
    when(wingsPersistence.createQuery(Service.class)).thenReturn(datastore.createQuery(Service.class));
    when(wingsPersistence.createUpdateOperations(Service.class))
        .thenReturn(datastore.createUpdateOperations(Service.class));
    when(wingsPersistence.addToList(
             eq(Service.class), anyString(), anyString(), any(Query.class), anyString(), any(Command.class)))
        .thenReturn(true);
  }

  /**
   * Should list services.
   */
  @Test
  public void shouldListServices() {
    PageRequest<Service> request = new PageRequest<>();
    request.addFilter("appId", APP_ID, EQ);
    srs.list(request);
    ArgumentCaptor<PageRequest> argument = ArgumentCaptor.forClass(PageRequest.class);
    verify(wingsPersistence).query(eq(Service.class), argument.capture());
    SearchFilter filter = (SearchFilter) argument.getValue().getFilters().get(0);
    assertThat(filter.getFieldName()).isEqualTo("appId");
    assertThat(filter.getFieldValues()).containsExactly(APP_ID);
    assertThat(filter.getOp()).isEqualTo(EQ);
  }

  /**
   * Should save service.
   */
  @Test
  public void shouldSaveService() {
    Service service = srs.save(builder.but().build());
    assertThat(service.getUuid()).isEqualTo(SERVICE_ID);
    verify(wingsPersistence).addToList(Application.class, service.getAppId(), "services", service);
    verify(wingsPersistence).saveAndGet(Service.class, service);
    verify(wingsPersistence, times(6)).get(Service.class, APP_ID, SERVICE_ID);
    verify(wingsPersistence, times(3))
        .addToList(eq(Service.class), eq(APP_ID), eq(SERVICE_ID), any(Query.class), anyString(), anyString());
    verify(wingsPersistence, times(3)).createQuery(Service.class);
    verify(configService, times(3)).getConfigFilesForEntity(DEFAULT_TEMPLATE_ID, SERVICE_ID);
  }

  /**
   * Should fetch service.
   */
  @Test
  public void shouldFetchService() {
    when(wingsPersistence.get(Service.class, APP_ID, SERVICE_ID)).thenReturn(builder.but().build());
    when(configService.getConfigFilesForEntity(DEFAULT_TEMPLATE_ID, SERVICE_ID))
        .thenReturn(new ArrayList<ConfigFile>());
    srs.get(APP_ID, SERVICE_ID);
    verify(wingsPersistence).get(Service.class, APP_ID, SERVICE_ID);
    verify(configService).getConfigFilesForEntity(DEFAULT_TEMPLATE_ID, SERVICE_ID);
  }

  /**
   * Should update service.
   */
  @Test
  public void shouldUpdateService() {
    Service service = builder.withName("UPDATED_SERVICE_NAME")
                          .withDescription("UPDATED_SERVICE_DESC")
                          .withArtifactType(WAR)
                          .withAppContainer(anAppContainer().withUuid("UPDATED_APP_CONTAINER_ID").build())
                          .build();
    srs.update(service);
    verify(wingsPersistence)
        .updateFields(Service.class, SERVICE_ID,
            ImmutableMap.of("name", "UPDATED_SERVICE_NAME", "description", "UPDATED_SERVICE_DESC", "artifactType", WAR,
                "appContainer", anAppContainer().withUuid("UPDATED_APP_CONTAINER_ID").build()));
    verify(wingsPersistence).get(Service.class, APP_ID, SERVICE_ID);
  }

  /**
   * Should delete service.
   */
  @Test
  public void shouldDeleteService() {
    srs.delete(APP_ID, SERVICE_ID);
    verify(wingsPersistence).delete(Service.class, SERVICE_ID);
  }

  /**
   * Should add command state.
   */
  @Test
  public void shouldAddCommand() {
    when(wingsPersistence.addToList(
             eq(Service.class), eq(APP_ID), eq(SERVICE_ID), any(Query.class), eq("commands"), any(Command.class)))
        .thenReturn(true);

    Graph commandGraph =
        aGraph()
            .withGraphName("START")
            .addNodes(aNode().withId(ORIGIN_STATE).withType(ORIGIN_STATE).build(),
                aNode()
                    .withId("1")
                    .withType("EXEC")
                    .addProperty("commandPath", "/home/xxx/tomcat")
                    .addProperty("commandString", "bin/startup.sh")
                    .build())
            .addLinks(aLink().withFrom(ORIGIN_STATE).withTo("1").withType("ANY").withId("linkid").build())
            .build();

    srs.addCommand(APP_ID, SERVICE_ID, commandGraph);

    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);
    verify(wingsPersistence)
        .addToList(eq(Service.class), eq(APP_ID), eq(SERVICE_ID), any(Query.class), eq("commands"),
            eq(commandBuilder.withGraph(commandGraph).build()));
    verify(wingsPersistence).createQuery(Service.class);
    verify(configService).getConfigFilesForEntity(DEFAULT_TEMPLATE_ID, SERVICE_ID);
  }

  @Test
  public void shouldUpdateCommand() {
    when(wingsPersistence.addToList(
             eq(Service.class), eq(APP_ID), eq(SERVICE_ID), any(Query.class), eq("commands"), any(Command.class)))
        .thenReturn(true);

    Graph commandGraph =
        aGraph()
            .withGraphName("START")
            .addNodes(aNode().withId(ORIGIN_STATE).withType(ORIGIN_STATE).build(),
                aNode()
                    .withId("1")
                    .withType("EXEC")
                    .addProperty("commandPath", "/home/xxx/tomcat")
                    .addProperty("commandString", "bin/startup.sh")
                    .build())
            .addLinks(aLink().withFrom(ORIGIN_STATE).withTo("1").withType("ANY").withId("linkid").build())
            .build();

    Command expectedCommand = aCommand().withGraph(commandGraph).build();
    expectedCommand.transformGraph();

    srs.updateCommand(APP_ID, SERVICE_ID, commandGraph);

    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);

    verify(wingsPersistence)
        .update(sameQueryAs(datastore.createQuery(Service.class)
                                .field(ID_KEY)
                                .equal(SERVICE_ID)
                                .field("appId")
                                .equal(APP_ID)
                                .field("commands.name")
                                .equal(commandGraph.getGraphName())),
            sameUpdateOperationsAs(datastore.createUpdateOperations(Service.class).set("commands.$", expectedCommand)));

    verify(wingsPersistence).createUpdateOperations(Service.class);
    verify(wingsPersistence).createQuery(Service.class);
    verify(configService).getConfigFilesForEntity(DEFAULT_TEMPLATE_ID, SERVICE_ID);
  }

  /**
   * Should fail when command state is duplicate.
   */
  @Test
  public void shouldFailWhenCommandIsDuplicate() {
    when(wingsPersistence.addToList(
             eq(Service.class), eq(APP_ID), eq(SERVICE_ID), any(Query.class), eq("commands"), any(Command.class)))
        .thenReturn(false);

    Graph commandGraph =
        aGraph()
            .withGraphName("START")
            .addNodes(aNode().withId(ORIGIN_STATE).withType(ORIGIN_STATE).build(),
                aNode()
                    .withId("1")
                    .withType("EXEC")
                    .addProperty("commandPath", "/home/xxx/tomcat")
                    .addProperty("commandString", "bin/startup.sh")
                    .build())
            .addLinks(aLink().withFrom(ORIGIN_STATE).withTo("1").withType("ANY").withId("linkid").build())
            .build();

    assertThatExceptionOfType(WingsException.class).isThrownBy(() -> srs.addCommand(APP_ID, SERVICE_ID, commandGraph));

    verify(wingsPersistence).get(Service.class, APP_ID, SERVICE_ID);
    verify(wingsPersistence)
        .addToList(eq(Service.class), eq(APP_ID), eq(SERVICE_ID), any(Query.class), eq("commands"),
            eq(commandBuilder.withGraph(commandGraph).build()));
    verify(wingsPersistence).createQuery(Service.class);
  }

  /**
   * Should delete command state.
   */
  @Test
  public void shouldDeleteCommandState() {
    srs.deleteCommand(APP_ID, SERVICE_ID, "START");

    verify(wingsPersistence, times(2)).get(Service.class, APP_ID, SERVICE_ID);
    verify(wingsPersistence).createUpdateOperations(Service.class);
    verify(wingsPersistence).createQuery(Service.class);
    verify(wingsPersistence).update(any(Query.class), any());
    verify(configService).getConfigFilesForEntity(DEFAULT_TEMPLATE_ID, SERVICE_ID);
  }

  /**
   * Should get command stencils.
   */
  @Test
  public void shouldGetCommandStencils() {
    when(wingsPersistence.get(eq(Service.class), anyString(), anyString()))
        .thenReturn(builder.but().addCommands(commandBuilder.build()).build());

    Map<String, String> commandStencils = srs.getCommandStencils(APP_ID, SERVICE_ID);

    assertThat(commandStencils).isNotNull().hasSize(1).containsEntry("START", "START");

    verify(wingsPersistence, times(1)).get(Service.class, APP_ID, SERVICE_ID);
    verify(configService).getConfigFilesForEntity(DEFAULT_TEMPLATE_ID, SERVICE_ID);
  }

  /**
   * Should get command by name.
   */
  @Test
  public void shouldGetCommandByName() {
    when(wingsPersistence.get(eq(Service.class), anyString(), anyString()))
        .thenReturn(builder.but().addCommands(commandBuilder.build()).build());

    assertThat(srs.getCommandByName(APP_ID, SERVICE_ID, "START")).isNotNull();

    verify(wingsPersistence, times(1)).get(Service.class, APP_ID, SERVICE_ID);
    verify(configService).getConfigFilesForEntity(DEFAULT_TEMPLATE_ID, SERVICE_ID);
  }
}
