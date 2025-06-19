#!/bin/sh
cd "`dirname $0`/.."
JAVA="$JRE_HOME/bin/java"
if [ ! -x "$JAVA" ]
then
  JAVA="$JAVA_HOME/bin/java"
  if [ ! -x "$JAVA" ]
  then
    JAVA="`eval ls -dt1 \"/opt/j*/bin/java\" 2>/dev/null|head -1`"
    [ ! -x "$JAVA" ] && JAVA="`which java`"
  fi
fi
[ ! -x "$JAVA" ] && printf 'Java not found. If you have Java 1.4 or later installed, set the JRE_HOME environment variable to point to where the JRE or JDK is located.\n' && exit 1
"$JAVA" -classpath test:javamon.jar TestAPI
