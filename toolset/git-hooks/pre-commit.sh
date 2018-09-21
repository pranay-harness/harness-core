#!/bin/sh
#
#This pre - commit hook checks if any versions of clang - format
#are installed, and if so, uses the installed version to format
#the staged changes.

pushd `dirname $0` > /dev/null && cd ../.. && BASEDIR=$(pwd -L) && popd > /dev/null

CHECK_CONFLICTS=hook.pre-commit.check-conflicts
if [ "`git config $CHECK_CONFLICTS`" == "false" ]
then
    echo '\033[0;31m' checking left conflicts is disabled - to enable: '\033[0;37m'git config --unset $CHECK_CONFLICTS '\033[0m'
else
    echo '\033[0;34m' checking left conflicts  ... to disable: '\033[0;37m'git config --add $CHECK_CONFLICTS false '\033[0m'

    . $BASEDIR/toolset/git-hooks/check_conflicts.sh
fi

CHECKSTYLE_PROPERTY=hook.pre-commit.stylecheck
if [ "`git config $CHECKSTYLE_PROPERTY`" == "false" ]
then
    echo '\033[0;31m' checking style is disabled - to enable: '\033[0;37m'git config --unset $CHECKSTYLE_PROPERTY '\033[0m'
else
    echo '\033[0;34m' checking style  ... to disable: '\033[0;37m'git config --add $CHECKSTYLE_PROPERTY false '\033[0m'

    . $BASEDIR/toolset/git-hooks/checkstyle.sh
fi

FORMAT_PROPERTY=hook.pre-commit.format
if [ "`git config $FORMAT_PROPERTY`" == "false" ]
then
    echo '\033[0;31m' formatting is disabled - to enable: '\033[0;37m'git config --unset $FORMAT_PROPERTY '\033[0m'
else
    echo '\033[0;34m' formatting  ... to disable: '\033[0;37m'git config --add $FORMAT_PROPERTY false '\033[0m'

    if git rev-parse --verify HEAD >/dev/null 2>&1
    then
        against=HEAD
    else
        #Initial commit : diff against an empty tree object
        against=4b825dc642cb6eb9a060e54bf8d69288fbee4904
    fi

    #do the formatting
    for file in `git diff-index --cached --name-only $against | grep "\.java$"`
    do
        if [ -e "${file}" ]
        then
            clang-format -i "${file}"
            git diff --exit-code -- "${file}"
            if [ "$?" -ne "0" ]
            then
                git add "${file}"
            fi
        fi
    done
fi