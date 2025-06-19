#!/bin/sh
# === CONFIG END ===============================

# Javamon host (localhost, if empty)
JAVAMON_HOST=

# Javamon port (9091, if empty)
JAVAMON_PORT=

# The main class to load
CMDMAINCLASS=TestWrapper

# The command-line parameters for the main class
CMDPARAMS="param1 param2"

# === CONFIG END ===============================
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
CNFG=
[ "$JAVAMON_HOST" = "" ] || CNFG=-Djm.host=$JAVAMON_HOST
[ "$JAVAMON_PORT" = "" ] || CNFG="$CNFG -Djm.port=$JAVAMON_PORT"
[ "$CMDMAINCLASS" = "" ] || CNFG="$CNFG -Djm.main=$CMDMAINCLASS"
printf 'Monitoring endpoint: http://[jm.host]:[jm.port]/metrics\n'
"$JAVA" $CNFG -classpath test:javamon.jar com.agent.javamon $CMDPARAMS
