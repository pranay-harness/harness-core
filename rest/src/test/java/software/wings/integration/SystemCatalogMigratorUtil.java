package software.wings.integration;

import static software.wings.beans.AppContainer.Builder.anAppContainer;
import static software.wings.beans.SystemCatalog.Builder.aSystemCatalog;
import static software.wings.beans.SystemCatalog.CatalogType.APPSTACK;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.service.intfc.FileService.FileBucket.PLATFORMS;
import static software.wings.utils.ContainerFamily.TOMCAT;
import static software.wings.beans.SearchFilter.Operator.EQ;

import com.google.inject.Inject;

import ch.qos.logback.core.net.SyslogOutputStream;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.AppContainer;
import software.wings.beans.Base;
import software.wings.beans.SearchFilter;
import software.wings.beans.SystemCatalog;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.rules.Integration;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppContainerService;
import software.wings.service.intfc.SystemCatalogService;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Created by sgurubelli on 5/24/17.
 */
@Integration
@Ignore
public class SystemCatalogMigratorUtil extends WingsBaseTest {
  @Inject private MainConfiguration configuration;
  @Inject private SystemCatalogService systemCatalogService;
  @Inject private AppContainerService appContainerService;
  @Inject private AccountService accountService;

  private static final String AWS_S3_CATALOG_TOMCAT7 =
      "https://s3.amazonaws.com/harness-catalogs/appstack/apache-tomcat-7.0.78.tar.gz";
  private static final String AWS_S3_CATALOG_TOMCAT8 =
      "https://s3.amazonaws.com/harness-catalogs/appstack/apache-tomcat-8.5.15.tar.gz";

  private static final String AWS_S3_CATALOG_TOMCAT7_HARDENED =
      "https://s3.amazonaws.com/harness-catalogs/appstack/apache-tomcat-7.0.78-hardened.tar.gz";
  private static final String AWS_S3_CATALOG_TOMCAT8_HARDENED =
      "https://s3.amazonaws.com/harness-catalogs/appstack/apache-tomcat-8.5.15-hardened.tar.gz";

  @Test
  public void createSystemAppStackCatalogs() throws IOException {
    long fileSize = configuration.getFileUploadLimits().getAppContainerLimit();
    List<SystemCatalog> systemCatalogs = (systemCatalogService.list(
        aPageRequest().addFilter("catalogType", EQ, APPSTACK).addFilter("family", EQ, TOMCAT).build()));
    List<String> catalognames = systemCatalogs.stream().map(SystemCatalog::getFileName).collect(Collectors.toList());

    System.out.println("Creating System App Stack Catalogs");
    // Create Tomcat 7 Standard
    SystemCatalog systemCatalog = null;
    if (!catalognames.contains("apache-tomcat-7.0.78.tar.gz")) {
      systemCatalog = aSystemCatalog()
                          .withCatalogType(APPSTACK)
                          .withName("Standard Tomcat 7")
                          .withFileName("apache-tomcat-7.0.78.tar.gz")
                          .withAppId(Base.GLOBAL_APP_ID)
                          .withFamily(TOMCAT)
                          .withNotes("System created.")
                          .build();
      systemCatalogService.save(systemCatalog, AWS_S3_CATALOG_TOMCAT7, PLATFORMS, fileSize);
    }
    if (!catalognames.contains("apache-tomcat-7.0.78-hardened.tar.gz")) {
      systemCatalog = aSystemCatalog()
                          .withCatalogType(APPSTACK)
                          .withName("Hardened Tomcat 7")
                          .withFileName("apache-tomcat-7.0.78-hardened.tar.gz")
                          .withAppId(Base.GLOBAL_APP_ID)
                          .withFamily(TOMCAT)
                          .withNotes("System created. Hardened Version")
                          .build();
      systemCatalogService.save(systemCatalog, AWS_S3_CATALOG_TOMCAT7_HARDENED, PLATFORMS, fileSize);
    }
    if (!catalognames.contains("apache-tomcat-8.5.15.tar.gz")) {
      systemCatalog = aSystemCatalog()
                          .withCatalogType(APPSTACK)
                          .withName("Standard Tomcat 8")
                          .withFileName("apache-tomcat-8.5.15.tar.gz")
                          .withAppId(Base.GLOBAL_APP_ID)
                          .withFamily(TOMCAT)
                          .withNotes("System created.")
                          .build();
      systemCatalogService.save(systemCatalog, AWS_S3_CATALOG_TOMCAT8, PLATFORMS, fileSize);
    }
    if (!catalognames.contains("apache-tomcat-8.5.15-hardened.tar.gz")) {
      systemCatalog = aSystemCatalog()
                          .withCatalogType(APPSTACK)
                          .withName("Hardened Tomcat 8")
                          .withFileName("apache-tomcat-8.5.15-hardened.tar.gz")
                          .withAppId(Base.GLOBAL_APP_ID)
                          .withFamily(TOMCAT)
                          .withNotes("System created. Hardened Version")
                          .build();
      systemCatalogService.save(systemCatalog, AWS_S3_CATALOG_TOMCAT8_HARDENED, PLATFORMS, fileSize);
    }
  }

  @Test
  public void createSystemAppContainers() {
    System.out.println("Creating System App Containers");
    List<Account> accounts =
        accountService.list(aPageRequest().withLimit(PageRequest.UNLIMITED).addFieldsIncluded("uuid").build());
    if (CollectionUtils.isEmpty(accounts)) {
      return;
    }
    List<SystemCatalog> systemCatalogs = systemCatalogService.list(
        aPageRequest().addFilter("catalogType", SearchFilter.Operator.EQ, SystemCatalog.CatalogType.APPSTACK).build());
    accounts.forEach(account -> {
      for (SystemCatalog systemCatalog : systemCatalogs) {
        AppContainer appContainer = anAppContainer()
                                        .withAccountId(account.getUuid())
                                        .withAppId(systemCatalog.getAppId())
                                        .withChecksum(systemCatalog.getChecksum())
                                        .withChecksumType(systemCatalog.getChecksumType())
                                        .withFamily(systemCatalog.getFamily())
                                        .withStackRootDirectory(systemCatalog.getStackRootDirectory())
                                        .withFileUuid(systemCatalog.getFileUuid())
                                        .withFileType(systemCatalog.getFileType())
                                        .withSize(systemCatalog.getSize())
                                        .withName(systemCatalog.getName())
                                        .withSystemCreated(true)
                                        .withDescription(systemCatalog.getNotes())
                                        .build();
        try {
          PageResponse<AppContainer> pageResponse =
              appContainerService.list(aPageRequest()
                                           .addFilter("accountId", EQ, account.getUuid())
                                           .addFilter("fileUuid", EQ, systemCatalog.getFileUuid())
                                           .build());
          if (CollectionUtils.isEmpty(pageResponse.getResponse())) {
            appContainerService.save(appContainer);
          }
        } catch (Exception e) {
          e.printStackTrace();
          System.out.println("Error while creating system app container " + appContainer);
        }
      }

    });
    System.out.println("System App Containers created successfully");
  }
}
