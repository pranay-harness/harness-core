package software.wings.generator;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ServiceTemplateService;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class OwnerManager {
  @Inject private AccountService accountService;
  @Inject private AppService applicationService;
  @Inject private ServiceTemplateService serviceTemplateService;

  public Owners create() {
    final Owners owners = new Owners();
    owners.manager = this;
    return owners;
  }

  public static class Owners {
    private OwnerManager manager;
    private List<Object> objects = new ArrayList();

    public void add(Object owner) {
      if (owner != null) {
        objects.add(owner);
      }
    }

    public Account obtainAccount() {
      Account account =
          objects.stream().filter(obj -> obj instanceof Account).findFirst().map(obj -> (Account) obj).orElse(null);

      if (account == null) {
        Application application = obtainApplication();
        if (application != null) {
          account = manager.accountService.get(application.getAccountId());
          add(account);
        }
      }

      return account;
    }

    public Application obtainApplication() {
      Application application = objects.stream()
                                    .filter(obj -> obj instanceof Application)
                                    .findFirst()
                                    .map(obj -> (Application) obj)
                                    .orElse(null);

      if (application == null) {
        final Environment environment = obtainEnvironment();
        if (environment != null) {
          application = manager.applicationService.get(environment.getAppId());
          add(application);
        }
      }

      if (application == null) {
        final Service service = obtainService();
        if (service != null) {
          application = manager.applicationService.get(service.getAppId());
          add(application);
        }
      }

      if (application == null) {
        final InfrastructureProvisioner infrastructureProvisioner = obtainInfrastructureProvisioner();
        if (infrastructureProvisioner != null) {
          application = manager.applicationService.get(infrastructureProvisioner.getAppId());
          add(application);
        }
      }

      return application;
    }

    public Environment obtainEnvironment() {
      return objects.stream()
          .filter(obj -> obj instanceof Environment)
          .findFirst()
          .map(obj -> (Environment) obj)
          .orElse(null);
    }

    public Service obtainService() {
      return objects.stream().filter(obj -> obj instanceof Service).findFirst().map(obj -> (Service) obj).orElse(null);
    }

    public ServiceTemplate obtainServiceTemplate() {
      ServiceTemplate serviceTemplate = objects.stream()
                                            .filter(obj -> obj instanceof ServiceTemplate)
                                            .findFirst()
                                            .map(obj -> (ServiceTemplate) obj)
                                            .orElse(null);
      if (serviceTemplate == null) {
        Application application = obtainApplication();
        Environment environment = obtainEnvironment();
        Service service = obtainService();

        if (application != null && environment != null && service != null) {
          serviceTemplate =
              manager.serviceTemplateService.get(application.getUuid(), service.getUuid(), environment.getUuid());
        }
      }
      return serviceTemplate;
    }

    public InfrastructureProvisioner obtainInfrastructureProvisioner() {
      InfrastructureProvisioner infrastructureProvisioner = objects.stream()
                                                                .filter(obj -> obj instanceof InfrastructureProvisioner)
                                                                .findFirst()
                                                                .map(obj -> (InfrastructureProvisioner) obj)
                                                                .orElse(null);
      return infrastructureProvisioner;
    }
  }
}
