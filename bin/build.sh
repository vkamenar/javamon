#!/bin/sh
# === CONFIG BEGIN ==========================================

# If you have Proguard installed, set the jar location below.
PROGUARD=~/proguard.jar

# === CONFIG END ============================================
cd "`dirname $0`/.."

# Get JDK path
JMINVER=1.4
JAVAC="$JDK_HOME/bin/javac"
if [ ! -x "$JAVAC" ]
then
  JAVAC="$JAVA_HOME/bin/javac"
  if [ ! -x "$JAVAC" ]
  then
    JAVAC="`eval ls -dt1 \"/opt/j*/bin/javac\" 2>/dev/null|head -1`"
    [ ! -x "$JAVAC" ] && JAVAC="`which javac`"
  fi
fi
[ ! -x "$JAVAC" ] && printf 'JDK not found. If you have JDK %s or later installed, set the JDK_HOME environment variable to point to where the JDK is located.\n' "$JMINVER" && exit 1
JDK="`dirname \"$JAVAC\"`"
rm -rf classes 2>/dev/null
mkdir -p classes/javamon >/dev/null 2>/dev/null
RT="$JDK/../jre/lib/rt.jar"
[ ! -f $RT ] && printf '%s not found\n' $RT && exit 1

# Compile the javamon
"$JAVAC" -source $JMINVER -target $JMINVER -bootclasspath $RT -classpath src test/TestWrapper.java test/TestAPI.java
"$JAVAC" -source $JMINVER -target $JMINVER -bootclasspath $RT -d classes/javamon -g:lines -classpath src src/com/agent/javamon.java
[ $? -eq 0 ] || exit 1

# Generate the Manifest
cat >classes/javamon.MF <<EOL
Manifest-Version: 1.0
Main-Class: com.agent.javamon
Copyright: 2025
Created-By: Vladimir Kamenar

EOL

# Pack the JAR (optionally optimize with Proguard)
if [ -f "$PROGUARD" ]
then
  "$JDK/jar" cmf classes/javamon.MF javamon_in.jar -C classes/javamon .
  [ $? -eq 0 ] || exit 1
  "$JDK/java" -jar "$PROGUARD" -injars javamon_in.jar -outjar javamon.jar -libraryjars $RT @src/javamon.pro
  rm -f javamon_in.jar
else
  "$JDK/jar" cmf classes/javamon.MF javamon.jar -C classes/javamon .
fi
