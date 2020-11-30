package io.harness;

import static org.mockito.Mockito.mock;

import io.harness.beans.EmbeddedUser;
import io.harness.encryptors.CustomEncryptor;
import io.harness.encryptors.Encryptors;
import io.harness.encryptors.KmsEncryptor;
import io.harness.encryptors.VaultEncryptor;
import io.harness.encryptors.clients.AwsKmsEncryptor;
import io.harness.encryptors.clients.AwsSecretsManagerEncryptor;
import io.harness.encryptors.clients.AzureVaultEncryptor;
import io.harness.encryptors.clients.CyberArkVaultEncryptor;
import io.harness.encryptors.clients.GcpKmsEncryptor;
import io.harness.encryptors.clients.HashicorpVaultEncryptor;
import io.harness.encryptors.clients.LocalEncryptor;
import io.harness.factory.ClosingFactory;
import io.harness.factory.ClosingFactoryModule;
import io.harness.govern.ProviderModule;
import io.harness.govern.ServersModule;
import io.harness.mongo.MongoPersistence;
import io.harness.morphia.MorphiaRegistrar;
import io.harness.persistence.HPersistence;
import io.harness.persistence.UserProvider;
import io.harness.queue.QueueController;
import io.harness.rule.InjectorRuleMixin;
import io.harness.secretmanagers.SecretManagerConfigService;
import io.harness.secretmanagers.SecretsManagerRBACService;
import io.harness.secrets.SecretsAuditService;
import io.harness.secrets.SecretsFileService;
import io.harness.secrets.SecretsRBACService;
import io.harness.secrets.setupusage.SecretSetupUsageBuilder;
import io.harness.secrets.setupusage.SecretSetupUsageBuilders;
import io.harness.serializer.KryoModule;
import io.harness.serializer.KryoRegistrar;
import io.harness.serializer.SMCoreRegistrars;
import io.harness.testlib.module.MongoRuleMixin;
import io.harness.testlib.module.TestMongoModule;
import io.harness.threading.CurrentThreadExecutor;
import io.harness.threading.ExecutorModule;
import io.harness.time.TimeModule;
import io.harness.version.VersionModule;

import com.google.common.collect.ImmutableSet;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import java.io.Closeable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.mongodb.morphia.converters.TypeConverter;

@Slf4j
public class SMCoreRule implements MethodRule, InjectorRuleMixin, MongoRuleMixin {
  ClosingFactory closingFactory;

  public SMCoreRule(ClosingFactory closingFactory) {
    this.closingFactory = closingFactory;
  }

  @Override
  public List<Module> modules(List<Annotation> annotations) throws Exception {
    ExecutorModule.getInstance().setExecutorService(new CurrentThreadExecutor());

    List<Module> modules = new ArrayList<>();
    modules.add(new ClosingFactoryModule(closingFactory));
    modules.add(KryoModule.getInstance());
    modules.add(new ProviderModule() {
      @Provides
      @Singleton
      Set<Class<? extends KryoRegistrar>> kryoRegistrars() {
        return ImmutableSet.<Class<? extends KryoRegistrar>>builder().addAll(SMCoreRegistrars.kryoRegistrars).build();
      }

      @Provides
      @Singleton
      Set<Class<? extends MorphiaRegistrar>> morphiaRegistrars() {
        return ImmutableSet.<Class<? extends MorphiaRegistrar>>builder()
            .addAll(SMCoreRegistrars.morphiaRegistrars)
            .build();
      }

      @Provides
      @Singleton
      Set<Class<? extends TypeConverter>> morphiaConverters() {
        return ImmutableSet.<Class<? extends TypeConverter>>builder().build();
      }
    });

    modules.add(mongoTypeModule(annotations));

    modules.add(new AbstractModule() {
      @Override
      protected void configure() {
        bind(HPersistence.class).to(MongoPersistence.class);
        bind(SecretManagerConfigService.class).toInstance(mock(SecretManagerConfigService.class));
        bind(QueueController.class).toInstance(new QueueController() {
          @Override
          public boolean isPrimary() {
            return true;
          }

          @Override
          public boolean isNotPrimary() {
            return false;
          }
        });

        binder()
            .bind(SecretSetupUsageBuilder.class)
            .annotatedWith(Names.named(SecretSetupUsageBuilders.SERVICE_VARIABLE_SETUP_USAGE_BUILDER.getName()))
            .toInstance(mock(SecretSetupUsageBuilder.class));
        binder()
            .bind(SecretSetupUsageBuilder.class)
            .annotatedWith(Names.named(SecretSetupUsageBuilders.CONFIG_FILE_SETUP_USAGE_BUILDER.getName()))
            .toInstance(mock(SecretSetupUsageBuilder.class));
        binder()
            .bind(SecretSetupUsageBuilder.class)
            .annotatedWith(Names.named(SecretSetupUsageBuilders.SETTING_ATTRIBUTE_SETUP_USAGE_BUILDER.getName()))
            .toInstance(mock(SecretSetupUsageBuilder.class));
        binder()
            .bind(SecretSetupUsageBuilder.class)
            .annotatedWith(Names.named(SecretSetupUsageBuilders.SECRET_MANAGER_CONFIG_SETUP_USAGE_BUILDER.getName()))
            .toInstance(mock(SecretSetupUsageBuilder.class));

        binder()
            .bind(VaultEncryptor.class)
            .annotatedWith(Names.named(Encryptors.HASHICORP_VAULT_ENCRYPTOR.getName()))
            .to(HashicorpVaultEncryptor.class);

        binder()
            .bind(VaultEncryptor.class)
            .annotatedWith(Names.named(Encryptors.AWS_VAULT_ENCRYPTOR.getName()))
            .to(AwsSecretsManagerEncryptor.class);

        binder()
            .bind(VaultEncryptor.class)
            .annotatedWith(Names.named(Encryptors.AZURE_VAULT_ENCRYPTOR.getName()))
            .to(AzureVaultEncryptor.class);

        binder()
            .bind(VaultEncryptor.class)
            .annotatedWith(Names.named(Encryptors.CYBERARK_VAULT_ENCRYPTOR.getName()))
            .to(CyberArkVaultEncryptor.class);

        binder()
            .bind(KmsEncryptor.class)
            .annotatedWith(Names.named(Encryptors.AWS_KMS_ENCRYPTOR.getName()))
            .to(AwsKmsEncryptor.class);

        binder()
            .bind(KmsEncryptor.class)
            .annotatedWith(Names.named(Encryptors.GCP_KMS_ENCRYPTOR.getName()))
            .to(GcpKmsEncryptor.class);

        binder()
            .bind(KmsEncryptor.class)
            .annotatedWith(Names.named(Encryptors.LOCAL_ENCRYPTOR.getName()))
            .to(LocalEncryptor.class);

        binder()
            .bind(KmsEncryptor.class)
            .annotatedWith(Names.named(Encryptors.GLOBAL_GCP_KMS_ENCRYPTOR.getName()))
            .to(GcpKmsEncryptor.class);

        binder()
            .bind(KmsEncryptor.class)
            .annotatedWith(Names.named(Encryptors.GLOBAL_AWS_KMS_ENCRYPTOR.getName()))
            .to(AwsKmsEncryptor.class);

        binder()
            .bind(CustomEncryptor.class)
            .annotatedWith(Names.named(Encryptors.CUSTOM_ENCRYPTOR.getName()))
            .toInstance(mock(CustomEncryptor.class));

        bind(SecretsFileService.class).toInstance(mock(SecretsFileService.class));
        bind(SecretsAuditService.class).toInstance(mock(SecretsAuditService.class));
        bind(SecretsRBACService.class).toInstance(mock(SecretsRBACService.class));
        bind(SecretsManagerRBACService.class).toInstance(mock(SecretsManagerRBACService.class));
      }
    });
    modules.add(VersionModule.getInstance());
    modules.add(TimeModule.getInstance());
    modules.add(TestMongoModule.getInstance());
    modules.add(SecretManagementCoreModule.getInstance());
    return modules;
  }

  @Override
  public void initialize(Injector injector, List<Module> modules) {
    for (Module module : modules) {
      if (module instanceof ServersModule) {
        for (Closeable server : ((ServersModule) module).servers(injector)) {
          closingFactory.addServer(server);
        }
      }
    }
    HPersistence persistence = injector.getInstance(HPersistence.class);
    persistence.registerUserProvider(new UserProvider() {
      @Override
      public EmbeddedUser activeUser() {
        return EmbeddedUser.builder().email("test@test.com").name("test").uuid("dummy").build();
      }
    });
  }

  @Override
  public Statement apply(Statement statement, FrameworkMethod frameworkMethod, Object target) {
    return applyInjector(statement, frameworkMethod, target);
  }
}
