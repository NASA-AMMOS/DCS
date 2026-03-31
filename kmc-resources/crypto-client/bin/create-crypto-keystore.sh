#!/bin/bash
#
# This script create keys to a keystore according to input from the data file.
# Each line in the data file contains 3 parameters for creating a key:
#   keyRef keyAlgorithm keyMaterial
# Blank lines and lines start with # are ignored.
# Examples:
#   kmc/test/key0 AES 000102030405060708090A0B0C0D0E0F000102030405060708090A0B0C0D0E0F
#   kmc/test/hmac0 HmacSHA1 f5731a6e8925f74306fa
#   kmc/test/hmac1 HmacSHA256 9779d9120642797f1747025d5b22b7ac607cab08e1758f2f3a46c8be1e25c53b8c6a8f58ffefa176
#

CP=__LIBPATH__/bc-fips-__BCFIPS_VER__.jar:__LIBPATH__/kmc-key-client.jar
if [ "$CLASSPATH" ]; then
  CP=$CP:$CLASSPATH
fi

USAGE="Usage: $0 [-v] keystoreFile keystoreType keystorePasswordFile dataFile [ keyPasswordFile ] "

if [ "$1" == "-v" ]; then
  VERBOSE="$1"
  shift
fi

if [ $# -lt 4 ]; then
  echo "$USAGE"
  exit 1
fi

if [ "$1" == "-h" ] || [ "$1" == "-help" ] || [ "$1" == "--help" ]; then
  echo "$USAGE"
  exit 1
fi

KEYSTORE="$1"
STORETYPE="$2"
PASSWORDFILE="$3"
DATA="$4"
KEYPASSWORDFILE="$5"

if [ ! -f "$KEYSTORE" ]; then
  touch "$KEYSTORE" >& /dev/null
  if [ $? != 0 ]; then
    echo "Error: Cannot create keystore file: $KEYSTORE"
    echo $USAGE
    exit 1
  fi
  rm -f "$KEYSTORE"
elif [ ! -w "$KEYSTORE" ]; then
  echo "Error: Keystore file ($KEYSTORE) is not writable"
  echo $USAGE
  exit 1
fi

if [ ! -r "$PASSWORDFILE" ]; then
  echo "Error: Unreadable keystore password file: $PASSWORDFILE"
  echo $USAGE
  exit 1
else
  PASSWORD="$(cat $PASSWORDFILE)"
fi

if [ "$KEYPASSWORDFILE" ]; then
  if [ ! -r "$KEYPASSWORDFILE" ]; then
    echo "Error: Unreadable key password file: $KEYPASSWORDFILE"
    echo $USAGE
    exit 1
  else
    KEYPASSWORD="$(cat $KEYPASSWORDFILE)"
  fi
else
  KEYPASSWORD=$PASSWORD
fi

if [ ! -r "$DATA" ]; then
  echo "Error: Unreadable data file: $DATA"
  echo $USAGE
  exit 1
fi

if [ "$VERBOSE" ]; then
  echo "Add keys to $KEYSTORE based on data in $DATA"
  echo
fi

read_line ()
{
  LINE_NUM=$(( $LINE_NUM + 1 ))
  read -r LINE || DONE=true
  if [ "$DONE" ]; then
    #echo end of file
    return
  fi
  if [ -z "$LINE" ]; then
    #echo blank line
    read_line
    return
  fi
  if [[ "$LINE" == \#* ]]; then
    #echo comment line
    read_line
    return
  fi
  if [ "$VERBOSE" ]; then
    echo Line $LINE_NUM: $LINE
  fi
}

while true; do
  read_line
  if [ "$DONE" ]; then
    break
  fi

  java -cp $CP gov.nasa.jpl.ammos.kmc.keystore.KmcKeystoreCreate $KEYSTORE $STORETYPE $PASSWORD $KEYPASSWORD $LINE
  if [ $? == 1 ]; then
    echo "Error: Keystore issue prevented adding keys"
    exit 1
  elif [ $? == 2 ]; then
    echo "Error: Failed to create key from data in line $LINE_NUM"
  fi
done < "$DATA"
