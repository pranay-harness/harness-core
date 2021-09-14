#!/usr/bin/env bash

# Copyright 2021 Harness Inc.
# 
# Licensed under the Apache License, Version 2.0
# http://www.apache.org/licenses/LICENSE-2.0

##########################################################################################

# Run this script to download the required Go tools for Go development

##########################################################################################
set -euo pipefail
PROJECT_ROOT="${PROJECT_ROOT:-$(git rev-parse --show-toplevel)}"

echo $PROJECT_ROOT
cd $PROJECT_ROOT
go get -u github.com/mdempsky/gocode github.com/uudashr/gopkgs/v2/cmd/gopkgs github.com/ramya-rao-a/go-outline github.com/acroca/go-symbols golang.org/x/tools/cmd/guru golang.org/x/tools/cmd/gorename github.com/cweill/gotests github.com/fatih/gomodifytags github.com/josharian/impl github.com/davidrjenni/reftools/cmd/fillstruct github.com/haya14busa/goplay/cmd/goplay github.com/godoctor/godoctor github.com/stamblerre/gocode github.com/rogpeppe/godef github.com/sqs/goreturns golang.org/x/lint/golint github.com/golang/mock/gomock golang.org/x/tools/go/packages github.com/wadey/gocovmerge github.com/bazelbuild/bazel-gazelle/cmd/gazelle

go install github.com/golang/mock/mockgen
