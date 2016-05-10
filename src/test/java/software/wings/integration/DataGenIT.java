package software.wings.integration;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static java.lang.Integer.MAX_VALUE;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.ArtifactSource.ArtifactType.WAR;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;

import com.google.inject.Inject;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.assertj.core.api.Assertions;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.glassfish.jersey.media.multipart.internal.MultiPartWriter;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import software.wings.WingsBaseIntegrationTest;
import software.wings.beans.AppContainer;
import software.wings.beans.Application;
import software.wings.beans.Base;
import software.wings.beans.PageResponse;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.dl.WingsPersistence;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

/**
 * Created by anubhaw on 5/6/16.
 */
@Path("/dataGen")
public class DataGenIT extends WingsBaseIntegrationTest {
  private static final int NUM_APPS = 100;
  private static final int NUM_APP_CONTAINER_PER_APP = 100;
  private static final int NUM_SERVICES_PER_APP = 10;
  private static final int NUM_CONFIG_FILE_PER_SERVICE = 20;
  private static final int NUM_ENV_PER_APP = 4;
  private static final int NUM_HOSTS_PER_INFRA = 100;
  private Client client;
  private Random random = new Random();

  @Rule public TemporaryFolder testFolder = new TemporaryFolder();

  @Inject private WingsPersistence wingsPersistence;

  /**
   * Generated Data for across the API use
   */

  @Before
  public void setUp() throws Exception {
    wingsPersistence.getDatastore().getDB().dropDatabase();

    ClientConfig config = new ClientConfig(new JacksonJaxbJsonProvider().configure(FAIL_ON_UNKNOWN_PROPERTIES, false));
    config.register(MultiPartWriter.class);
    client = ClientBuilder.newClient(config);
  }

  @Test
  public void populateDataIT() throws IOException {
    List<Application> apps = createApplications();
    Map<String, List<AppContainer>> containers = new HashMap<>();
    Map<String, List<Service>> services = new HashMap<>();

    apps.forEach(application -> {
      // add resources
      List<AppContainer> appContainers = addAppContainers(application.getUuid());
      containers.put(application.getUuid(), appContainers);
      services.put(application.getUuid(), addServices(application.getUuid(), appContainers));
    });
  }

  private List<Application> createApplications() {
    List<Application> apps = new ArrayList<>();

    WebTarget target = client.target("http://localhost:9090/wings/apps/");

    for (int i = 0; i < NUM_APPS; i++) {
      String name = getName(appNames);
      RestResponse<Application> response = target.request().post(
          Entity.entity(anApplication().withName(name).withDescription(name).build(), APPLICATION_JSON),
          new GenericType<RestResponse<Application>>() {});
      Assertions.assertThat(response.getResource()).isInstanceOf(Application.class);
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
      Assertions.assertThat(response.getResource()).isInstanceOf(Service.class);
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
        Assertions.assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
      }
    } catch (IOException e) {
      log().info(e.getMessage());
    }
  }

  private List<AppContainer> addAppContainers(String appId) {
    List<String> containerNames = Arrays.asList("AOLserver", "Apache HTTP Server", "Apache Tomcat",
        "Barracuda Web Server", "Boa", "Caddy", "Caudium", "Cherokee HTTP Server", "GlassFish", "Hiawatha", "HFS",
        "IBM HTTP Server", "Internet Information Services", "Jetty", "Jexus", "lighttpd", "LiteSpeed Web Server",
        "Mongoose", "Monkey HTTP Server", "NaviServer", "NCSA HTTPd", "Nginx", "OpenLink Virtuoso", "OpenLiteSpeed",
        "Oracle HTTP Server", "Oracle iPlanet Web Server", "Oracle WebLogic Server", "Resin Open Source",
        "Resin Professional", "thttpd", "TUX web server", "Wakanda Server", "WEBrick", "Xitami", "Yaws",
        "Zeus Web Server", "Zope");

    List<AppContainer> containers = new ArrayList<>();
    WebTarget target = client.target("http://localhost:9090/wings/app-containers/?appId=" + appId);
    for (int i = 0; i < NUM_APP_CONTAINER_PER_APP; i++) {
      try {
        String version = (randomInt(1, 10)) + "." + randomInt(100);
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
        Assertions.assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
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
    File file = testFolder.newFile(name + randomInt());
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

  private int randomInt(int low, int high) {
    return random.nextInt(high - low) + low;
  }

  private int randomInt(int high) {
    return randomInt(0, high);
  }

  private int randomInt() {
    return randomInt(0, MAX_VALUE);
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

  private List<String> seedNames = Arrays.asList("Achelois", "Achelous", "Acheron", "Achilles", "Acidalia",
      "Adamanthea", "Adephagia", "Adonis", "Adrastea", "Adrasteia", "Aeacos", "Aeacus", "Aegaeon", "Aegina", "Aegle",
      "Aello", "Aellopos", "Aeolos", "Aeolus", "Aer", "Aesculapius", "Aethalides", "Aether", "Aethon", "Aetna", "Agave",
      "Agdistes", "Agdos", "Aglaea", "Aglaia", "Aglauros", "Aglaurus", "Agraulos", "Agrotara", "Agrotora", "Aiakos",
      "Aigle", "Aiolos", "Air", "Aither", "Alcemana", "Alcides", "Alcmena", "Alcmene", "Alcyone", "Alcyone(2)",
      "Alecto", "Alectrona", "Alexandra", "Alkyone", "Aloadae", "Alpheos", "Alpheus", "Amalthea", "Amaltheia",
      "Amarynthia", "Ampelius", "Amphion", "Amphitrite", "Amphitryon", "Amymone", "Ananke", "Andromeda", "Antaeus",
      "Antaios", "Anteros", "Anticlea", "Antiklia", "Antiope", "Apate", "Aphrodite", "Apollo", "Apollon", "Arachne",
      "Arcas", "Areon", "Ares", "Arethusa", "Argeos", "Argus", "Ariadne", "Arion", "Arion(2)", "Aristaeus", "Aristaios",
      "Aristeas", "Arkas", "Artemis", "Asclepius", "Asklepios", "Asopus", "Asteria", "Asterie", "Astraea", "Astraeus",
      "Astraios", "Atalanta", "Ate", "Athamas", "Athamus", "Athena", "Athene", "Atlantides", "Atlas", "Atropos",
      "Attis", "Attropus", "Augean Stables", "Augian Stables", "Aurai", "Autolycus", "Autolykos", "Auxesia", "Bacchae",
      "Bacchantes", "Balios", "Balius", "Bellerophon", "Bia", "Bias", "Boreads", "Boreas", "Briareos", "Briareus",
      "Bromios", "Cadmus", "Caeneus", "Caenis", "Calais", "Calchas", "Calliope", "Callisto", "Calypso", "Cassandra",
      "Castor", "Cecrops", "Celaeno", "Celaeno(2)", "Celoneo", "Ceneus", "Cerberus", "Cercopes", "Cerigo",
      "Cerynean Hind", "Ceryneian Hind", "Cerynitis", "Ceto", "Chaos", "Charites", "Charon", "Charybdis", "Cheiron",
      "Chelone", "Chimaera", "Chimera", "Chione", "Chiron", "Chloe", "Chloris", "Chronos", "Chronus", "Chthonia",
      "Circe", "Clio", "Clotho", "Clymene", "Coeus", "Coltus", "Comus", "Cottus", "Cotys", "Cotytto", "Cratus",
      "Cretan Bull", "Crius", "Cronos", "Cronus", "Cybele", "Cyclopes", "Cyclops", "Cynthia", "Cyrene", "Cytherea",
      "Danae", "Daphnaie", "Deimos", "Deimus", "Deino", "Delos", "Delphyne", "Demeter", "Demphredo", "Deo", "Despoena",
      "Deucalion", "Deukalion", "Dice", "Dike", "Dione", "Dionysos", "Dionysus", "Dioscuri", "Dithyrambos", "Doris",
      "Dryades", "Dryads", "Echidna", "Echo", "Eileithyia", "Eirene", "Ekhidna", "Ekho", "Electra", "Electra(2)",
      "Electra(3)", "Elektra", "Eleuthia", "Elpis", "Empousa", "Empousai", "Empusa", "Enosichthon", "Enyalius", "Enyo",
      "Eos", "Epaphos", "Epaphus", "Ephialtes", "Epimeliades", "Epimeliads", "Epimelides", "Epimetheus", "Epiona",
      "Epione", "Epiphanes", "Erato", "Erebos", "Erebus", "Erechtheus", "Erichtheus", "Erichthoneus", "Erichthonios",
      "Erichthonius", "Erinyes", "Erinys", "Eris", "Eros", "Erotes", "Erymanthean Boar", "Erymanthian Boar", "Erytheia",
      "Erytheis", "Erythia", "Ether", "Eumenides", "Eunomia", "Euphrosyne(2)", "Europa", "Euros", "Eurus", "Euryale",
      "Eurybia", "Eurydice", "Eurynome", "Eurystheus", "Euterpe", "Fates", "Furies", "Ga", "Gaea", "Gaia", "Gaiea",
      "Galeotes", "Ganymede", "Ganymedes", "Ge", "Geryon", "Geryones", "Geyron", "Glaucus", "Gorgons", "Graces",
      "Graeae", "Graiae", "Graii", "Gratiae", "Gyes", "Gyges", "Hades", "Haides", "Halcyone", "Hamadryades",
      "Hamadryads", "Harmonia", "Harmony", "Harpies", "Harpocrates", "Harpyia", "Harpyiai", "Hebe", "Hecate",
      "Hecatoncheires", "Hecatonchires", "Hekate", "Hekatonkheires", "Helen", "Heliades", "Helice", "Helios", "Helius",
      "Hemera", "Hemere", "Hephaestus", "Hephaistos", "Hera", "Heracles", "Herakles", "Hermaphroditos",
      "Hermaphroditus", "Hermes", "Hespera", "Hesperethousa", "Hesperia", "Hesperides", "Hesperids", "Hesperie",
      "Hesperis", "Hesperos", "Hesperus", "Hestia", "Himeros", "Hippolyta", "Hippolytos", "Hippolytta", "Hippolytus",
      "Hope", "Horae", "Horai", "Hyacinth", "Hyacinthus", "Hyades", "Hyakinthos", "Hydra", "Hydriades", "Hydriads",
      "Hygeia", "Hygieia", "Hymen", "Hymenaeus", "Hymenaios", "Hyperion", "Hypnos", "Hypnus", "Hyppolyta", "Hyppolyte",
      "Iacchus", "Iambe", "Iapetos", "Iapetus", "Icelos", "Ikelos", "Ilithyia", "Ilythia", "Inachus", "Ino", "Io",
      "Ion", "Iphicles", "Irene", "Iris", "Kadmos", "Kalais", "Kalliope", "Kallisto", "Kalypso", "Kekrops", "Kelaino",
      "Kerberos", "Keres", "Kerkopes", "Keto", "Khaos", "Kharon", "Kharybdis", "Kheiron", "Khelone", "Khimaira",
      "Khione", "Khloris", "Khronos", "Kirke", "Kleio", "Klotho", "Klymene", "Koios", "Komos", "Kore", "Kottos",
      "Kratos", "Krios", "Kronos", "Kronus", "Kybele", "Kyklopes", "Kyrene", "Lachesis", "Laertes", "Lakhesis", "Lamia",
      "Lampetia", "Lampetie", "Leda", "Leimoniades", "Leimoniads", "Lelantos", "Lelantus", "Lethe", "Leto",
      "Limoniades", "Limoniads", "Linus", "Maenads", "Maia", "Maiandros", "Maliades", "Mares of Diomedes", "Meandrus",
      "Medea", "Medousa", "Medusa", "Meliades", "Meliads", "Meliai", "Melidae", "Melpomene", "Memnon", "Menoetius",
      "Menoitos", "Merope", "Metis", "Minos", "Minotaur", "Mnemosyne", "Modesty", "Moirae", "Moirai", "Momos", "Momus",
      "Mopsus", "Mormo", "Mormolykeia", "Moros", "Morpheus", "Morus", "Mount Olympus", "Mousai", "Muses", "Myiagros",
      "Naiades", "Naiads", "Naias", "Neaera", "Neaira", "Nemean Lion", "Nemeian Lion", "Nemesis", "Nephelai", "Nephele",
      "Nereides", "Nereids", "Nereus", "Nike", "Nikothoe", "Niobe", "Nix", "Nomios", "Nona", "Notos", "Notus", "Nox",
      "Nymphai", "Nymphs", "Nyx", "Oannes", "Obriareos", "Oceanides", "Oceanids", "Oceanus", "Ocypete", "Odysseus",
      "Oeager", "Oeagrus", "Oenomaus", "Oinone", "Okeanides", "Okeanos", "Okypete", "Okypode", "Okythoe", "Olympus",
      "Omphale", "Oreades", "Oreads", "Oreiades", "Oreiads", "Oreithuia", "Oreithyia", "Orion", "Orithyea", "Orithyia",
      "Orpheus", "Orphus", "Orth", "Orthrus", "Ossa", "Otus", "Ourania", "Ouranos", "Paeon", "Paieon", "Paion",
      "Pallas", "Pallas(2)", "Pallas(3)", "Pallas(5)", "Pallas Athena", "Pan", "Panacea", "Panakeia", "Pandemos",
      "Pandora", "Pasiphae", "Pasithea", "Pegasos", "Pegasus", "Pelops", "Pemphredo", "Penia", "Penie", "Persa",
      "Perse", "Perseis", "Persephassa", "Persephone", "Perses", "Perseus", "Persis", "Perso", "Petesuchos",
      "Phaethousa", "Phaethusa", "Phaeton", "Phantasos", "Phema", "Pheme", "Phemes", "Philammon", "Philomenus",
      "Philyra", "Philyre", "Phobetor", "Phobos", "Phobus", "Phoebe", "Phoebe(2)", "Phoibe", "Phorcys", "Phorkys",
      "Phospheros", "Pleiades", "Pleione", "Pleone", "Ploutos", "Plutus", "Podarge", "Podarke", "Pollux", "Polydeuces",
      "Polyhymnia", "Polymnia", "Polyphemos", "Polyphemus", "Pontos", "Pontus", "Poros", "Porus", "Poseidon", "Potamoi",
      "Priapos", "Priapus", "Prometheus", "Proteus", "Psyche", "Pyrrha", "Python", "Rhadamanthus", "Rhadamanthys",
      "Rhamnousia", "Rhamnusia", "Rhea", "Rheia", "Sabazius", "Salmoneus", "Sarapis", "Sarpedon", "Scamander", "Scylla",
      "Seilenos", "Seirenes", "Selene", "Semele", "Serapis", "Sibyl of Cumae", "Sibyls", "Silenos", "Silenus", "Sirens",
      "Sisyphus", "Sito", "Skamandros", "Skylla", "Spercheios", "Spercheus", "Sperkheios", "Sphinx(2)", "Sterope",
      "Stheno", "Stymphalian Birds", "Stymphalion Birds", "Styx", "Syrinx", "Tantalus", "Tartaros", "Tartarus",
      "Taygete", "Telchines", "Telkhines", "Terpsichore", "Terpsikhore", "Tethys", "Thalassa", "Thaleia", "Thalia",
      "Thamrys", "Thanatos", "Thanatus", "Thanotos", "Thaumas", "Thea", "Thebe", "Theia", "Thelxinoe", "Themis",
      "Theseus", "Thetis", "Thetys", "Three Fates", "Titanes", "Titanides", "Titans", "Tithonus", "Triptolemos",
      "Triptolemus", "Triton", "Tritones", "Tyche", "Tykhe", "Typhoeus", "Typhon", "Ulysses", "Urania", "Uranus",
      "Xanthos", "Xanthus", "Zelos", "Zelus", "Zephyros", "Zephyrs", "Zephyrus", "Zetes", "Zethes", "Zethus", "Zeus");
  private List<String> appNames = new ArrayList<String>(seedNames);
  private List<String> serviceNames;
  private List<String> configFileNames;
}
