#!/usr/bin/env bash
# Copyright Harness.io
# 
# Licensed under the Harness Example License.
# 
# Full license can be found at
# https://harness.io/license.txt
# 
# By accepting this license you become the legal property of Harness Inc.

function usage {
  echo "Script to add license header to files"
  echo
  echo "./$(basename $0) -f <file> -l <file>"
  echo "Options"
  echo "  -h        prints usage"
  echo "  -d        dry run"
  echo "  -l <file> path to file containing license text"
  echo "  -f <file> path to file containing source files to process"
}

function error {
  usage
  echo
  echo "$1"
}

function error_missing_file {
  error "ERROR: $1 file is required!"
  exit 3
}

function error_cannot_read_file {
  error "ERROR: Cannot open file for reading $1"
  exit 4
}

while getopts ":df:hl:" arg; do
  case ${arg} in
    d) DRY_RUN="true" ;;
    f) PATH_TO_INPUT=${OPTARG} ;;
    h) usage ; exit 0 ;;
    l) PATH_TO_LICENSE=${OPTARG} ;;
    :)
       echo "$0: Must supply an argument to -$OPTARG." >&2
       exit 1 ;;
    ?)
       echo "Invalid option: -${OPTARG}."
       exit 2 ;;
  esac
done

if [ -z $PATH_TO_LICENSE ]; then
  error_missing_file "License"
fi

if [ -z $PATH_TO_INPUT ]; then
  error_missing_file "Source"
fi

if [ ! -r $PATH_TO_LICENSE ]; then
  error_cannot_read_file "$PATH_TO_LICENSE"
fi

if [ ! -r $PATH_TO_INPUT ]; then
  error_cannot_read_file "$PATH_TO_INPUT"
fi

function add_to_unprocessed_files {
  if [ "$DRY_RUN" != "true" ]; then
    echo "$FILE" >> $UNPROCESSED_FILES
  fi
}

############################
##  Double Slash          ##
############################

function handle_double_slash {
  EXISTING_HEADER=$(read_header_double_slash)
  if [ -z "$EXISTING_HEADER" ]; then
    add_header_double_slash
  else
    replace_header_double_slash
  fi
}

function read_header_double_slash {
  awk '{ if (/^\/\//) {print} else {exit} }' $FILE
}

function add_header_double_slash {
  echo "Adding license header to $FILE"
  FILE_CONTENT=$(cat $FILE)
  write_file_header_and_content_double_slash
}

function replace_header_double_slash {
  HEADER_WITHOUT_COMMENT_LITERAL=$(cut -c 4- <<<"$EXISTING_HEADER")
  if [ "$HEADER_WITHOUT_COMMENT_LITERAL" = "$LICENSE_TEXT" ]; then
    echo "Skipping file as it already has the correct header $FILE"
  elif [ $(grep -m1 -ciE "(copyright|license)" <<<"$EXISTING_HEADER") -eq 1 ]; then
    echo "Skipping file as it already has a different license header $FILE"
    add_to_unprocessed_files
  else
    add_header_double_slash
  fi
}

function write_file_header_and_content_double_slash {
  if [ "$DRY_RUN" != "true" ]; then
    NEW_FILE="$FILE.new"
    while read license_line; do
      echo "// $license_line" >> $NEW_FILE
    done <<<"$LICENSE_TEXT"
    echo >> $NEW_FILE
    echo "$FILE_CONTENT" >> $NEW_FILE
    mv $NEW_FILE $FILE
  fi
}

############################
##  Slash Star            ##
############################

function handle_slash_star {
  EXISTING_HEADER=$(read_header_slash_star)
  if [ -z "$EXISTING_HEADER" ]; then
    add_header_slash_star
  else
    replace_header_slash_star
  fi
}

function read_header_slash_star {
  awk '{ if (/(\/\*| \*)/) {print} else {exit} }' $FILE
}

function add_header_slash_star {
  echo "Adding license header to $FILE"
  FILE_CONTENT=$(cat $FILE)
  write_file_header_and_content_slash_star
}

function replace_header_slash_star {
  HEADER_WITHOUT_COMMENT_LITERAL=$(cut -c 4- <<<"$EXISTING_HEADER" | awk 'NR > 1 && NR < 10')
  if [ "$HEADER_WITHOUT_COMMENT_LITERAL" = "$LICENSE_TEXT" ]; then
    echo "Skipping file as it already has the correct header $FILE"
  elif [ $(grep -m1 -ciE "(copyright|license)" <<<"$EXISTING_HEADER") -eq 1 ]; then
    echo "Skipping file as it already has a different license header $FILE"
    add_to_unprocessed_files
  else
    add_header_slash_star
  fi
}

function write_file_header_and_content_slash_star {
  if [ "$DRY_RUN" != "true" ]; then
    NEW_FILE="$FILE.new"
    echo "/*" > $NEW_FILE
    while read license_line; do
      echo " * $license_line" >> $NEW_FILE
    done <<<"$LICENSE_TEXT"
    echo " */" >> $NEW_FILE
    echo >> $NEW_FILE
    echo "$FILE_CONTENT" >> $NEW_FILE
    mv $NEW_FILE $FILE
  fi
}

############################
##  Hash                  ##
############################

function handle_hash {
  RAW_HEADER=$(read_header_hash)
  EXISTING_HEADER=$(grep -v "^#!" <<<"$RAW_HEADER")
  IS_MISSING_SHE_BANG=$(test "$RAW_HEADER" = "$EXISTING_HEADER" && echo "TRUE")

  if [ -z "$EXISTING_HEADER" ]; then
    add_header_hash
  else
    replace_header_hash
  fi
}

function read_header_hash {
  awk '{ if (/^#/) {print} else {exit} }' $FILE
}

function add_header_hash {
  echo "Adding license header to $FILE"
  FILE_CONTENT=$(cat $FILE)
  write_file_header_and_content_hash
}

function replace_header_hash {
  HEADER_WITHOUT_COMMENT_LITERAL=$(cut -c 3- <<<"$EXISTING_HEADER")
  if [ "$HEADER_WITHOUT_COMMENT_LITERAL" = "$LICENSE_TEXT" ]; then
    echo "Skipping file as it already has the correct header $FILE"
  elif [ $(grep -m1 -ciE "(copyright|license)" <<<"$EXISTING_HEADER") -eq 1 ]; then
    echo "Skipping file as it already has a different license header $FILE"
    add_to_unprocessed_files
  else
    add_header_hash
  fi
}

function write_file_header_and_content_hash {
  if [ "$DRY_RUN" != "true" ]; then
    NEW_FILE="$FILE.new"
    if [ "$IS_MISSING_SHE_BANG" != "TRUE" ]; then
      head -1 <<<"$FILE_CONTENT" > $NEW_FILE
    fi
    while read license_line; do
      echo "# $license_line" >> $NEW_FILE
    done <<<"$LICENSE_TEXT"
    echo >> $NEW_FILE
    if [ "$IS_MISSING_SHE_BANG" != "TRUE" ]; then
      echo "$FILE_CONTENT" | awk "NR > 1" >> $NEW_FILE
    else
      echo "$FILE_CONTENT" >> $NEW_FILE
    fi
    mv $NEW_FILE $FILE
  fi
}

############################
##  Double Hyphen         ##
############################

function handle_double_hyphen {
  EXISTING_HEADER=$(read_header_double_hyphen)
  if [ -z "$EXISTING_HEADER" ]; then
    add_header_double_hyphen
  else
    replace_header_double_hyphen
  fi
}

function read_header_double_hyphen {
  awk '{ if (/^--/) {print} else {exit} }' $FILE
}

function add_header_double_hyphen {
  echo "Adding license header to $FILE"
  FILE_CONTENT=$(cat $FILE)
  write_file_header_and_content_double_hyphen
}

function replace_header_double_hyphen {
  HEADER_WITHOUT_COMMENT_LITERAL=$(cut -c 4- <<<"$EXISTING_HEADER")
  if [ "$HEADER_WITHOUT_COMMENT_LITERAL" = "$LICENSE_TEXT" ]; then
    echo "Skipping file as it already has the correct header $FILE"
  elif [ $(grep -m1 -ciE "(copyright|license)" <<<"$EXISTING_HEADER") -eq 1 ]; then
    echo "Skipping file as it already has a different license header $FILE"
    add_to_unprocessed_files
  else
    add_header_double_hyphen
  fi
}

function write_file_header_and_content_double_hyphen {
  if [ "$DRY_RUN" != "true" ]; then
    NEW_FILE="$FILE.new"
    while read license_line; do
      echo "-- $license_line" >> $NEW_FILE
    done <<<"$LICENSE_TEXT"
    echo >> $NEW_FILE
    echo "$FILE_CONTENT" >> $NEW_FILE
    mv $NEW_FILE $FILE
  fi
}

############################
##  Execution             ##
############################

LICENSE_TEXT=$(cat $PATH_TO_LICENSE)
SOURCE_FILES=$(cat $PATH_TO_INPUT)
PREVIOUSLY_OVERWRITTEN_HEADER=""
UNPROCESSED_FILES="UNPROCESSED_FILES.txt"
if [ "$DRY_RUN" != "true" ]; then
  printf "" > $UNPROCESSED_FILES
fi

for FILE in $(xargs <<<$SOURCE_FILES); do
  if [ ! -f "$FILE" ]; then
    echo "Skipping file as it does not exist $FILE"
    add_to_unprocessed_files
    continue
  elif [ ! -w "$FILE" ]; then
    echo "Skipping file as it is not writable $FILE"
    add_to_unprocessed_files
    continue
  fi

  FILE_TYPE=$(awk '{gsub(/.*\//, ""); gsub(/.*\./, ""); print}' <<<"$FILE")
  if [ "$FILE_TYPE" = "go" ]; then
    handle_double_slash
  elif [ "$FILE_TYPE" = "groovy" ]; then
    handle_slash_star
  elif [ "$FILE_TYPE" = "java" ]; then
    handle_slash_star
  elif [ "$FILE_TYPE" = "js" ]; then
    handle_slash_star
  elif [ "$FILE_TYPE" = "pl" ]; then # Perl
    handle_hash
  elif [ "$FILE_TYPE" = "py" ]; then # Python
    handle_hash
  elif [ "$FILE_TYPE" = "rs" ]; then # Rust
    handle_double_slash
  elif [ "$FILE_TYPE" = "sh" ]; then
    handle_hash
  elif [ "$FILE_TYPE" = "sql" ]; then
    handle_double_hyphen
  else
    echo "Skipping file with extension '$FILE_TYPE' as it is not a supported filetype, file is $FILE"
    add_to_unprocessed_files
  fi
done
