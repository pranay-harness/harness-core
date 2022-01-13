#!/usr/bin/perl
# Copyright 2020 Harness Inc. All rights reserved.
# Use of this source code is governed by the PolyForm Shield 1.0.0 license
# that can be found in the licenses directory at the root of this repository, also available at
# https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.

use Config::Properties;

use Time::HiRes qw(usleep);

use Cwd qw(chdir);

sub logmsg {
  print (scalar localtime() . " @_\n");
}

sub logerr {
  print STDERR (scalar localtime() . "ERROR: @_\n");
}

sub debug {
  if ($debug) {
    print "DEBUG: @_\n";
  }
}
logmsg "Initiating the Execution \n";

my ($portalURL, $releaseId, $artifactId) = @ARGV;

if ( not defined $portalURL or not defined $releaseId or not defined $artifactId) {
	die "Some of the parameters are missing - parameters passed: @ARGV" 
} else {
	logmsg "Parameters received - portalURL:$portalURL, releaseId:$releaseId, artifactId:$artifactId";
}

$app_config=".app.config";

logmsg "Downloading the configuration";
system "curl -sk -o $app_config $portalURL";

if (-e $app_config) {
	logmsg "Application Config file is downloaded";
}

# reading...

  open my $fh, '<', $app_config
    or die "unable to open app config file";

  my $properties = Config::Properties->new();
  $properties->load($fh);

$app_config_prefix="__app.";

$runtime_path=$properties->getProperty("${app_config_prefix}runtime_path");
$install_path=$properties->getProperty("${app_config_prefix}install_path");
$download_path=$properties->getProperty("${app_config_prefix}download_path");
$startup_path=$properties->getProperty("${app_config_prefix}startup_path");
$shutdown_path=$properties->getProperty("${app_config_prefix}shutdown_path");
$artifact_url=$properties->getProperty("${app_config_prefix}artifact_url");
$artifact_filename=$properties->getProperty("${app_config_prefix}artifact_filename");
$artifact_extn=$properties->getProperty("${app_config_prefix}artifact_extn");

logmsg "runtime_path : $runtime_path";
logmsg "startup_path : $startup_path";
logmsg "shutdown_path : $shutdown_path";
logmsg "artifact_url : $artifact_url";
logmsg "artifact_filename : $artifact_filename";
logmsg "artifact_extn : $artifact_extn";

  close($fh)
       || warn "close failed: $!";


use Time::HiRes qw(gettimeofday); 
my $now = gettimeofday;
logmsg "CurrentTimestamp : $now\n";
$install_path="${install_path}\/${now}";
$download_path="${download_path}\/${now}";

logmsg "install_path : $install_path";
logmsg "download_path : $download_path";

logmsg "install_path : $install_path\n";
system "mkdir -p $download_path";
system "mkdir -p $install_path";

logmsg "download and install directory got created\n";

chdir($download_path) or die "unable to chdir to $download_path :: exit code=$!";
logmsg "Trying to download artifact - artifact_url : $artifact_url";
system "curl -sk -o $artifact_filename $artifact_url";# or die 'unable to download $artifact_url :: exit code : $!';

if (-e $artifact_filename) {
	logmsg "Application artifact is downloaded";
}

chdir($install_path) or die "unable to chdir to $install_path :: exit code=$!";

if ($artifact_extn == 'TAR') {
	system "tar xvf $download_path/$artifact_filename"; # or die 'unable to unarchive the artifact $download_path/$artifact_filename :: exit code : $!';
}

if (-e $runtime_path) {
	logmsg "Runtimepath already exists ... moving to backup";
	system "mv $runtime_path ${runtime_path}_${now}"
}


system "mkdir -p $runtime_path";
chdir($runtime_path) or die "unable to chdir to $runtime_path :: exit code=$!";

logmsg "Trying to rsynch the install to runtime path";
system "rsync -az $install_path/* $runtime_path"; # or die "Unable to rsync to runtime path $runtime_path :: exit code=$!";

if (-e "$runtime_path/$startup_path") {
	logmsg "Found the application startup script : $runtime_path/$startup_path";	
} else {
	die "Startup script $runtime_path/$startup_path not found";	
}

system "chmod a+x $runtime_path/$startup_path"; # or die "unable to provide execute permission";

system "nohup $runtime_path/$startup_path"; # or die "could not launch the startup script successfully";

logmsg "Deployment completed";

 
