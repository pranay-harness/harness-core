Installation instructions

********Install*******

1) The harness_installer.tar.gz should be copied over to the remote machine on the customer's internal network where Harness will run
2) On the remote machine, set the required information for
    a) accountdetails.properties : Set the AccountName, CompanyName and the initial email address
    b) inframapping.properties : Set the Host IP address.
    d) Run the install_harness.sh as sudo user. You can pass in the runtime directory as an argument where harness will
    install, else it will default to the user's $HOME directory and will be installed in $HOME/harness_runtime
    e) Navigate to the URL as printed at the end of the install


********* Upgrade **************
1) Extract the harness_installer.tar.gz
2) Run the install_harness.sh as sudo user.Make sure to use the same runtime directory as an argument where harness was installed.
   If no argument was provided during the first time installation, there is no need to pass an argument while upgrading. It will
   use the default runtime directory location of $HOME/harness_runtime directory where it was first installed.
3) The install_harness.sh will upgrade the existing setup
