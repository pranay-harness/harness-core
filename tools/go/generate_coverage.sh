#!/usr/bin/env bash
#
# How to Run:
# tools/go/generate_coverage.sh <type>
# type: [func, html]
#
# func: Generates function-wise coverage
# html: Generates HTML report and displays it on a browser
# THe combined coverage files are located in /tmp/symportal/coverage

set -euf -o pipefail

PROJECT_ROOT="${PROJECT_ROOT:-$(git rev-parse --show-toplevel)}"

echo "Moving to project root directory: $PROJECT_ROOT"
cd $PROJECT_ROOT

echo "Generating coverage using bazel... "
bazel coverage //...

echo "Removing any previous existing coverage directory... "
rm -rf /tmp/symportal

echo "Creating symlinked workspace for go tools to work .. "
mkdir -p /tmp/symportal/src/github.com/wings-software
mkdir -p /tmp/symportal/coverage
ln -s $PROJECT_ROOT /tmp/symportal/src/github.com/wings-software/portal
COVERAGE_OUT="/tmp/symportal/coverage/combined_coverage.out"
COVERAGE_HTML="/tmp/symportal/coverage/combined_coverage.html"

echo "Merging coverage reports ... "
if ! ((find $PROJECT_ROOT/bazel-testlogs/ -name coverage.dat | tr '\n' ' ' | xargs gocovmerge) | sed '/mock.go/d' >> $COVERAGE_OUT); then
	printf "\e[31mFailed to merge coverage dat files. Please make sure you have run portal/tools/go/go_setup.sh.\n"
	printf "\e[31mAlso ensure \$GOPATH/bin is added to \$PATH and contains the gocovmerge binary."
	exit
fi

echo "Adding symlinked path to GOPATH temporarily ... "
export GOPATH=$GOPATH:/tmp/symportal

if [ "$1" == "func" ]; then
	echo "Generating function-wise coverage... "
	go tool cover -func=$COVERAGE_OUT
elif [ "$1" == "html" ]; then
	echo "Generating HTML report... "
	go tool cover -html=$COVERAGE_OUT -o $COVERAGE_HTML
	go tool cover -html=$COVERAGE_OUT
else
	echo "Input must be one of [func, html]"
fi
