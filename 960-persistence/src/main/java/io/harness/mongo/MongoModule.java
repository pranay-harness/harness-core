/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.mongo;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;
import static org.mongodb.morphia.logging.MorphiaLoggerFactory.registerLogger;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.GeneralException;
import io.harness.exception.UnexpectedException;
import io.harness.logging.MorphiaLoggerFactory;
import io.harness.mongo.index.migrator.Migrator;
import io.harness.mongo.tracing.TracerModule;
import io.harness.morphia.MorphiaModule;
import io.harness.persistence.Store;
import io.harness.serializer.KryoModule;

import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.name.Named;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.AdvancedDatastore;
import org.mongodb.morphia.Morphia;
import org.mongodb.morphia.ObjectFactory;

@OwnedBy(HarnessTeam.PL)
@Slf4j
public class MongoModule extends AbstractModule {
  private static volatile MongoModule instance;

  static MongoModule getInstance() {
    if (instance == null) {
      instance = new MongoModule();
    }
    return instance;
  }

  @Provides
  @Named("defaultMongoClientOptions")
  @Singleton
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

  private MongoModule() {
    try {
      registerLogger(MorphiaLoggerFactory.class);
    } catch (Exception e) {
      // happens when MorphiaLoggerFactory.get has already been called.
      log.warn("Failed to register logger", e);
    }
  }

  @Override
  protected void configure() {
    install(ObjectFactoryModule.getInstance());
    install(MorphiaModule.getInstance());
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
  @Named("primaryDatastore")
  @Singleton
  public AdvancedDatastore primaryDatastore(MongoConfig mongoConfig, @Named("morphiaClasses") Set<Class> classes,
      @Named("morphiaInterfaceImplementersClasses") Map<String, Class> morphiaInterfaceImplementers, Morphia morphia,
      ObjectFactory objectFactory, IndexManager indexManager) {
    for (Class clazz : classes) {
      if (morphia.getMapper().getMCMap().get(clazz.getName()).getCollectionName().startsWith("!!!custom_")) {
        throw new UnexpectedException(format("The custom collection name for %s is not provided", clazz.getName()));
      }
    }

    MongoClientOptions primaryMongoClientOptions;
    MongoSSLConfig mongoSSLConfig = mongoConfig.getMongoSSLConfig();
    if (mongoSSLConfig != null && mongoSSLConfig.isMongoSSLEnabled()) {
      primaryMongoClientOptions = getMongoSslContextClientOptions(mongoConfig);
    } else {
      primaryMongoClientOptions = MongoClientOptions.builder()
                                      .retryWrites(true)
                                      .connectTimeout(mongoConfig.getConnectTimeout())
                                      .serverSelectionTimeout(mongoConfig.getServerSelectionTimeout())
                                      .maxConnectionIdleTime(mongoConfig.getMaxConnectionIdleTime())
                                      .connectionsPerHost(mongoConfig.getConnectionsPerHost())
                                      .readPreference(mongoConfig.getReadPreference())
                                      .build();
    }

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

  @Provides
  @Named("locksMongoClient")
  @Singleton
  public MongoClient getLocksMongoClient(MongoConfig mongoConfig) {
    MongoClientURI uri;
    if (isNotEmpty(mongoConfig.getLocksUri())) {
      uri = new MongoClientURI(
          mongoConfig.getLocksUri(), MongoClientOptions.builder(getDefaultMongoClientOptions(mongoConfig)));
    } else {
      uri = new MongoClientURI(
          mongoConfig.getUri(), MongoClientOptions.builder(getDefaultMongoClientOptions(mongoConfig)));
    }
    return new MongoClient(uri);
  }

  @Provides
  @Named("locksDatabase")
  @Singleton
  public String getLocksDatabase(MongoConfig mongoConfig) {
    MongoClientURI uri;
    if (isNotEmpty(mongoConfig.getLocksUri())) {
      uri = new MongoClientURI(
          mongoConfig.getLocksUri(), MongoClientOptions.builder(getDefaultMongoClientOptions(mongoConfig)));
    } else {
      uri = new MongoClientURI(
          mongoConfig.getUri(), MongoClientOptions.builder(getDefaultMongoClientOptions(mongoConfig)));
    }
    return uri.getDatabase();
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
