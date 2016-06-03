package software.wings.integration;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.shuffle;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.Response.Status.OK;
import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Activity.Builder.anActivity;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Artifact.Builder.anArtifact;
import static software.wings.beans.ArtifactSource.ArtifactType.WAR;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.beans.Environment.EnvironmentBuilder.anEnvironment;
import static software.wings.beans.JenkinsConfig.Builder.aJenkinsConfig;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.Release.ReleaseBuilder.aRelease;
import static software.wings.beans.ServiceInstance.ServiceInstanceBuilder.aServiceInstance;
import static software.wings.beans.ServiceTemplate.ServiceTemplateBuilder.aServiceTemplate;
import static software.wings.beans.SettingAttribute.SettingAttributeBuilder.aSettingAttribute;
import static software.wings.beans.SettingValue.SettingVariableTypes.HOST_CONNECTION_ATTRIBUTES;
import static software.wings.helpers.ext.mail.SmtpConfig.Builder.aSmtpConfig;
import static software.wings.integration.IntegrationTestUtil.randomInt;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
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
import software.wings.beans.Activity;
import software.wings.beans.Activity.Status;
import software.wings.beans.AppContainer;
import software.wings.beans.Application;
import software.wings.beans.Artifact;
import software.wings.beans.Base;
import software.wings.beans.BastionConnectionAttributes;
import software.wings.beans.Environment;
import software.wings.beans.Host;
import software.wings.beans.Infra;
import software.wings.beans.Release;
import software.wings.beans.RestResponse;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.SettingAttribute;
import software.wings.beans.Tag;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.rules.Integration;
import software.wings.utils.JsonUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
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
public class DataGenUtil extends WingsBaseTest {
  private static final int NUM_APPS = 1; /* Max 1000 */
  private static final int NUM_APP_CONTAINER_PER_APP = 1; /* Max 1000 */
  private static final int NUM_SERVICES_PER_APP = 2; /* Max 1000 */
  private static final int NUM_CONFIG_FILE_PER_SERVICE = 2; /* Max 100  */
  private static final int NUM_ENV_PER_APP = 2; /* Max 10   */
  private static final int NUM_HOSTS_PER_INFRA = 10; /* No limit */
  private static final int NUM_TAG_GROUPS_PER_ENV = 3; /* Max 10   */
  private static final int TAG_HIERARCHY_DEPTH = 3; /* Max 10   */
  @Rule public TemporaryFolder testFolder = new TemporaryFolder();
  private Client client;
  @Inject private WingsPersistence wingsPersistence;
  private String randomSeedString = "Lorem Ipsum is simply dummy text of the printing and typesetting industry. "
      + "Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, "
      + "when an unknown printer took a galley of type and scrambled it to make a type specimen book. "
      + "It has survived not only five centuries, but also the leap into electronic typesetting, "
      + "remaining essentially unchanged. It was popularised in the 1960s with the release of "
      + "Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software "
      + "like Aldus PageMaker including versions of Lorem Ipsum";
  private List<String> envNames =
      asList("DEV", "QA", "UAT", "PROD", "STRESS", "INTEGRATION", "SECURITY", "CLOUD", "PRIVATE", "INTERNAL");
  private List<String> containerNames =
      asList("AOLserver", "Apache HTTP Server", "Apache Tomcat", "Barracuda Web Server", "Boa", "Caddy", "Caudium",
          "Cherokee HTTP Server", "GlassFish", "Hiawatha", "HFS", "IBM HTTP Server", "Internet Information Services",
          "Jetty", "Jexus", "lighttpd", "LiteSpeed Web Server", "Mongoose", "Monkey HTTP Server", "NaviServer",
          "NCSA HTTPd", "Nginx", "OpenLink Virtuoso", "OpenLiteSpeed", "Oracle HTTP Server",
          "Oracle iPlanet Web Server", "Oracle WebLogic Server", "Resin Open Source", "Resin Professional", "thttpd",
          "TUX web server", "Wakanda Server", "WEBrick", "Xitami", "Yaws", "Zeus Web Server", "Zope");
  private List<String> seedNames = asList("Abaris", "Abundantia", "Acca Larentia", "Achelois", "Achelous", "Acheron",
      "Achilles", "Acidalia", "Acis", "Acmon", "Acoetes", "Actaeon", "Adamanthea", "Adephagia", "Adonis", "Adrastea",
      "Adrasteia", "Aeacos", "Aeacus", "Aegaeon", "Aegeus", "Aegina", "Aegle", "Aello", "Aellopos", "Aeneas", "Aeolos",
      "Aeolus", "Aequitas", "Aer", "Aerecura", "Aesacus", "Aesculapius", "Aesir", "Aeson", "Aeternitas", "Aethalides",
      "Aether", "Aethon", "Aetna", "Aeëtes", "Agamemnon", "Agave", "Agdistes", "Agdos", "Aglaea", "Aglaia", "Aglaulus",
      "Aglauros", "Aglaurus", "Agraulos", "Agrotara", "Agrotora", "Aiakos", "Aigle", "Aiolos", "Aion", "Air", "Aither",
      "Aius Locutius", "Ajax the Great", "Ajax the Lesser", "Alcemana", "Alcides", "Alcmena", "Alcmene", "Alcyone",
      "Alecto", "Alectrona", "Alernus or Elernus", "Alexandra", "Alkyone", "Aloadae", "Alpheos", "Alpheus", "Althaea",
      "Amalthea", "Amaltheia", "Amarynthia", "Ampelius", "Amphion", "Amphitrite", "Amphitryon", "Amymone", "Ananke",
      "Anaxarete", "Andhrimnir", "Andromeda", "Angerona", "Angitia", "Angrboda", "Anius", "Anna Perenna", "Annona",
      "Antaeus", "Antaios", "Anteros", "Antevorta", "Anticlea", "Antiklia", "Antiope", "Apate", "Aphrodite", "Apollo",
      "Apollon", "Aquilo", "Arachne", "Arcas", "Areon", "Ares", "Arethusa", "Argeos", "Argus", "Ariadne", "Arimanius",
      "Arion", "Aristaeus", "Aristaios", "Aristeas", "Arkas", "Arkeus Ultor", "Artemis", "Asclepius", "Asklepios",
      "Asopus", "Asteria", "Asterie", "Astraea", "Astraeus", "Astraios", "Astrild", "Atalanta", "Ate", "Athamas",
      "Athamus", "Athena", "Athene", "Athis", "Atla", "Atlantides", "Atlas", "Atropos", "Attis", "Attropus", "Audhumla",
      "Augean Stables", "Augian Stables", "Aura", "Aurai", "Aurora", "Autolycus", "Autolykos", "Auxesia", "Averruncus",
      "Bacchae", "Bacchantes", "Bacchus", "Balder", "Balios", "Balius", "Battus", "Baucis", "Bellerophon",
      "Bellona or Duellona", "Beyla", "Bia", "Bias", "Bona Dea", "Bonus Eventus", "Boreads", "Boreas", "Borghild",
      "Bragi", "Briareos", "Briareus", "Bromios", "Brono", "Bubona", "Byblis", "Bylgia", "Caca", "Cacus", "Cadmus",
      "Caelus", "Caeneus", "Caenis", "Calais", "Calchas", "Calliope", "Callisto", "Calypso", "Camenae", "Canens",
      "Cardea", "Carmenta", "Carmentes", "Carna", "Cassandra", "Castor", "Caunus", "Cecrops", "Celaeno", "Celoneo",
      "Ceneus", "Cephalus", "Cerberus", "Cercopes", "Ceres", "Cerigo", "Cerynean Hind", "Ceryneian Hind", "Cerynitis",
      "Ceto", "Ceyx", "Chaos", "Chariclo", "Charites", "Charon", "Charybdis", "Cheiron", "Chelone", "Chimaera",
      "Chimera", "Chione", "Chiron", "Chloe", "Chloris", "Chronos", "Chronus", "Chthonia", "Cinyras", "Cipus", "Circe",
      "Clementia", "Clio", "Cloacina", "Clotho", "Clymene", "Coeus", "Coltus", "Comus", "Concordia", "Consus", "Cornix",
      "Cottus", "Cotys", "Cotytto", "Cratus", "Cretan Bull", "Crius", "Cronos", "Cronus", "Cupid", "Cura", "Cyane",
      "Cybele", "Cyclopes", "Cyclops", "Cygnus", "Cyllarus", "Cynthia", "Cyparissus", "Cyrene", "Cytherea", "Cyáneë",
      "Daedalion", "Daedalus", "Dagur", "Danae", "Daphnaie", "Daphne", "Dea Dia", "Dea Tacita", "Decima", "Deimos",
      "Deimus", "Deino", "Delos", "Delphyne", "Demeter", "Demphredo", "Deo", "Despoena", "Deucalion", "Deukalion",
      "Devera or Deverra", "Deïanira", "Di inferi", "Diana", "Diana Nemorensis", "Dice", "Dike", "Diomedes", "Dione",
      "Dionysos", "Dionysus", "Dioscuri", "Dis", "Disciplina", "Discordia", "Disen", "Dithyrambos", "Dius Fidius",
      "Doris", "Dryades", "Dryads", "Dryope", "Echidna", "Echo", "Edesia", "Egeria", "Eileithyia", "Eir", "Eirene",
      "Ekhidna", "Ekho", "Electra", "Elektra", "Eleuthia", "Elli", "Elpis", "Empanda or Panda", "Empousa", "Empousai",
      "Empusa", "Enosichthon", "Enyalius", "Enyo", "Eos", "Epaphos", "Epaphus", "Ephialtes", "Epimeliades",
      "Epimeliads", "Epimelides", "Epimetheus", "Epiona", "Epione", "Epiphanes", "Epona", "Erato", "Erebos", "Erebus",
      "Erechtheus", "Erichtheus", "Erichthoneus", "Erichthonios", "Erichthonius", "Erinyes", "Erinys", "Eris", "Eros",
      "Erotes", "Erymanthean Boar", "Erymanthian Boar", "Erysichthon", "Erytheia", "Erytheis", "Erythia", "Ether",
      "Eumenides", "Eunomia", "Euphrosyne", "Europa", "Euros", "Eurus", "Euryale", "Eurybia", "Eurydice", "Eurynome",
      "Eurystheus", "Eurytus", "Euterpe", "Falacer", "Fama", "Fascinus", "Fates", "Fauna", "Faunus", "Faustitas",
      "Febris", "Februus", "Fecunditas", "Felicitas", "Fenrir", "Ferentina", "Feronia", "Fides", "Flora",
      "Fontus or Fons", "Fornax", "Forseti", "Fortuna", "Freya", "Freyr", "Frigg", "Fufluns", "Fulgora", "Furies",
      "Furrina", "Ga", "Gaea", "Gaia", "Gaiea", "Galanthis", "Galatea", "Galeotes", "Ganymede", "Ganymedes", "Ge",
      "Gefion", "Genius", "Gerd", "Geryon", "Geryones", "Geyron", "Glaucus", "Gorgons", "Graces", "Graeae", "Graiae",
      "Graii", "Gratiae", "Gyes", "Gyges", "Hades", "Haides", "Halcyone", "Hamadryades", "Hamadryads", "Harmonia",
      "Harmony", "Harpies", "Harpocrates", "Harpyia", "Harpyiai", "Hebe", "Hecate", "Hecatoncheires", "Hecatonchires",
      "Hecuba", "Heimdall", "Hekate", "Hekatonkheires", "Hel", "Helen", "Heliades", "Helice", "Helios", "Helius",
      "Hemera", "Hemere", "Hephaestus", "Hephaistos", "Hera", "Heracles", "Herakles", "Hercules", "Hermaphroditos",
      "Hermaphroditus", "Hermes", "Hermod", "Herse", "Hersilia", "Hespera", "Hesperethousa", "Hesperia", "Hesperides",
      "Hesperids", "Hesperie", "Hesperis", "Hesperos", "Hesperus", "Hestia", "Himeros", "Hippodame", "Hippolyta",
      "Hippolytos", "Hippolytta", "Hippolytus", "Hippomenes", "Hod", "Holler", "Honos", "Hope", "Hora", "Horae",
      "Horai", "Hyacinth", "Hyacinthus", "Hyades", "Hyakinthos", "Hydra", "Hydriades", "Hydriads", "Hygeia", "Hygieia",
      "Hylonome", "Hymen", "Hymenaeus", "Hymenaios", "Hyperion", "Hypnos", "Hypnus", "Hyppolyta", "Hyppolyte",
      "Iacchus", "Iambe", "Ianthe", "Iapetos", "Iapetus", "Icarus", "Icelos", "Idmon", "Idun", "Ikelos", "Ilia",
      "Ilithyia", "Ilythia", "Inachus", "Indiges", "Ino", "Intercidona", "Inuus", "Invidia", "Io", "Ion", "Iphicles",
      "Iphigenia", "Iphis", "Irene", "Iris", "Isis", "Itys", "Ixion", "Janus", "Jason", "Jord", "Jormungand", "Juno",
      "Jupiter", "Justitia", "Juturna", "Juventas", "Kadmos", "Kalais", "Kalliope", "Kallisto", "Kalypso", "Kari",
      "Kekrops", "Kelaino", "Kerberos", "Keres", "Kerkopes", "Keto", "Khaos", "Kharon", "Kharybdis", "Kheiron",
      "Khelone", "Khimaira", "Khione", "Khloris", "Khronos", "Kirke", "Kleio", "Klotho", "Klymene", "Koios", "Komos",
      "Kore", "Kottos", "Kratos", "Krios", "Kronos", "Kronus", "Kvasir", "Kybele", "Kyklopes", "Kyrene", "Lachesis",
      "Laertes", "Laga", "Lakhesis", "Lamia", "Lampetia", "Lampetie", "Laomedon", "Lares", "Latona", "Latreus",
      "Laverna", "Leda", "Leimoniades", "Leimoniads", "Lelantos", "Lelantus", "Lemures", "Lethaea", "Lethe", "Leto",
      "Letum", "Leucothea", "Levana", "Liber", "Libera", "Liberalitas", "Libertas", "Libitina", "Lichas", "Limoniades",
      "Limoniads", "Linus", "Lofn", "Loki", "Lua", "Lucifer", "Lucina", "Luna", "Lupercus", "Lycaon", "Lympha",
      "Macareus", "Maenads", "Magni", "Maia", "Maiandros", "Maliades", "Mana Genita", "Manes", "Mani", "Mania",
      "Mantus", "Mares of Diomedes", "Mars", "Mater Matuta", "Meandrus", "Medea", "Meditrina", "Medousa", "Medusa",
      "Mefitis or Mephitis", "Meleager", "Meliades", "Meliads", "Meliai", "Melidae", "Mellona or Mellonia", "Melpomene",
      "Memnon", "Mena or Mene", "Menoetius", "Menoitos", "Mercury", "Merope", "Metis", "Midas", "Miming", "Mimir",
      "Minerva", "Minos", "Minotaur", "Minotaurus", "Mithras", "Mnemosyne", "Modesty", "Modi", "Moirae", "Moirai",
      "Molae", "Momos", "Momus", "Moneta", "Mopsus", "Mormo", "Mormolykeia", "Moros", "Morpheus", "Mors", "Morta",
      "Morus", "Mount Olympus", "Mousai", "Murcia or Murtia", "Muses", "Mutunus Tutunus", "Myiagros", "Myrrha",
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
  private SettingAttribute envAttr = null;

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
    assertThat(NUM_TAG_GROUPS_PER_ENV).isBetween(1, 10);
    assertThat(TAG_HIERARCHY_DEPTH).isBetween(1, 10);

    dropDBAndEnsureIndexes();

    ClientConfig config = new ClientConfig(new JacksonJsonProvider().configure(FAIL_ON_UNKNOWN_PROPERTIES, false));
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
  public void populateData() throws IOException {
    createGlobalSettings();

    List<Application> apps = createApplications();
    Map<String, List<AppContainer>> containers = new HashMap<>();
    Map<String, List<Service>> services = new HashMap<>();
    Map<String, List<Environment>> appEnvs = new HashMap<>();

    for (Application application : apps) {
      containers.put(application.getUuid(), addAppContainers(application.getUuid()));
      services.put(application.getUuid(), addServices(application.getUuid(), containers.get(application.getUuid())));
      appEnvs.put(application.getUuid(), addEnvs(application.getUuid()));
      addServiceInstances(application, services.get(application.getUuid()), appEnvs.get(application.getUuid()));
      addActivitiesAndLogs(application, services.get(application.getUuid()), appEnvs.get(application.getUuid()));
    }
  }

  private void addServiceInstances(Application application, List<Service> services, List<Environment> appEnvs) {
    // TODO: improve make http calls and use better generation scheme
    services.forEach(service -> {
      appEnvs.forEach(environment -> {
        String infraId =
            wingsPersistence.createQuery(Infra.class).field("envId").equal(environment.getUuid()).get().getUuid();
        List<Host> hosts = wingsPersistence.createQuery(Host.class)
                               .field("appId")
                               .equal(environment.getAppId())
                               .field("infraId")
                               .equal(infraId)
                               .asList();
        ServiceTemplate template =
            wingsPersistence.saveAndGet(ServiceTemplate.class, aServiceTemplate().withName("catalog:8080").build());
        Release release = wingsPersistence.saveAndGet(Release.class, aRelease().withReleaseName("Rel1.1").build());
        Artifact artifact =
            wingsPersistence.saveAndGet(Artifact.class, anArtifact().withDisplayName("Build_02_16_10AM").build());

        hosts.forEach(host
            -> wingsPersistence.save(aServiceInstance()
                                         .withAppId(host.getAppId())
                                         .withEnvId(environment.getUuid())
                                         .withHost(host)
                                         .withService(service)
                                         .withServiceTemplate(template)
                                         .withRelease(release)
                                         .withArtifact(artifact)
                                         .build()));
      });
    });
  }

  private void addActivitiesAndLogs(Application application, List<Service> services, List<Environment> appEnvs) {
    // TODO: improve make http calls and use better generation scheme
    appEnvs.forEach(environment -> {
      String infraId =
          wingsPersistence.createQuery(Infra.class).field("envId").equal(environment.getUuid()).get().getUuid();
      List<Host> hosts = wingsPersistence.createQuery(Host.class)
                             .field("appId")
                             .equal(environment.getAppId())
                             .field("infraId")
                             .equal(infraId)
                             .asList();
      ServiceTemplate template = wingsPersistence.query(ServiceTemplate.class, new PageRequest<>()).get(0);
      template.setService(services.get(0));
      Release release = wingsPersistence.query(Release.class, new PageRequest<>()).get(0);
      Artifact artifact = wingsPersistence.query(Artifact.class, new PageRequest<>()).get(0);

      shuffle(hosts);
      createDeployActivity(application, environment, template, hosts.get(0), release, artifact, Status.RUNNING);
      createStartActivity(application, environment, template, hosts.get(1), Status.COMPLETED);
      createStopActivity(application, environment, template, hosts.get(2), Status.FAILED);
      createDeployActivity(application, environment, template, hosts.get(0), release, artifact, Status.ABORTED);
    });
  }

  private void createStopActivity(
      Application application, Environment environment, ServiceTemplate template, Host host, Status status) {
    Activity activity = wingsPersistence.saveAndGet(Activity.class,
        anActivity()
            .withAppId(application.getUuid())
            .withCommandType("COMMAND")
            .withCommandName("STOP")
            .withEnvironmentId(environment.getUuid())
            .withHostName(host.getHostName())
            .withServiceId(template.getService().getUuid())
            .withServiceName(template.getService().getName())
            .withServiceTemplateId(template.getUuid())
            .withServiceTemplateName(template.getName())
            .withStatus(status)
            .build());

    addLogLine(application, template, host, activity,
        "------ deploying to " + host.getHostName() + ":" + template.getName() + " -------");
    addLogLine(application, template, host, activity, getTimeStamp() + "INFO connecting to " + host.getHostName());
    addLogLine(application, template, host, activity, getTimeStamp() + "INFO starting tomcat ./bin/startup.sh");
  }

  private void createStartActivity(
      Application application, Environment environment, ServiceTemplate template, Host host, Status status) {
    Activity activity = wingsPersistence.saveAndGet(Activity.class,
        anActivity()
            .withAppId(application.getUuid())
            .withCommandType("COMMAND")
            .withCommandName("START")
            .withEnvironmentId(environment.getUuid())
            .withHostName(host.getHostName())
            .withServiceId(template.getService().getUuid())
            .withServiceName(template.getService().getName())
            .withServiceTemplateId(template.getUuid())
            .withServiceTemplateName(template.getName())
            .withStatus(status)
            .build());

    addLogLine(application, template, host, activity,
        "------ deploying to " + host.getHostName() + ":" + template.getName() + " -------");
    addLogLine(application, template, host, activity, getTimeStamp() + "INFO connecting to " + host.getHostName());
    addLogLine(application, template, host, activity, getTimeStamp() + "INFO starting tomcat ./bin/startup.sh");
  }

  private void createDeployActivity(Application application, Environment environment, ServiceTemplate template,
      Host host, Release release, Artifact artifact, Status status) {
    Activity activity = wingsPersistence.saveAndGet(Activity.class,
        anActivity()
            .withAppId(application.getUuid())
            .withArtifactName(artifact.getDisplayName())
            .withCommandType("COMMAND")
            .withCommandName("DEPLOY")
            .withEnvironmentId(environment.getUuid())
            .withHostName(host.getHostName())
            .withReleaseName(release.getReleaseName())
            .withReleaseId(release.getUuid())
            .withServiceId(template.getService().getUuid())
            .withServiceName(template.getService().getName())
            .withServiceTemplateId(template.getUuid())
            .withServiceTemplateName(template.getName())
            .withStatus(status)
            .build());

    addLogLine(application, template, host, activity,
        "------ deploying to " + host.getHostName() + ":" + template.getName() + " -------");
    addLogLine(application, template, host, activity, getTimeStamp() + "INFO connecting to " + host.getHostName());
    addLogLine(application, template, host, activity, getTimeStamp() + "INFO stopping tomcat ./bin/shutdown.sh");
    addLogLine(application, template, host, activity,
        getTimeStamp() + "INFO copying artifact artifact.war to stating /home/staging");
    addLogLine(application, template, host, activity, getTimeStamp() + "INFO untar artifact to /home/tomcat");
    addLogLine(application, template, host, activity, getTimeStamp() + "INFO starting tomcat ./bin/startup.sh");
  }

  private void addLogLine(
      Application application, ServiceTemplate template, Host host, Activity activity, String logLine) {
    wingsPersistence.save(aLog()
                              .withAppId(application.getUuid())
                              .withActivityId(activity.getUuid())
                              .withHostName(host.getHostName())
                              .withLogLine(logLine)
                              .build());
  }

  private String getTimeStamp() {
    TimeZone tz = TimeZone.getTimeZone("UTC");
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mmZ");
    df.setTimeZone(tz);
    return df.format(new Date());
  }

  private void createGlobalSettings() {
    WebTarget target = client.target("http://localhost:9090/wings/settings/?appId=" + SettingAttribute.GLOBAL_APP_ID);
    System.out.println(JsonUtils.asJson(aSettingAttribute()
                                            .withName("Wings Jenkins")
                                            .withValue(aJenkinsConfig()
                                                           .withJenkinsUrl("https://jenkins-wingssoftware.rhcloud.com")
                                                           .withUsername("admin")
                                                           .withPassword("W!ngs")
                                                           .build())
                                            .build()));
    target.request().post(Entity.entity(aSettingAttribute()
                                            .withName("Wings Jenkins")
                                            .withValue(aJenkinsConfig()
                                                           .withJenkinsUrl("https://jenkins-wingssoftware.rhcloud.com")
                                                           .withUsername("admin")
                                                           .withPassword("W!ngs")
                                                           .build())
                                            .build(),
                              APPLICATION_JSON),
        new GenericType<RestResponse<SettingAttribute>>() {});
    target.request().post(Entity.entity(aSettingAttribute()
                                            .withName("SMTP")
                                            .withValue(aSmtpConfig()
                                                           .withFromAddress("wings_test@wings.software")
                                                           .withUsername("wings_test")
                                                           .withPassword("@wes0me@pp")
                                                           .withPort(465)
                                                           .withUseSSL(true)
                                                           .build())
                                            .build(),
                              APPLICATION_JSON),
        new GenericType<RestResponse<SettingAttribute>>() {});
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

  private List<Service> addServices(String appId, List<AppContainer> appContainers) throws IOException {
    serviceNames = new ArrayList<>(seedNames);
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
      configFileNames = new ArrayList<>(seedNames);
      addConfigFilesToEntity(response.getResource(), DEFAULT_TEMPLATE_ID, NUM_CONFIG_FILE_PER_SERVICE);
    }
    return services;
  }

  private void addConfigFilesToEntity(Base entity, String templateId, int numConfigFilesToBeAdded) throws IOException {
    while (numConfigFilesToBeAdded > 0) {
      if (addOneConfigFileToEntity(templateId, entity.getUuid())) {
        numConfigFilesToBeAdded--;
      }
    }
  }

  private boolean addOneConfigFileToEntity(String templateId, String entityId) throws IOException {
    WebTarget target =
        client.target(format("http://localhost:9090/wings/configs/?entityId=%s&templateId=%s", entityId, templateId));
    File file = getTestFile(getName(configFileNames) + ".properties");
    FileDataBodyPart filePart = new FileDataBodyPart("file", file);
    FormDataMultiPart multiPart =
        new FormDataMultiPart().field("name", file.getName()).field("relativePath", "./configs/");
    multiPart.bodyPart(filePart);
    Response response = target.request().post(Entity.entity(multiPart, multiPart.getMediaType()));
    return response.getStatus() == 200;
  }

  private List<AppContainer> addAppContainers(String appId) {
    int containersToBeAdded = NUM_APP_CONTAINER_PER_APP;
    while (containersToBeAdded > 0) {
      if (addOneAppContainer(appId)) {
        containersToBeAdded--;
      }
    }
    return getAppContainers(appId);
  }

  private List<AppContainer> getAppContainers(String appId) {
    RestResponse<PageResponse<AppContainer>> response =
        client.target("http://localhost:9090/wings/app-containers/?appId=" + appId)
            .request()
            .get(new GenericType<RestResponse<PageResponse<AppContainer>>>() {});
    return response.getResource().getResponse();
  }

  private boolean addOneAppContainer(String appId) {
    WebTarget target = client.target("http://localhost:9090/wings/app-containers/?appId=" + appId);
    String version = format("%s.%s.%s", randomInt(10), randomInt(100), randomInt(1000));
    String name = containerNames.get(randomInt() % containerNames.size());

    try {
      File file = getTestFile(name);
      FileDataBodyPart filePart = new FileDataBodyPart("file", file);
      FormDataMultiPart multiPart = new FormDataMultiPart()
                                        .field("name", name)
                                        .field("version", version)
                                        .field("description", randomText(20))
                                        .field("sourceType", "FILE_UPLOAD")
                                        .field("standard", "false");
      multiPart.bodyPart(filePart);
      Response response = target.request().post(Entity.entity(multiPart, multiPart.getMediaType()));
      return response.getStatus() == 200;
    } catch (IOException e) {
      log().info("Error occured in uploading app container" + e.getMessage());
    }
    return false;
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
      //      createAndTagHosts(response.getResource());
    }
    return environments;
  }

  private void createAndTagHosts(Environment environment) {
    RestResponse<PageResponse<Tag>> response =
        client
            .target(format("http://localhost:9090/wings/tag-types?appId=%s&envId=%s", environment.getAppId(),
                environment.getUuid()))
            .request()
            .get(new GenericType<RestResponse<PageResponse<Tag>>>() {});
    log().info(response.getResource().getResponse().toString());
  }

  private void addHostsToEnv(Environment env) throws IOException {
    if (envAttr == null) {
      envAttr = wingsPersistence.saveAndGet(SettingAttribute.class,
          aSettingAttribute().withAppId(env.getAppId()).withValue(new BastionConnectionAttributes()).build());
    }

    WebTarget target =
        client.target(format("http://localhost:9090/wings/hosts?appId=%s&envId=%s", env.getAppId(), env.getUuid()));

    List<SettingAttribute> connectionAttributes = wingsPersistence.createQuery(SettingAttribute.class)
                                                      .field("appId")
                                                      .equal(env.getAppId())
                                                      .field("value.type")
                                                      .equal(HOST_CONNECTION_ATTRIBUTES)
                                                      .asList();

    for (int i = 1; i <= NUM_HOSTS_PER_INFRA; i++) {
      Response response = target.request().post(
          Entity.entity(ImmutableMap.of("hostNames", asList("host" + i + ".ec2.aws.com"), "hostConnAttrs",
                            connectionAttributes.get(i % connectionAttributes.size()), "bastionConnAttrs", envAttr),
              APPLICATION_JSON));
      assertThat(response.getStatus()).isEqualTo(OK.getStatusCode());
    }
  }

  private File getTestFile(String name) throws IOException {
    File file = new File(testFolder.getRoot().getAbsolutePath() + "/" + name);
    if (!file.isFile()) {
      file = testFolder.newFile(name);
    }
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

  private String randomText(int length) { // TODO: choose words start to word end boundary
    int low = randomInt(50);
    int high = length + low > randomSeedString.length() ? randomSeedString.length() - low : length + low;
    return randomSeedString.substring(low, high);
  }
}
