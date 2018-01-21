#!/bin/sh
#
#This pre - commit hook checks if any versions of clang - format
#are installed, and if so, uses the installed version to format
#the staged changes.

pushd `dirname $0` > /dev/null && cd ../.. && BASEDIR=$(pwd -L) && popd > /dev/null

CHECKSTYLE_PROPERTY=hook.pre-push.stylecheck
if [ "`git config $CHECKSTYLE_PROPERTY`" == "false" ]
then
    echo '\033[0;31m' checking style is disabled - to enable: '\033[0;37m'git config --unset $CHECKSTYLE_PROPERTY '\033[0m'
else
    echo '\033[0;34m' checking style  ... to disable: '\033[0;37m'git config --add $CHECKSTYLE_PROPERTY false '\033[0m'

    . $BASEDIR/toolset/git-hooks/checkstyle.sh
fi