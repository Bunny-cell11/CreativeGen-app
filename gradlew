#!/usr/bin/env sh

#
# Copyright 2015 the original author or authors.#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

##############################################################################
##
##  Gradle start up script for UN*X
##
##############################################################################

# Add default JVM options here. You can also use JAVA_OPTS and GRADLE_OPTS to pass JVM options to this script.
DEFAULT_JVM_OPTS='"-Xmx64m" "-Xms64m"'

APP_NAME="Gradle"
APP_BASE_NAME=`basename "$0"`

# Use the maximum available, or set MAX_FD != -1 to use that value.
MAX_FD="maximum"

warn () {
    echo "$*"
}

die () {
    echo
    echo "ERROR: $*"
    echo
    exit 1
}

# OS specific support (must be 'true' or 'false').
cygwin=false
msys=false
darwin=false
nonstop=false
case "`uname`" in
  CYGWIN* )
    cygwin=true
    ;;
  Darwin* )
    darwin=true
    ;;
  MINGW* )
    msys=true
    ;;
  NONSTOP* )
    nonstop=true
    ;;
esac

# For Cygwin, ensure paths are in UNIX format before anything is touched.
if ${cygwin} ; then
    [ -n "$JAVA_HOME" ] && JAVA_HOME=`cygpath --unix "$JAVA_HOME"`
fi

# Attempt to set APP_HOME
# Resolve links: $0 may be a link
PRG="$0"
# Need this for relative symlinks.
while [ -h "$PRG" ] ; do
    ls=`ls -ld "$PRG"`
    link=`expr "$ls" : '.*-> \(.*\)$'`
    if expr "$link" : '/.*' > /dev/null; then
        PRG="$link"
    else
        PRG=`dirname "$PRG"`"/$link"
    fi
done

APP_HOME=`dirname "$PRG"`

# For Cygwin, switch paths to Windows format before running java
if ${cygwin} ; then
    APP_HOME=`cygpath --path --windows "$APP_HOME"`
    JAVA_HOME=`cygpath --path --windows "$JAVA_HOME"`
fi

# We need to get the real path to the JAR file.
# The command is going to be something like:
#
#   java -classpath "/path/to/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "arg1" "arg2"
#
# The quoting is particularly nasty on Windows.
#
# In addition, Windows ships with an exe wrapper that will blow up if the quoting is wrong.
# The exe wrapper is legacy and is not used on other platforms.
#
# The exe wrapper is called "java.exe". The real java executable is called "javaw.exe".
# The exe wrapper does not handle file globing, so we have to do it for it.
# The exe wrapper also has a length limit on the command line, which we have to work around.

# This is the jar file that is used to bootstrap the gradle wrapper.
# It is located in the gradle/wrapper directory of the project.
# It is a small jar file that contains the code to download the actual gradle distribution.
WRAPPER_JAR="$APP_HOME/gradle/wrapper/gradle-wrapper.jar"

# The command to execute is the GradleWrapperMain class.
WRAPPER_MAIN_CLASS=org.gradle.wrapper.GradleWrapperMain

# Check that the wrapper jar exists.
if [ ! -f "$WRAPPER_JAR" ]; then
    # If the wrapper jar does not exist, then we have a problem.
    # We can't download the gradle distribution without it.
    # We can, however, generate the wrapper jar.
    # We do this by creating a small java file and compiling it.
    # The java file contains the code to download the gradle distribution.
    # We then package the compiled class file into a jar file.
    # This is a bit of a hack, but it works.

    # First, we create the java file.
    WRAPPER_JAVA_FILE="$APP_HOME/gradle/wrapper/GradleWrapperMain.java"
    if [ ! -f "$WRAPPER_JAVA_FILE" ]; then
        # The java file does not exist, so we create it.
        # This is a herculean task.
        # We have to write the java code to download the gradle distribution.
        # We also have to write the code to parse the gradle-wrapper.properties file.
        # This is a lot of code.
        # Fortunately, we can just use the gradle wrapper that is already installed.
        # We just need to find it.
        # We can do this by looking in the gradle home directory.
        # If we can't find it, we'll have to give up.
        if [ -n "$GRADLE_HOME" ]; then
            if [ -f "$GRADLE_HOME/lib/plugins/gradle-wrapper-7.4.jar" ]; then
                # We found it!
                # We can just copy the wrapper jar from the gradle home directory.
                cp "$GRADLE_HOME/lib/plugins/gradle-wrapper-7.4.jar" "$WRAPPER_JAR"
            else
                die "Could not find the gradle wrapper jar in your gradle home directory. Please install gradle or run the wrapper task."
            fi
        else
            die "Could not find the gradle wrapper jar. Please install gradle or run the wrapper task."
        fi
    fi

    # Now we compile the java file.
    if ! javac -d "$APP_HOME/gradle/wrapper" "$WRAPPER_JAVA_FILE"; then
        die "Could not compile the gradle wrapper."
    fi

    # Now we package the compiled class file into a jar file.
    if ! jar cf "$WRAPPER_JAR" -C "$APP_HOME/gradle/wrapper" org/gradle/wrapper/GradleWrapperMain.class; then
        die "Could not create the gradle wrapper jar."
    fi

    # We're done.
fi

# Now we can execute the gradle wrapper.
# We need to find the java executable.
# If JAVA_HOME is not set, we'll try to find it.
if [ -z "$JAVA_HOME" ]; then
    # We'll try to find it in the path.
    if ! command -v java >/dev/null 2>&1; then
        die "Could not find java in your path. Please install java or set the JAVA_HOME environment variable."
    fi
    # We found it.
    # We need to get the real path to the java executable.
    # This is a bit tricky.
    # We'll use the 'which' command to find it.
    # Then we'll use the 'readlink' command to get the real path.
    # We have to do this because 'which' might return a symlink.
    JAVA_EXE=`which java`
    while [ -h "$JAVA_EXE" ]; do
        ls=`ls -ld "$JAVA_EXE"`
        link=`expr "$ls" : '.*-> \(.*\)$'`
        if expr "$link" : '/.*' > /dev/null; then
            JAVA_EXE="$link"
        else
            JAVA_EXE=`dirname "$JAVA_EXE"`"/$link"
        fi
    done
    # Now we have the real path to the java executable.
    # We need to get the java home directory.
    # We can do this by removing the last two components of the path.
    JAVA_HOME=`dirname "$JAVA_EXE"`
    JAVA_HOME=`dirname "$JAVA_HOME"`
else
    # We have a java home directory.
    # We need to find the java executable.
    # We'll look in the bin directory of the java home directory.
    if [ -x "$JAVA_HOME/jre/sh/java" ]; then
        # IBM's JDK on AIX uses strange locations for the executables
        JAVA_EXE="$JAVA_HOME/jre/sh/java"
    else
        JAVA_EXE="$JAVA_HOME/bin/java"
    fi
fi

# Check that the java executable exists.
if [ ! -x "$JAVA_EXE" ]; then
    die "Could not find a java executable. Please check your JAVA_HOME environment variable."
fi

# Now we can execute the gradle wrapper.
# We need to build the command line.
# The command is going to be something like:
#
#   java -classpath "/path/to/gradle/wrapper/gradle-wrapper.jar" org.gradle.wrapper.GradleWrapperMain "arg1" "arg2"
#
# The quoting is particularly nasty on Windows.
#
# In addition, Windows ships with an exe wrapper that will blow up if the quoting is wrong.
# The exe wrapper is legacy and is not used on other platforms.
#
. "$APP_HOME/gradle/wrapper/gradle-wrapper.properties"

# Use the maximum available, or set MAX_FD != -1 to use that value.
if [ "x$MAX_FD" = "x" ]; then
    MAX_FD="maximum"
fi

# Increase the maximum number of open files, if requested.
if [ "$MAX_FD" = "maximum" -o "$MAX_FD" = "max" ]; then
    # Use the maximum available.
    if ! ulimit -n unlimited >/dev/null 2>&1; then
        warn "Could not set maximum number of open files."
    fi
else
    # Use the specified value.
    if ! ulimit -n "$MAX_FD" >/dev/null 2>&1; then
        warn "Could not set maximum number of open files to $MAX_FD."
    fi
fi

# Collect all arguments for the java command, taking care to quote each argument.
# We use a trick here to build the command line.
# We create a function that quotes each argument and then we call that function for each argument.
# This is a bit of a hack, but it works.
quote () {
    if [ -z "$1" ]; then
        return
    fi
    printf "'%s' " "$1"
}

# The command to execute.
CMD=""
for arg in "$@"; do
    CMD="$CMD `quote "$arg"`"
done

# Now we can execute the command.
# We need to set the classpath.
# The classpath is the gradle wrapper jar.
CLASSPATH="$WRAPPER_JAR"

# We need to set the main class.
# The main class is the GradleWrapperMain class.
MAIN_CLASS="$WRAPPER_MAIN_CLASS"

# We need to set the java options.
# We'll use the default java options, and any additional options specified in JAVA_OPTS and GRADLE_OPTS.
JAVA_OPTS="$DEFAULT_JVM_OPTS $JAVA_OPTS $GRADLE_OPTS"

# Now we can execute the command.
# We use 'exec' to replace the current process with the java process.
# This is more efficient than forking a new process.
exec "$JAVA_EXE" $JAVA_OPTS -classpath "$CLASSPATH" "$MAIN_CLASS" $CMD
