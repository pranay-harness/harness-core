#!/usr/bin/env bash

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

############################
##  Common Functions      ##
############################

function print_possible_alternates {
  POTENTIAL_ALTERNATE=$(find . -name "$(basename "$FILE")")
  if [ ! -z "$POTENTIAL_ALTERNATE" ]; then
    echo "Has the file been moved to         $POTENTIAL_ALTERNATE"
  fi
}

function create_output_file {
  cp -p "$FILE" "$NEW_FILE"
  : > "$NEW_FILE"
}

function add_header_if_required {
  if [ "$HEADER_WITHOUT_COMMENT_SYMBOL" = "$LICENSE_TEXT" ]; then
    return 0
  elif [ $(grep -m1 -ciE "(copyright|license)" <<<"$EXISTING_HEADER") -eq 1 ]; then
    echo "Skipping file as it already has a different license header $FILE"
  else
    $1
  fi
}

function write_file_header {
  while read license_line; do
    if [ -z "$license_line" ]; then
      echo "$SYMBOL" >> "$NEW_FILE"
    else
      echo "$SYMBOL $license_line" >> "$NEW_FILE"
    fi
  done <<<"$LICENSE_TEXT"
}

function write_remaining_file_content {
  if [ ! -z "$(head -1 <<<"$FILE_CONTENT")" ]; then
    echo >> "$NEW_FILE"
  fi
  echo "$FILE_CONTENT" >> "$NEW_FILE"
  mv "$NEW_FILE" "$FILE"
}

############################
##  Double Slash          ##
############################

function handle_double_slash {
  EXISTING_HEADER=$(read_header_double_slash)
  if [ -z "$EXISTING_HEADER" ]; then
    write_file_double_slash
  else
    HEADER_WITHOUT_COMMENT_SYMBOL=$(cut -c 4- <<<"$EXISTING_HEADER")
    add_header_if_required "write_file_double_slash"
  fi
}

function read_header_double_slash {
  awk '{ if (/^\/\//) {print} else {exit} }' "$FILE"
}

function write_file_double_slash {
  if [ "$DRY_RUN" != "true" ]; then
    NEW_FILE="$FILE.new"
    SYMBOL="//"
    create_output_file
    write_file_header
    write_remaining_file_content
  fi
}

############################
##  Slash Star            ##
############################

function handle_slash_star {
  EXISTING_HEADER=$(read_header_slash_star)
  if [ -z "$EXISTING_HEADER" ]; then
    write_file_slash_star
  else
    HEADER_WITHOUT_COMMENT_SYMBOL=$(cut -c 4- <<<"$EXISTING_HEADER" | awk 'NR > 1')
    add_header_if_required "write_file_slash_star"
  fi
}

function read_header_slash_star {
  awk '{ if (/(\/\*| \*)/) {print} else {exit} }' "$FILE"
}

function write_file_slash_star {
  if [ "$DRY_RUN" != "true" ]; then
    NEW_FILE="$FILE.new"
    SYMBOL=" *"
    create_output_file
    echo "/*" > "$NEW_FILE"
    write_file_header
    echo " */" >> "$NEW_FILE"
    write_remaining_file_content
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
    write_file_hash
  else
    HEADER_WITHOUT_COMMENT_SYMBOL=$(cut -c 3- <<<"$EXISTING_HEADER")
    add_header_if_required "write_file_hash"
  fi
}

function read_header_hash {
  awk '{ if (/^#/) {print} else {exit} }' "$FILE"
}

function write_file_hash {
  if [ "$DRY_RUN" != "true" ]; then
    NEW_FILE="$FILE.new"
    SYMBOL="#"
    create_output_file
    if [ "$IS_MISSING_SHE_BANG" != "TRUE" ]; then
      head -1 <<<"$FILE_CONTENT" > "$NEW_FILE"
    fi
    write_file_header
    if [ "$IS_MISSING_SHE_BANG" != "TRUE" ]; then
      FILE_CONTENT=$(echo "$FILE_CONTENT" | awk "NR > 1")
    fi
    write_remaining_file_content
  fi
}

############################
##  Double Hyphen         ##
############################

function handle_double_hyphen {
  EXISTING_HEADER=$(read_header_double_hyphen)
  if [ -z "$EXISTING_HEADER" ]; then
    write_file_double_hyphen
  else
    HEADER_WITHOUT_COMMENT_SYMBOL=$(cut -c 4- <<<"$EXISTING_HEADER")
    add_header_if_required "write_file_double_hyphen"
  fi
}

function read_header_double_hyphen {
  awk '{ if (/^--/) {print} else {exit} }' "$FILE"
}

function write_file_double_hyphen {
  if [ "$DRY_RUN" != "true" ]; then
    NEW_FILE="$FILE.new"
    SYMBOL="--"
    create_output_file
    write_file_header
    write_remaining_file_content
  fi
}

############################
##  Execution             ##
############################

LICENSE_TEXT=$(cat $PATH_TO_LICENSE)
SOURCE_FILES=$(cat $PATH_TO_INPUT)
PREVIOUSLY_OVERWRITTEN_HEADER=""

while read -r FILE; do
  if [ ! -e "$FILE" ]; then
    echo "Skipping file as it does not exist $FILE"
    print_possible_alternates
    echo
    continue
  elif [ -d "$FILE" ]; then
    echo "Skipping directory, only files are supported $FILE"
    continue
  elif [ ! -w "$FILE" ]; then
    echo "Skipping file as it is not writable $FILE"
    continue
  fi

  FILE_TYPE=$(awk '{gsub(/.*\//, ""); gsub(/.*\./, ""); print}' <<<"$FILE")
  FILE_CONTENT=$(cat "$FILE")
  if [ "$FILE_TYPE" = "cjs" ]; then # Common JavaScript
    handle_slash_star
  elif [ "$FILE_TYPE" = "css" ]; then
    handle_slash_star
  elif [ "$FILE_TYPE" = "go" ]; then
    handle_double_slash
  elif [ "$FILE_TYPE" = "groovy" ]; then
    handle_slash_star
  elif [ "$FILE_TYPE" = "java" ]; then
    handle_slash_star
  elif [ "$FILE_TYPE" = "js" ]; then
    handle_slash_star
  elif [ "$FILE_TYPE" = "jsx" ]; then
    handle_slash_star
  elif [ "$FILE_TYPE" = "mjs" ]; then # JavaScript
    handle_slash_star
  elif [ "$FILE_TYPE" = "pl" ]; then # Perl
    handle_hash
  elif [ "$FILE_TYPE" = "proto" ]; then
    handle_double_slash
  elif [ "$FILE_TYPE" = "py" ]; then # Python
    handle_hash
  elif [ "$FILE_TYPE" = "rs" ]; then # Rust
    handle_double_slash
  elif [ "$FILE_TYPE" = "scss" ]; then
    handle_slash_star
  elif [ "$FILE_TYPE" = "sh" ]; then
    handle_hash
  elif [ "$FILE_TYPE" = "sql" ]; then
    handle_double_hyphen
  elif [ "$FILE_TYPE" = "ts" ]; then
    handle_slash_star
  elif [ "$FILE_TYPE" = "tsx" ]; then
    handle_slash_star
  else
    echo "Skipping file with extension '$FILE_TYPE' as it is not a supported filetype, file is $FILE"
  fi
done <<< "$SOURCE_FILES"
