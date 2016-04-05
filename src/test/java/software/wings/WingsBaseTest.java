package software.wings;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.mongodb.MongoClient;
import com.mongodb.ServerAddress;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.rules.ExternalResource;
import org.junit.rules.TestRule;
import org.mongodb.morphia.Datastore;
import org.mongodb.morphia.Morphia;
import ru.vyarus.guice.validator.ValidationModule;
import software.wings.beans.ReadPref;
import software.wings.dl.WingsMongoPersistence;
import software.wings.dl.WingsPersistence;
import software.wings.rules.WingsRule;
import software.wings.service.impl.ArtifactServiceImpl;
import software.wings.service.intfc.ArtifactService;

import java.net.InetSocketAddress;
import java.util.Map;

/**
 * Created by peeyushaggarwal on 4/5/16.
 */
public class WingsBaseTest { @Rule public WingsRule wingsRule = new WingsRule(); }
