Installation instructions

********Install*******

1) The harness_installer_version.tar.gz file should be downloaded/copied over to the remote machine on the customer's internal network where Harness will be installed. ex: harness_installer_51107.tar.gz
2) Harness installation requires docker to be installed and docker daemon service to be running on machine where Harness will be installed.
3) Open a terminal and extract the contents of harness_installer_version.tar.gz file by executing tar -xvf harness_installer_version.tar.gz
4) Navigate to the harness_installer folder that was created after executing the above mentioned tar command.
4) Open inframapping.properties in a text editor, and edit the HOST1_IP_ADDRESS property. Enter the external, public IP address of the host that will run Harness.
5) Navigate to harness_installer/pre_install_scripts folder
6) Run setup_mongo_directories.sh as sudo user.  Sudo user is required (for chown command) as this script create appropriate permissions for mongo directories.
   (You can pass in the runtime directory as an argument to set where the MongoDB directories are installed.
   Without the argument, the script defaults to the $HOME directory, and installs the new directories within $HOME/harness_runtime.)
7) You can now install Harness as a sudo user or non-sudo user.
8) Navigate to the harness_installer folder and run install_harness.sh.
   (You can pass in the runtime directory as an argument to set where Harness is installed. Without the argument, the script defaults to the $HOME directory and Harness is installed in $HOME/harness_runtime.)
8) Upon successful completion, the install_harness.sh script will start Harness micro-services docker containers and setup up seed data in MongoDB.
   The script will output a URL, using the public IP address you provided in inframapping.properties.
   Navigate to the displayed URL. This is the Harness Manager login URL.
9) Log into Harness using the onprem-signup URL. For example: http://<IP_address>:7143/#/onprem-signup.
10) Create a new account by filling up the sign-up form
11) After successful sign-up, try logging into Harness  http://<IP_address>:7143/#/login
12) Start setting up Harness by downloading and installing a Harness Delegate.


********* Upgrade **************
1) When you receive an upgrade to Harness Disconnected On-Prem, extract the harness_installer_version.tar.gz file.
2) Open the harness_installer folder in a terminal.
3) Make sure you current user is same user that installed Harness. Refer to step 7) in Install steps.
4) Run the install_harness.sh script.
   If you specified a runtime directory when you first installed Harness, ensure you use the same runtime directory as an argument when running install_harness.sh.
   If no argument was provided during the first time installation, there is no need to pass an argument while upgrading.
   The installer will use the default runtime directory location of $HOME/harness_runtime where it was first installed.
5) Navigate to the URL displayed by the install_harness.sh script. This is the Harness Manager login.
6) Log in to Harness.


For additional details pls refer to our docs https://docs.harness.io/article/oycugk29oy-harness-disconnected-on-premise-setup