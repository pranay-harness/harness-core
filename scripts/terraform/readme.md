# Terraform 

### Get access
Get access to Harness-DevOps account by creating a devops ticket.

### Setting up locally
#### [Install CLI](https://learn.hashicorp.com/tutorials/terraform/install-cli)
#### Login

Once you have access to terraform, Use terraform login to login in CLI. 

https://www.terraform.io/docs/cli/commands/login.html

### Terraform commands
```
Go to /portal/scripts/terraform/stackdriver
terraform init # will read backend.tf file in the current directory.
terraform workspace list # to list workspaces.
terraform plan # no opp command to print diff between local directory and actual 
terraform state list
```
For local testing always set target to only try with updated file instead of applying all the changes.
```
terraform plan -target=module.cvng.google_monitoring_dashboard.cvng_tasks_dashboard
terraform apply -target=module.cvng.google_monitoring_dashboard.cvng_tasks_dashboard
```

### Google cloud monitoring 
* https://registry.terraform.io/providers/hashicorp/google/latest/docs/resources/monitoring_dashboard