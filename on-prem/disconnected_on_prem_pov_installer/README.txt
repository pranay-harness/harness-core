Installation instructions

********First time install*******
    Prerequisites
        a) Network connectivity to DockerHub

1) Run the first_time_only_install_harness.sh as sudo
2) This script will prepopulate the installer with a Mongo docker image that is to required for Harness and will generate a harness_disconnected_on_prem_pov_final.zip file
3) The harness_disconnected_on_prem_pov_final.zip should be copied over to the remote machine on the customer's internal network where Harness will run
4) On the remote machine, set the required information for
    a) accountdetails.properties : Set the AccountName, CompanyName and the initial email address
    b) inframapping.properties : Set the Host IP address.
    c) config.properties : Set the runtime_dir : Give an ABSOLUTE path where Harness will store its local files
    d) Run the install_harness.sh as sudo user
    e) Navigate to the URL as printed at the end of the install


*********Upgrade**************
1) Unzip the harness_installer.zip
2) Run the upgrade_harness.sh which will generate a harness_disconnected_on_prem_pov_final.zip file
3) The harness_disconnected_on_prem_pov_final.zip should be copied over to the remote machine on the customer's internal network where Harness is installed
4) Unzip the harness_disconnected_on_prem_pov_final.zip.
5) Pass the runtime directory as an argument to the install_harness.sh command eg: sudo ./install_harness.sh /home/ubuntu/harness_runtime
6) Run the install_harness.sh as sudo user. The install_harness.sh will upgrade the existing setup