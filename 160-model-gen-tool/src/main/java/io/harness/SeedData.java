/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness;

import com.google.common.collect.ImmutableList;
import java.security.SecureRandom;
import java.util.List;

/**
 * Created by anubhaw on 6/3/16.
 */
public class SeedData {
  private static final SecureRandom random = new SecureRandom();
  /**
   * The constant randomSeedString.
   */
  public static final String randomSeedString =
      "Lorem Ipsum is simply dummy text of the printing and typesetting industry. "
      + "Lorem Ipsum has been the industry's standard dummy text ever since the 1500s, "
      + "when an unknown printer took a galley of type and scrambled it to make a type specimen book. "
      + "It has survived not only five centuries, but also the leap into electronic typesetting, "
      + "remaining essentially unchanged. It was popularised in the 1960s with the release of "
      + "Letraset sheets containing Lorem Ipsum passages, and more recently with desktop publishing software "
      + "like Aldus PageMaker including versions of Lorem Ipsum";

  /**
   * The constant containerNames.
   */
  protected static final List<String> containerNames = ImmutableList.of("AOLserver", "Apache HTTP Server",
      "Apache Tomcat", "Barracuda Web Server", "Boa", "Caddy", "Caudium", "Cherokee HTTP Server", "GlassFish",
      "Hiawatha", "HFS", "IBM HTTP Server", "Internet Information Services", "Jetty", "Jexus", "lighttpd",
      "LiteSpeed Web Server", "Mongoose", "Monkey HTTP Server", "NaviServer", "NCSA HTTPd", "Nginx",
      "OpenLink Virtuoso", "OpenLiteSpeed", "Oracle HTTP Server", "Oracle iPlanet Web Server", "Oracle WebLogic Server",
      "Resin Open Source", "Resin Professional", "thttpd", "TUX web server", "Wakanda Server", "WEBrick", "Xitami",
      "Yaws", "Zeus Web Server", "Zope");
  /**
   * The constant seedNames.
   */
  protected static final List<String> seedNames = ImmutableList.of("Abaris", "Abundantia", "Acca Larentia", "Achelois",
      "Achelous", "Acheron", "Achilles", "Acidalia", "Acis", "Acmon", "Acoetes", "Actaeon", "Adamanthea", "Adephagia",
      "Adonis", "Adrastea", "Adrasteia", "Aeacos", "Aeacus", "Aegaeon", "Aegeus", "Aegina", "Aegle", "Aello",
      "Aellopos", "Aeneas", "Aeolos", "Aeolus", "Aequitas", "Aer", "Aerecura", "Aesacus", "Aesculapius", "Aesir",
      "Aeson", "Aeternitas", "Aethalides", "Aether", "Aethon", "Aetna", "Aeëtes", "Agamemnon", "Agave", "Agdistes",
      "Agdos", "Aglaea", "Aglaia", "Aglaulus", "Aglauros", "Aglaurus", "Agraulos", "Agrotara", "Agrotora", "Aiakos",
      "Aigle", "Aiolos", "Aion", "Air", "Aither", "Aius Locutius", "Ajax the Great", "Ajax the Lesser", "Alcemana",
      "Alcides", "Alcmena", "Alcmene", "Alcyone", "Alecto", "Alectrona", "Alernus or Elernus", "Alexandra", "Alkyone",
      "Aloadae", "Alpheos", "Alpheus", "Althaea", "Amalthea", "Amaltheia", "Amarynthia", "Ampelius", "Amphion",
      "Amphitrite", "Amphitryon", "Amymone", "Ananke", "Anaxarete", "Andhrimnir", "Andromeda", "Angerona", "Angitia",
      "Angrboda", "Anius", "Anna Perenna", "Annona", "Antaeus", "Antaios", "Anteros", "Antevorta", "Anticlea",
      "Antiklia", "Antiope", "Apate", "Aphrodite", "Apollo", "Apollon", "Aquilo", "Arachne", "Arcas", "Areon", "Ares",
      "Arethusa", "Argeos", "Argus", "Ariadne", "Arimanius", "Arion", "Aristaeus", "Aristaios", "Aristeas", "Arkas",
      "Arkeus Ultor", "Artemis", "Asclepius", "Asklepios", "Asopus", "Asteria", "Asterie", "Astraea", "Astraeus",
      "Astraios", "Astrild", "Atalanta", "Ate", "Athamas", "Athamus", "Athena", "Athene", "Athis", "Atla", "Atlantides",
      "Atlas", "Atropos", "Attis", "Attropus", "Audhumla", "Augean Stables", "Augian Stables", "Aura", "Aurai",
      "Aurora", "Autolycus", "Autolykos", "Auxesia", "Averruncus", "Bacchae", "Bacchantes", "Bacchus", "Balder",
      "Balios", "Balius", "Battus", "Baucis", "Bellerophon", "Bellona or Duellona", "Beyla", "Bia", "Bias", "Bona Dea",
      "Bonus Eventus", "Boreads", "Boreas", "Borghild", "Bragi", "Briareos", "Briareus", "Bromios", "Brono", "Bubona",
      "Byblis", "Bylgia", "Caca", "Cacus", "Cadmus", "Caelus", "Caeneus", "Caenis", "Calais", "Calchas", "Calliope",
      "Callisto", "Calypso", "Camenae", "Canens", "Cardea", "Carmenta", "Carmentes", "Carna", "Cassandra", "Castor",
      "Caunus", "Cecrops", "Celaeno", "Celoneo", "Ceneus", "Cephalus", "Cerberus", "Cercopes", "Ceres", "Cerigo",
      "Cerynean Hind", "Ceryneian Hind", "Cerynitis", "Ceto", "Ceyx", "Chaos", "Chariclo", "Charites", "Charon",
      "Charybdis", "Cheiron", "Chelone", "Chimaera", "Chimera", "Chione", "Chiron", "Chloe", "Chloris", "Chronos",
      "Chronus", "Chthonia", "Cinyras", "Cipus", "Circe", "Clementia", "Clio", "Cloacina", "Clotho", "Clymene", "Coeus",
      "Coltus", "Comus", "Concordia", "Consus", "Cornix", "Cottus", "Cotys", "Cotytto", "Cratus", "Cretan Bull",
      "Crius", "Cronos", "Cronus", "Cupid", "Cura", "Cyane", "Cybele", "Cyclopes", "Cyclops", "Cygnus", "Cyllarus",
      "Cynthia", "Cyparissus", "Cyrene", "Cytherea", "Cyáneë", "Daedalion", "Daedalus", "Dagur", "Danae", "Daphnaie",
      "Daphne", "Dea Dia", "Dea Tacita", "Decima", "Deimos", "Deimus", "Deino", "Delos", "Delphyne", "Demeter",
      "Demphredo", "Deo", "Despoena", "Deucalion", "Deukalion", "Devera or Deverra", "Deïanira", "Di inferi", "Diana",
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

  public static String randomText(int length) { // TODO: choose words start to word end boundary
    int low = random.nextInt(50);
    int high = length + low > randomSeedString.length() ? randomSeedString.length() - low : length + low;
    return randomSeedString.substring(low, high);
  }
}
