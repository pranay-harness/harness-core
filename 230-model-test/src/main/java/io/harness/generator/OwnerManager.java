/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.generator;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static java.util.stream.Collectors.toList;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.EmbeddedUser;

import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.InfrastructureProvisioner;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.Workflow;
import software.wings.beans.container.ContainerTask;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ServiceTemplateService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;

@OwnedBy(CDP)
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
      if (objects.contains(owner)) {
        return;
      }
      if (owner != null) {
        objects.add(owner);
      }
    }

    public void clear(Class clz) {
      objects = objects.stream().filter(obj -> obj.getClass().equals(clz)).collect(toList());
    }

    public EmbeddedUser obtainUser() {
      return objects.stream()
          .filter(obj -> obj instanceof EmbeddedUser)
          .findFirst()
          .map(obj -> (EmbeddedUser) obj)
          .orElse(null);
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

    public Account obtainAccount(Generator<Account> generator) {
      Account account = obtainAccount();
      if (account != null) {
        return account;
      }

      account = generator.generate();
      if (account != null) {
        add(account);
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

    public interface Generator<T> {
      T generate();
    }

    public Application obtainApplication(Generator<Application> generator) {
      Application application = obtainApplication();
      if (application != null) {
        return application;
      }

      application = generator.generate();
      if (application != null) {
        add(application);
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

    public Environment obtainEnvironment(Generator<Environment> generator) {
      Environment environment = obtainEnvironment();
      if (environment != null) {
        return environment;
      }

      environment = generator.generate();
      if (environment != null) {
        add(environment);
      }

      return environment;
    }

    public Service obtainService() {
      return objects.stream().filter(obj -> obj instanceof Service).findFirst().map(obj -> (Service) obj).orElse(null);
    }

    public Service obtainServiceByServiceName(String serviceName) {
      return objects.stream()
          .filter(obj -> obj instanceof Service)
          .filter(obj -> ((Service) obj).getName().equals(serviceName))
          .findFirst()
          .map(obj -> (Service) obj)
          .orElse(null);
    }

    public Service obtainServiceByServiceId(String serviceId) {
      return objects.stream()
          .filter(obj -> obj instanceof Service)
          .filter(obj -> ((Service) obj).getUuid().equals(serviceId))
          .findFirst()
          .map(obj -> (Service) obj)
          .orElse(null);
    }

    public List<String> obtainAllServiceIds() {
      List<String> serviceIds = new ArrayList<>();
      objects.stream().filter(obj -> obj instanceof Service).forEach(obj -> serviceIds.add(((Service) obj).getUuid()));
      return serviceIds;
    }

    public Service obtainService(Generator<Service> generator) {
      Service service = obtainService();
      if (service != null) {
        return service;
      }

      service = generator.generate();
      if (service != null) {
        add(service);
      }

      return service;
    }

    public ServiceTemplate obtainServiceTemplate() {
      obtainService();
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

    public ContainerTask obtainContainerTask() {
      return objects.stream()
          .filter(obj -> obj instanceof ContainerTask)
          .findFirst()
          .map(obj -> (ContainerTask) obj)
          .orElse(null);
    }

    public InfrastructureProvisioner obtainInfrastructureProvisioner() {
      return objects.stream()
          .filter(obj -> obj instanceof InfrastructureProvisioner)
          .findFirst()
          .map(obj -> (InfrastructureProvisioner) obj)
          .orElse(null);
    }

    public InfrastructureDefinition obtainInfrastructureDefinition() {
      return (InfrastructureDefinition) objects.stream()
          .filter(obj -> obj instanceof InfrastructureDefinition)
          .findFirst()
          .orElse(null);
    }

    public InfrastructureDefinition obtainInfrastructureDefinition(Generator<InfrastructureDefinition> generator) {
      InfrastructureDefinition infraDefinition = obtainInfrastructureDefinition();
      if (infraDefinition != null) {
        return infraDefinition;
      }

      infraDefinition = generator.generate();
      if (infraDefinition != null) {
        add(infraDefinition);
      }

      return infraDefinition;
    }

    public Workflow obtainWorkflow() {
      return (Workflow) objects.stream().filter(obj -> obj instanceof Workflow).findFirst().orElse(null);
    }
  }
}
