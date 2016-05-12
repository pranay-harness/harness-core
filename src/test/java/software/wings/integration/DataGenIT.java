package software.wings.integration;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.ArtifactSource.ArtifactType.WAR;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.Environment.EnvironmentBuilder.anEnvironment;
import static software.wings.common.UUIDGenerator.getUuid;
import static software.wings.integration.IntegrationTestUtil.createHostsFile;
import static software.wings.integration.IntegrationTestUtil.randomInt;

import com.google.inject.Inject;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.glassfish.jersey.media.multipart.internal.MultiPartWriter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mongodb.morphia.annotations.Embedded;
import org.mongodb.morphia.utils.ReflectionUtils;
import software.wings.WingsBaseTest;
import software.wings.beans.AppContainer;
import software.wings.beans.Application;
import software.wings.beans.Base;
import software.wings.beans.Environment;
import software.wings.beans.PageResponse;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

/**
 * Created by anubhaw on 5/6/16.
 */

@Integration
public class DataGenIT extends WingsBaseTest {
  private static final int NUM_APPS = 10; // Max 1000
  private static final int NUM_APP_CONTAINER_PER_APP = 10; // Max 1000
  private static final int NUM_SERVICES_PER_APP = 5; // Max 1000
  private static final int NUM_CONFIG_FILE_PER_SERVICE = 2; // Max 100
  private static final int NUM_ENV_PER_APP = 5; // Max 10
  private static final int NUM_HOSTS_PER_INFRA = 100; // No limits
  private Client client;

  @Rule public TemporaryFolder testFolder = new TemporaryFolder();

  @Inject private WingsPersistence wingsPersistence;

  /**
   * Generated Data for across the API use
   */

  @Before
  public void setUp() throws Exception {
    assertThat(NUM_APPS).isBetween(1, 1000);
    assertThat(NUM_APP_CONTAINER_PER_APP).isBetween(1, 1000);
    assertThat(NUM_SERVICES_PER_APP).isBetween(1, 1000);
    assertThat(NUM_CONFIG_FILE_PER_SERVICE).isBetween(0, 100);
    assertThat(NUM_ENV_PER_APP).isBetween(1, 10);

    dropDBAndEnsureIndexes();

    ClientConfig config = new ClientConfig(new JacksonJaxbJsonProvider().configure(FAIL_ON_UNKNOWN_PROPERTIES, false));
    config.register(MultiPartWriter.class);
    client = ClientBuilder.newClient(config);
  }

  private void dropDBAndEnsureIndexes() throws IOException, ClassNotFoundException {
    wingsPersistence.getDatastore().getDB().dropDatabase();
    for (final Class clazz : ReflectionUtils.getClasses("software.wings.beans", false)) {
      final Embedded embeddedAnn = ReflectionUtils.getClassEmbeddedAnnotation(clazz);
      final org.mongodb.morphia.annotations.Entity entityAnn = ReflectionUtils.getClassEntityAnnotation(clazz);
      final boolean isAbstract = Modifier.isAbstract(clazz.getModifiers());
      if ((entityAnn != null || embeddedAnn != null) && !isAbstract) {
        wingsPersistence.getDatastore().ensureIndexes(clazz);
      }
    }
  }

  @Test
  public void populateDataIT() throws IOException {
    List<Application> apps = createApplications();
    Map<String, List<AppContainer>> containers = new HashMap<>();
    Map<String, List<Service>> services = new HashMap<>();
    Map<String, List<Environment>> appEnvs = new HashMap<>();

    for (Application application : apps) {
      containers.put(application.getUuid(), addAppContainers(application.getUuid()));
      services.put(application.getUuid(), addServices(application.getUuid(), containers.get(application.getUuid())));
      appEnvs.put(application.getUuid(), addEnvs(application.getUuid()));
    }
  }

  private List<Environment> addEnvs(String appId) throws IOException {
    List<Environment> environments = new ArrayList<>();
    WebTarget target = client.target("http://localhost:9090/wings/environments?appId=" + appId);
    for (int i = 0; i < NUM_ENV_PER_APP; i++) {
      RestResponse<Environment> response = target.request().post(
          Entity.entity(
              anEnvironment().withAppId(appId).withName(envNames.get(i)).withDescription(randomText(10)).build(),
              APPLICATION_JSON),
          new GenericType<RestResponse<Environment>>() {});
      assertThat(response.getResource()).isInstanceOf(Environment.class);
      environments.add(response.getResource());
      addHostsToEnv(response.getResource());
    }
    return environments;
  }

  private void addHostsToEnv(Environment environment) throws IOException {
    WebTarget target = client.target(String.format("http://localhost:9090/wings/hosts/import/CSV?appId=%s&envId=%s",
        environment.getAppId(), environment.getUuid()));
    File file = createHostsFile(testFolder.newFile(environment.getUuid() + ".csv"), NUM_HOSTS_PER_INFRA);
    FormDataMultiPart multiPart = new FormDataMultiPart().field("sourceType", "FILE_UPLOAD");
    multiPart.bodyPart(new FileDataBodyPart("file", file));
    Response response = target.request().post(Entity.entity(multiPart, multiPart.getMediaType()));
    assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
  }

  private List<Application> createApplications() {
    List<Application> apps = new ArrayList<>();

    WebTarget target = client.target("http://localhost:9090/wings/apps/");

    for (int i = 0; i < NUM_APPS; i++) {
      String name = getName(appNames);
      RestResponse<Application> response = target.request().post(
          Entity.entity(anApplication().withName(name).withDescription(name).build(), APPLICATION_JSON),
          new GenericType<RestResponse<Application>>() {});
      assertThat(response.getResource()).isInstanceOf(Application.class);
      apps.add(response.getResource());
    }
    return apps;
  }

  private List<Service> addServices(String appId, List<AppContainer> appContainers) {
    serviceNames = new ArrayList<String>(seedNames);
    WebTarget target = client.target("http://localhost:9090/wings/services/?appId=" + appId);
    List<Service> services = new ArrayList<>();

    for (int i = 0; i < NUM_SERVICES_PER_APP; i++) {
      String name = getName(serviceNames);
      Map<String, Object> serviceMap = new HashMap<>();
      serviceMap.put("name", name);
      serviceMap.put("description", randomText(40));
      serviceMap.put("appId", appId);
      serviceMap.put("artifactType", WAR.name());
      serviceMap.put("appContainer", appContainers.get(randomInt(0, appContainers.size())));
      RestResponse<Service> response = target.request().post(
          Entity.entity(serviceMap, APPLICATION_JSON), new GenericType<RestResponse<Service>>() {});
      assertThat(response.getResource()).isInstanceOf(Service.class);
      services.add(response.getResource());
      configFileNames = new ArrayList<String>(seedNames);
      addConfigFilesToEntity(response.getResource(), DEFAULT_TEMPLATE_ID, NUM_CONFIG_FILE_PER_SERVICE);
    }
    return services;
  }

  private void addConfigFilesToEntity(Base entity, String templateId, int numConfigFiles) {
    String entityId = entity.getUuid();
    WebTarget target = client.target(
        String.format("http://localhost:9090/wings/configs/?entityId=%s&templateId=%s", entityId, templateId));
    try {
      for (int i = 0; i < numConfigFiles; i++) {
        File file = getTestFile(getName(configFileNames) + ".properties");
        FileDataBodyPart filePart = new FileDataBodyPart("file", file);
        FormDataMultiPart multiPart =
            new FormDataMultiPart().field("name", file.getName()).field("relativePath", "./configs/");
        multiPart.bodyPart(filePart);
        Response response = target.request().post(Entity.entity(multiPart, multiPart.getMediaType()));
        assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
      }
    } catch (IOException e) {
      log().info(e.getMessage());
    }
  }

  private List<AppContainer> addAppContainers(String appId) {
    WebTarget target = client.target("http://localhost:9090/wings/app-containers/?appId=" + appId);
    for (int i = 0; i < NUM_APP_CONTAINER_PER_APP; i++) {
      try {
        String version = String.format("%s.%s.%s", randomInt(10), randomInt(100), randomInt(1000));
        String name = containerNames.get(randomInt() % containerNames.size());
        File file = getTestFile("AppContainer" + randomInt());
        FileDataBodyPart filePart = new FileDataBodyPart("file", file);
        FormDataMultiPart multiPart = new FormDataMultiPart()
                                          .field("name", name)
                                          .field("version", version)
                                          .field("description", randomText(20))
                                          .field("sourceType", "FILE_UPLOAD")
                                          .field("standard", "false");
        multiPart.bodyPart(filePart);
        Response response = target.request().post(Entity.entity(multiPart, multiPart.getMediaType()));
        if (response.getStatus() != 200) {
          log().error("Duplicate app container. Retry");
          continue;
        }
      } catch (IOException e) {
        log().info(e.getMessage());
      }
    }
    RestResponse<PageResponse<AppContainer>> response =
        client.target("http://localhost:9090/wings/app-containers/?appId=" + appId)
            .request()
            .get(new GenericType<RestResponse<PageResponse<AppContainer>>>() {});
    return response.getResource().getResponse();
  }

  private File getTestFile(String name) throws IOException {
    File file = testFolder.newFile(name + getUuid());
    BufferedWriter out = new BufferedWriter(new FileWriter(file));
    out.write(randomText(100));
    out.close();
    return file;
  }

  private String getName(List<String> names) {
    int nameIdx = randomInt(0, names.size());
    String name = names.get(nameIdx);
    names.remove(nameIdx);
    return name;
  }

  private String randomText(int length) {
    int low = randomInt(50);
    int high = length + low > randomSeedString.length() ? randomSeedString.length() - low : length + low;
    return randomSeedString.substring(low, high);
  }

  private String randomSeedString = "Lorem Ipsum is simply dummy text of the printing and typesetting industry. "
      + "Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, "
      + "when an unknown printer took a galley of type and scrambled it to make a type specimen book. "
      + "It has survived not only five centuries, but also the leap into electronic typesetting, "
      + "remaining essentially unchanged. It was popularised in the 1960s with the release of "
      + "Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software "
      + "like Aldus PageMaker including versions of Lorem Ipsum";

  private List<String> envNames =
      Arrays.asList("DEV", "QA", "UAT", "PROD", "STRESS", "INTEGRATION", "SECURITY", "CLOUD", "PRIVATE", "INTERNAL");

  private List<String> containerNames = Arrays.asList("AOLserver", "Apache HTTP Server", "Apache Tomcat",
      "Barracuda Web Server", "Boa", "Caddy", "Caudium", "Cherokee HTTP Server", "GlassFish", "Hiawatha", "HFS",
      "IBM HTTP Server", "Internet Information Services", "Jetty", "Jexus", "lighttpd", "LiteSpeed Web Server",
      "Mongoose", "Monkey HTTP Server", "NaviServer", "NCSA HTTPd", "Nginx", "OpenLink Virtuoso", "OpenLiteSpeed",
      "Oracle HTTP Server", "Oracle iPlanet Web Server", "Oracle WebLogic Server", "Resin Open Source",
      "Resin Professional", "thttpd", "TUX web server", "Wakanda Server", "WEBrick", "Xitami", "Yaws",
      "Zeus Web Server", "Zope");

  private List<String> seedNames = Arrays.asList("Abaris", "Abundantia", "Acca Larentia", "Achelois", "Achelous",
      "Acheron", "Achilles", "Acidalia", "Acis", "Acmon", "Acoetes", "Actaeon", "Adamanthea", "Adephagia", "Adonis",
      "Adrastea", "Adrasteia", "Aeacos", "Aeacus", "Aegaeon", "Aegeus", "Aegina", "Aegle", "Aello", "Aellopos",
      "Aeneas", "Aeolos", "Aeolus", "Aequitas", "Aer", "Aerecura", "Aesacus", "Aesculapius", "Aesir", "Aeson",
      "Aeternitas", "Aethalides", "Aether", "Aethon", "Aetna", "Aeëtes", "Agamemnon", "Agave", "Agdistes", "Agdos",
      "Aglaea", "Aglaia", "Aglaulus", "Aglauros", "Aglaurus", "Agraulos", "Agrotara", "Agrotora", "Aiakos", "Aigle",
      "Aiolos", "Aion", "Air", "Aither", "Aius Locutius", "Ajax the Great", "Ajax the Lesser", "Alcemana", "Alcides",
      "Alcmena", "Alcmene", "Alcyone", "Alecto", "Alectrona", "Alernus or Elernus", "Alexandra", "Alkyone", "Aloadae",
      "Alpheos", "Alpheus", "Althaea", "Amalthea", "Amaltheia", "Amarynthia", "Ampelius", "Amphion", "Amphitrite",
      "Amphitryon", "Amymone", "Ananke", "Anaxarete", "Andhrimnir", "Andromeda", "Angerona", "Angitia", "Angrboda",
      "Anius", "Anna Perenna", "Annona", "Antaeus", "Antaios", "Anteros", "Antevorta", "Anticlea", "Antiklia",
      "Antiope", "Apate", "Aphrodite", "Apollo", "Apollon", "Aquilo", "Arachne", "Arcas", "Areon", "Ares", "Arethusa",
      "Argeos", "Argus", "Ariadne", "Arimanius", "Arion", "Aristaeus", "Aristaios", "Aristeas", "Arkas", "Arkeus Ultor",
      "Artemis", "Asclepius", "Asklepios", "Asopus", "Asteria", "Asterie", "Astraea", "Astraeus", "Astraios", "Astrild",
      "Atalanta", "Ate", "Athamas", "Athamus", "Athena", "Athene", "Athis", "Atla", "Atlantides", "Atlas", "Atropos",
      "Attis", "Attropus", "Audhumla", "Augean Stables", "Augian Stables", "Aura", "Aurai", "Aurora", "Autolycus",
      "Autolykos", "Auxesia", "Averruncus", "Bacchae", "Bacchantes", "Bacchus", "Balder", "Balios", "Balius", "Battus",
      "Baucis", "Bellerophon", "Bellona or Duellona", "Beyla", "Bia", "Bias", "Bona Dea", "Bonus Eventus", "Boreads",
      "Boreas", "Borghild", "Bragi", "Briareos", "Briareus", "Bromios", "Brono", "Bubona", "Byblis", "Bylgia", "Caca",
      "Cacus", "Cadmus", "Caelus", "Caeneus", "Caenis", "Calais", "Calchas", "Calliope", "Callisto", "Calypso",
      "Camenae", "Canens", "Cardea", "Carmenta", "Carmentes", "Carna", "Cassandra", "Castor", "Caunus", "Cecrops",
      "Celaeno", "Celoneo", "Ceneus", "Cephalus", "Cerberus", "Cercopes", "Ceres", "Cerigo", "Cerynean Hind",
      "Ceryneian Hind", "Cerynitis", "Ceto", "Ceyx", "Chaos", "Chariclo", "Charites", "Charon", "Charybdis", "Cheiron",
      "Chelone", "Chimaera", "Chimera", "Chione", "Chiron", "Chloe", "Chloris", "Chronos", "Chronus", "Chthonia",
      "Cinyras", "Cipus", "Circe", "Clementia", "Clio", "Cloacina", "Clotho", "Clymene", "Coeus", "Coltus", "Comus",
      "Concordia", "Consus", "Cornix", "Cottus", "Cotys", "Cotytto", "Cratus", "Cretan Bull", "Crius", "Cronos",
      "Cronus", "Cupid", "Cura", "Cyane", "Cybele", "Cyclopes", "Cyclops", "Cygnus", "Cyllarus", "Cynthia",
      "Cyparissus", "Cyrene", "Cytherea", "Cyáneë", "Daedalion", "Daedalus", "Dagur", "Danae", "Daphnaie", "Daphne",
      "Dea Dia", "Dea Tacita", "Decima", "Deimos", "Deimus", "Deino", "Delos", "Delphyne", "Demeter", "Demphredo",
      "Deo", "Despoena", "Deucalion", "Deukalion", "Devera or Deverra", "Deïanira", "Di inferi", "Diana",
      "Diana Nemorensis", "Dice", "Dike", "Diomedes", "Dione", "Dionysos", "Dionysus", "Dioscuri", "Dis", "Disciplina",
      "Discordia", "Disen", "Dithyrambos", "Dius Fidius", "Doris", "Dryades", "Dryads", "Dryope", "Echidna", "Echo",
      "Edesia", "Egeria", "Eileithyia", "Eir", "Eirene", "Ekhidna", "Ekho", "Electra", "Elektra", "Eleuthia", "Elli",
      "Elpis", "Empanda or Panda", "Empousa", "Empousai", "Empusa", "Enosichthon", "Enyalius", "Enyo", "Eos", "Epaphos",
      "Epaphus", "Ephialtes", "Epimeliades", "Epimeliads", "Epimelides", "Epimetheus", "Epiona", "Epione", "Epiphanes",
      "Epona", "Erato", "Erebos", "Erebus", "Erechtheus", "Erichtheus", "Erichthoneus", "Erichthonios", "Erichthonius",
      "Erinyes", "Erinys", "Eris", "Eros", "Erotes", "Erymanthean Boar", "Erymanthian Boar", "Erysichthon", "Erytheia",
      "Erytheis", "Erythia", "Ether", "Eumenides", "Eunomia", "Euphrosyne", "Europa", "Euros", "Eurus", "Euryale",
      "Eurybia", "Eurydice", "Eurynome", "Eurystheus", "Eurytus", "Euterpe", "Falacer", "Fama", "Fascinus", "Fates",
      "Fauna", "Faunus", "Faustitas", "Febris", "Februus", "Fecunditas", "Felicitas", "Fenrir", "Ferentina", "Feronia",
      "Fides", "Flora", "Fontus or Fons", "Fornax", "Forseti", "Fortuna", "Freya", "Freyr", "Frigg", "Fufluns",
      "Fulgora", "Furies", "Furrina", "Ga", "Gaea", "Gaia", "Gaiea", "Galanthis", "Galatea", "Galeotes", "Ganymede",
      "Ganymedes", "Ge", "Gefion", "Genius", "Gerd", "Geryon", "Geryones", "Geyron", "Glaucus", "Gorgons", "Graces",
      "Graeae", "Graiae", "Graii", "Gratiae", "Gyes", "Gyges", "Hades", "Haides", "Halcyone", "Hamadryades",
      "Hamadryads", "Harmonia", "Harmony", "Harpies", "Harpocrates", "Harpyia", "Harpyiai", "Hebe", "Hecate",
      "Hecatoncheires", "Hecatonchires", "Hecuba", "Heimdall", "Hekate", "Hekatonkheires", "Hel", "Helen", "Heliades",
      "Helice", "Helios", "Helius", "Hemera", "Hemere", "Hephaestus", "Hephaistos", "Hera", "Heracles", "Herakles",
      "Hercules", "Hermaphroditos", "Hermaphroditus", "Hermes", "Hermod", "Herse", "Hersilia", "Hespera",
      "Hesperethousa", "Hesperia", "Hesperides", "Hesperids", "Hesperie", "Hesperis", "Hesperos", "Hesperus", "Hestia",
      "Himeros", "Hippodame", "Hippolyta", "Hippolytos", "Hippolytta", "Hippolytus", "Hippomenes", "Hod", "Holler",
      "Honos", "Hope", "Hora", "Horae", "Horai", "Hyacinth", "Hyacinthus", "Hyades", "Hyakinthos", "Hydra", "Hydriades",
      "Hydriads", "Hygeia", "Hygieia", "Hylonome", "Hymen", "Hymenaeus", "Hymenaios", "Hyperion", "Hypnos", "Hypnus",
      "Hyppolyta", "Hyppolyte", "Iacchus", "Iambe", "Ianthe", "Iapetos", "Iapetus", "Icarus", "Icelos", "Idmon", "Idun",
      "Ikelos", "Ilia", "Ilithyia", "Ilythia", "Inachus", "Indiges", "Ino", "Intercidona", "Inuus", "Invidia", "Io",
      "Ion", "Iphicles", "Iphigenia", "Iphis", "Irene", "Iris", "Isis", "Itys", "Ixion", "Janus", "Jason", "Jord",
      "Jormungand", "Juno", "Jupiter", "Justitia", "Juturna", "Juventas", "Kadmos", "Kalais", "Kalliope", "Kallisto",
      "Kalypso", "Kari", "Kekrops", "Kelaino", "Kerberos", "Keres", "Kerkopes", "Keto", "Khaos", "Kharon", "Kharybdis",
      "Kheiron", "Khelone", "Khimaira", "Khione", "Khloris", "Khronos", "Kirke", "Kleio", "Klotho", "Klymene", "Koios",
      "Komos", "Kore", "Kottos", "Kratos", "Krios", "Kronos", "Kronus", "Kvasir", "Kybele", "Kyklopes", "Kyrene",
      "Lachesis", "Laertes", "Laga", "Lakhesis", "Lamia", "Lampetia", "Lampetie", "Laomedon", "Lares", "Latona",
      "Latreus", "Laverna", "Leda", "Leimoniades", "Leimoniads", "Lelantos", "Lelantus", "Lemures", "Lethaea", "Lethe",
      "Leto", "Letum", "Leucothea", "Levana", "Liber", "Libera", "Liberalitas", "Libertas", "Libitina", "Lichas",
      "Limoniades", "Limoniads", "Linus", "Lofn", "Loki", "Lua", "Lucifer", "Lucina", "Luna", "Lupercus", "Lycaon",
      "Lympha", "Macareus", "Maenads", "Magni", "Maia", "Maiandros", "Maliades", "Mana Genita", "Manes", "Mani",
      "Mania", "Mantus", "Mares of Diomedes", "Mars", "Mater Matuta", "Meandrus", "Medea", "Meditrina", "Medousa",
      "Medusa", "Mefitis or Mephitis", "Meleager", "Meliades", "Meliads", "Meliai", "Melidae", "Mellona or Mellonia",
      "Melpomene", "Memnon", "Mena or Mene", "Menoetius", "Menoitos", "Mercury", "Merope", "Metis", "Midas", "Miming",
      "Mimir", "Minerva", "Minos", "Minotaur", "Minotaurus", "Mithras", "Mnemosyne", "Modesty", "Modi", "Moirae",
      "Moirai", "Molae", "Momos", "Momus", "Moneta", "Mopsus", "Mormo", "Mormolykeia", "Moros", "Morpheus", "Mors",
      "Morta", "Morus", "Mount Olympus", "Mousai", "Murcia or Murtia", "Muses", "Mutunus Tutunus", "Myiagros", "Myrrha",
      "Myscelus", "Naenia", "Naiades", "Naiads", "Naias", "Narcissus", "Nascio", "Neaera", "Neaira", "Necessitas",
      "Nemean Lion", "Nemeian Lion", "Nemesis", "Nephelai", "Nephele", "Neptune", "Neptunus", "Nereides", "Nereids",
      "Nereus", "Nerio", "Nessus", "Nestor", "Neverita", "Nike", "Nikothoe", "Niobe", "Nix", "Nixi", "Njord", "Nomios",
      "Nona", "Norns", "Notos", "Nott", "Notus", "Nox", "Numa", "Nyctimene", "Nymphai", "Nymphs", "Nyx", "Oannes",
      "Obriareos", "Oceanides", "Oceanids", "Oceanus", "Ocypete", "Ocyrhoë", "Odin", "Odysseus", "Oeager", "Oeagrus",
      "Oenomaus", "Oinone", "Okeanides", "Okeanos", "Okypete", "Okypode", "Okythoe", "Olenus", "Olympus", "Omphale",
      "Ops", "Ops or Opis", "Orcus", "Oreades", "Oreads", "Oreiades", "Oreiads", "Oreithuia", "Oreithyia", "Orion",
      "Orithyea", "Orithyia", "Orpheus", "Orphus", "Orth", "Orthrus", "Ossa", "Otus", "Ourania", "Ouranos", "Paeon",
      "Paieon", "Paion", "Palatua", "Pales", "Pallas", "Pallas Athena", "Pan", "Panacea", "Panakeia", "Pandemos",
      "Pandora", "Paphos", "Parcae", "Paris", "Pasiphae", "Pasithea", "Pax", "Pegasos", "Pegasus", "Peleus", "Pelias",
      "Pelops", "Pemphredo", "Penia", "Penie", "Pentheus", "Perdix", "Persa", "Perse", "Perseis", "Persephassa",
      "Persephone", "Perses", "Perseus", "Persis", "Perso", "Petesuchos", "Phaedra", "Phaethousa", "Phaethusa",
      "Phaeton", "Phantasos", "Phaëton", "Phema", "Pheme", "Phemes", "Philammon", "Philemon", "Philomela", "Philomenus",
      "Philyra", "Philyre", "Phineus", "Phobetor", "Phobos", "Phobus", "Phocus", "Phoebe", "Phoibe", "Phorcys",
      "Phorkys", "Phospheros", "Picumnus", "Picus", "Pietas", "Pilumnus", "Pirithous", "Pleiades", "Pleione", "Pleone",
      "Ploutos", "Pluto", "Plutus", "Podarge", "Podarke", "Poena", "Pollux", "Polydectes", "Polydeuces", "Polydorus",
      "Polyhymnia", "Polymestor", "Polymnia", "Polyphemos", "Polyphemus", "Polyxena", "Pomona", "Pontos", "Pontus",
      "Poros", "Porrima", "Portunes", "Porus", "Poseidon", "Potamoi", "Priam", "Priapos", "Priapus", "Procne",
      "Procris", "Prometheus", "Proserpina", "Proteus", "Providentia", "Psyche", "Pudicitia", "Pygmalion", "Pyramus",
      "Pyreneus", "Pyrrha", "Pythagoras", "Python", "Querquetulanae", "Quirinus", "Quiritis", "Ran", "Rhadamanthus",
      "Rhadamanthys", "Rhamnousia", "Rhamnusia", "Rhea", "Rheia", "Robigo or Robigus", "Roma", "Romulus", "Rumina",
      "Sabazius", "Saga", "Salacia", "Salmoneus", "Salus", "Sancus", "Sarapis", "Sarpedon", "Saturn", "Saturnus",
      "Scamander", "Scylla", "Securitas", "Seilenos", "Seirenes", "Selene", "Semele", "Serapis", "Sibyl",
      "Sibyl of Cumae", "Sibyls", "Sif", "Silenos", "Silenus", "Silvanus", "Sirens", "Sisyphus", "Sito", "Sjofn",
      "Skadi", "Skamandros", "Skylla", "Sleipnir", "Sol", "Sol Invictus", "Somnus", "Soranus", "Sors", "Spercheios",
      "Spercheus", "Sperkheios", "Spes", "Sphinx", "Stata Mater", "Sterope", "Sterquilinus", "Stheno",
      "Stymphalian Birds", "Stymphalion Birds", "Styx", "Suadela", "Sulis Minerva", "Summanus", "Syn", "Syrinx",
      "Tantalus", "Tartaros", "Tartarus", "Taygete", "Telamon", "Telchines", "Telkhines", "Tellumo or Tellurus",
      "Tempestas", "Tereus", "Terminus", "Terpsichore", "Terpsikhore", "Tethys", "Thalassa", "Thaleia", "Thalia",
      "Thamrys", "Thanatos", "Thanatus", "Thanotos", "Thaumas", "Thea", "Thebe", "Theia", "Thelxinoe", "Themis",
      "Theseus", "Thetis", "Thetys", "Thisbe", "Thor", "Three Fates", "Tiberinus", "Tibertus", "Tiresias", "Tisiphone",
      "Titanes", "Titanides", "Titans", "Tithonus", "Tranquillitas", "Triptolemos", "Triptolemus", "Triton", "Tritones",
      "Trivia", "Tyche", "Tykhe", "Typhoeus", "Typhon", "Tyr", "Ubertas", "Ull", "Ulysses", "Unxia", "Urania", "Uranus",
      "Vacuna", "Vagitanus", "Vali", "Valkyries", "Vanir", "Var", "Vediovus or Veiovis", "Venilia or Venelia", "Venti",
      "Venus", "Veritas", "Verminus", "Vertumnus", "Vesta", "Vica Pota", "Victoria", "Vidar", "Viduus", "Virbius",
      "Virtus", "Volturnus", "Voluptas", "Vulcan", "Vulcanus", "Xanthos", "Xanthus", "Zelos", "Zelus", "Zephyros",
      "Zephyrs", "Zephyrus", "Zetes", "Zethes", "Zethus", "Zeus");
  private List<String> appNames = new ArrayList<String>(seedNames);
  private List<String> serviceNames;
  private List<String> configFileNames;
}
