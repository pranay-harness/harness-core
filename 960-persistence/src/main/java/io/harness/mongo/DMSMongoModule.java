package io.harness.mongo;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import io.harness.annotation.StoreIn;
import io.harness.annotations.ti.HarnessTrace;
import io.harness.exception.GeneralException;
import io.harness.exception.UnexpectedException;
import io.harness.logging.MorphiaLoggerFactory;
import io.harness.mongo.index.migrator.Migrator;
import io.harness.mongo.tracing.TracerModule;
import io.harness.morphia.HMappedClass;
import io.harness.morphia.MorphiaModule;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.morphia.MorphiaRegistrarHelperPut;
import io.harness.persistence.Store;
import io.harness.persistence.UserProvider;
import io.harness.serializer.KryoModule;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.ObjectFactory;
import org.mongodb.morphia.converters.TypeConverter;
import org.mongodb.morphia.mapping.MappedClass;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.morphia.MorphiaRegistrar.putClass;
import static java.lang.String.format;
import static org.mongodb.morphia.logging.MorphiaLoggerFactory.registerLogger;

@Slf4j
public class DMSMongoModule extends AbstractModule {

    private final MongoConfig mongoConfig;

    public DMSMongoModule(MongoConfig mongoConfig) {
        this.mongoConfig=mongoConfig;
    }


    public static MongoClientOptions getDefaultMongoClientOptions(MongoConfig mongoConfig) {
        MongoClientOptions defaultMongoClientOptions;
        MongoSSLConfig mongoSSLConfig = mongoConfig.getMongoSSLConfig();
        if (mongoSSLConfig != null && mongoSSLConfig.isMongoSSLEnabled()) {
            defaultMongoClientOptions = getMongoSslContextClientOptions(mongoConfig);
        } else {
            defaultMongoClientOptions = MongoClientOptions.builder()
                    .retryWrites(true)
                    .connectTimeout(30000)
                    .serverSelectionTimeout(90000)
                    .maxConnectionIdleTime(600000)
                    .connectionsPerHost(300)
                    .build();
        }
        return defaultMongoClientOptions;
    }

    public static AdvancedDatastore createDatastore(Morphia morphia, String uri) {
        MongoConfig mongoConfig = MongoConfig.builder().build();

        MongoClientURI clientUri =
                new MongoClientURI(uri, MongoClientOptions.builder(getDefaultMongoClientOptions(mongoConfig)));
        MongoClient mongoClient = new MongoClient(clientUri);

        AdvancedDatastore datastore = (AdvancedDatastore) morphia.createDatastore(mongoClient, clientUri.getDatabase());
        datastore.setQueryFactory(new QueryFactory());

        return datastore;
    }


    @Override
    protected void configure() {
        install(ObjectFactoryModule.getInstance());
        //install(MorphiaModule.getInstance());
        install(KryoModule.getInstance());
        install(TracerModule.getInstance());

        MapBinder.newMapBinder(binder(), String.class, Migrator.class);
    }

    private static MongoClientOptions getMongoSslContextClientOptions(MongoConfig mongoConfig) {
        MongoClientOptions primaryMongoClientOptions;
        validateSSLMongoConfig(mongoConfig);
        MongoSSLConfig mongoSSLConfig = mongoConfig.getMongoSSLConfig();
        String trustStorePath = mongoSSLConfig.getMongoTrustStorePath();
        String trustStorePassword = mongoSSLConfig.getMongoTrustStorePassword();
        primaryMongoClientOptions = MongoClientOptions.builder()
                .retryWrites(true)
                .connectTimeout(mongoConfig.getConnectTimeout())
                .serverSelectionTimeout(mongoConfig.getServerSelectionTimeout())
                .maxConnectionIdleTime(mongoConfig.getMaxConnectionIdleTime())
                .connectionsPerHost(mongoConfig.getConnectionsPerHost())
                .readPreference(mongoConfig.getReadPreference())
                .sslEnabled(mongoSSLConfig.isMongoSSLEnabled())
                .sslInvalidHostNameAllowed(true)
                .sslContext(sslContext(trustStorePath, trustStorePassword))
                .build();
        return primaryMongoClientOptions;
    }

    private static void validateSSLMongoConfig(MongoConfig mongoConfig) {
        MongoSSLConfig mongoSSLConfig = mongoConfig.getMongoSSLConfig();
        Preconditions.checkNotNull(mongoSSLConfig,
                "mongoSSLConfig must be set under mongo config if SSL context creation is requested or mongoSSLEnabled is set to true");
        Preconditions.checkArgument(
                mongoSSLConfig.isMongoSSLEnabled(), "mongoSSLEnabled must be set to true for MongoSSLConfiguration");
        Preconditions.checkArgument(StringUtils.isNotBlank(mongoSSLConfig.getMongoTrustStorePath()),
                "mongoTrustStorePath must be set if mongoSSLEnabled is set to true");
    }

    @Provides
    @Named("dms")
    @Singleton
    public AdvancedDatastore dms(@Named("dmsmorphiaClasses") Set<Class> classes,
                                              @Named("dmsmorphiaInterfaceImplementersClasses") Map<String, Class> morphiaInterfaceImplementers, Morphia morphia,
                                              ObjectFactory objectFactory, IndexManager indexManager) {


        MongoClientOptions primaryMongoClientOptions = primaryMongoClientOptions =
                MongoClientOptions.builder()
                        .retryWrites(true)
                        .connectTimeout(mongoConfig.getConnectTimeout())
                        .serverSelectionTimeout(mongoConfig.getServerSelectionTimeout())
                        .maxConnectionIdleTime(mongoConfig.getMaxConnectionIdleTime())
                        .connectionsPerHost(mongoConfig.getConnectionsPerHost())
                        .readPreference(mongoConfig.getReadPreference())
                        .build();

        MongoClientURI uri =
                new MongoClientURI(mongoConfig.getUri(), MongoClientOptions.builder(primaryMongoClientOptions));
        MongoClient mongoClient = new MongoClient(uri);

        AdvancedDatastore primaryDatastore = (AdvancedDatastore) morphia.createDatastore(mongoClient, uri.getDatabase());
        primaryDatastore.setQueryFactory(new QueryFactory(mongoConfig.getTraceMode()));

        Store store = null;
        if (Objects.nonNull(mongoConfig.getAliasDBName())) {
            store = Store.builder().name(mongoConfig.getAliasDBName()).build();
        }

        indexManager.ensureIndexes(mongoConfig.getIndexManagerMode(), primaryDatastore, morphia, store);

        HObjectFactory hObjectFactory = (HObjectFactory) objectFactory;

        ClassRefactoringManager.updateMovedClasses(primaryDatastore, morphiaInterfaceImplementers);
        hObjectFactory.setDatastore(primaryDatastore);

        return primaryDatastore;
    }

    @HarnessTrace
    @Provides
    @Named("dmsmorphiaClasses")
    @Singleton
    Set<Class> classes(Set<Class<? extends MorphiaRegistrar>> registrars) {
        Set<Class> classes = new HashSet<>();
        /*try {
            for (Class clazz : registrars) {
                Constructor<?> constructor = clazz.getConstructor();
                *//*if (clazz.isAnnotationPresent(StoreIn.class)){
                    //Arrays.stream(clazz.getAnnotationsByType(StoreIn.class)).filter();
                }*//*
                final MorphiaRegistrar morphiaRegistrar = (MorphiaRegistrar) constructor.newInstance();
                //morphiaRegistrar.registerClasses(classes);
            }
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            throw new GeneralException("Failed initializing morphia", e);
        }*/

        return classes;
    }



   /* @Provides
    @Singleton
    public Morphia morphia(@Named("dmsmorphiaClasses") Set<Class> classes,
                           @Named("dmsmorphiaClasses") Map<Class, String> customCollectionName, ObjectFactory objectFactory, Injector injector,
                           Set<Class<? extends TypeConverter>> morphiaConverters) {
        Morphia morphia = new Morphia();
        morphia.getMapper().getOptions().setObjectFactory(objectFactory);
        morphia.getMapper().getOptions().setMapSubPackages(true);

        Set<Class> classesCopy = new HashSet<>(classes);

        try {
            Method method =
                    morphia.getMapper().getClass().getDeclaredMethod("addMappedClass", MappedClass.class, boolean.class);
            method.setAccessible(true);

            for (Map.Entry<Class, String> entry : customCollectionName.entrySet()) {
                classesCopy.remove(entry.getKey());

                HMappedClass mappedClass = new HMappedClass(entry.getValue(), entry.getKey(), morphia.getMapper());

                method.invoke(morphia.getMapper(), mappedClass, Boolean.TRUE);
            }
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new UnexpectedException("We cannot add morphia MappedClass", e);
        }
        morphia.map(classesCopy);
        morphiaConverters.forEach(
                converter -> morphia.getMapper().getConverters().addConverter(injector.getInstance(converter)));
        return morphia;
    }*/

    @HarnessTrace
    @Provides
    @Named("dmsmorphiaInterfaceImplementersClasses")
    @Singleton
    Map<String, Class> collectMorphiaInterfaceImplementers(Set<Class<? extends MorphiaRegistrar>> registrars) {
        Map<String, Class> map = new ConcurrentHashMap<>();
        MorphiaRegistrarHelperPut h = (name, clazz) -> putClass(map, "io.harness." + name, clazz);
        MorphiaRegistrarHelperPut w = (name, clazz) -> putClass(map, "software.wings." + name, clazz);

        try {
            for (Class clazz : registrars) {
                Constructor<?> constructor = clazz.getConstructor();
                final MorphiaRegistrar morphiaRegistrar = (MorphiaRegistrar) constructor.newInstance();

               // morphiaRegistrar.registerImplementationClasses(h, w);
            }
        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new GeneralException("Failed to initialize MorphiaInterfaceImplementers", e);
        }

        return map;
    }


        private static SSLContext sslContext(String keystoreFile, String password) {
        SSLContext sslContext = null;
        try {
            KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
            InputStream in = new FileInputStream(keystoreFile);
            keystore.load(in, password.toCharArray());
            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keystore);
            sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), null);

        } catch (GeneralSecurityException | IOException exception) {
            throw new GeneralException("SSLContext exception: {}", exception);
        }
        return sslContext;
    }

}
