#!/usr/bin/env bash
# Copyright (C) 2022 Free Software Foundation
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program. If not, see <http://www.gnu.org/licenses/>.

# loader for gprofng GUI

#
# L10N Message translation utility
#	$1 - message id
#	$2 - fallback message text
#

GTXT()
{
    eval I18Ntxt$2='$1'
    eval I18Nset$2='$3'
    eval I18Nid$2='$4'
}

# define all i18n messages
GTXT "Usage: %s {options} arguments\n\nOptions can be\n\n" 1 17 1
GTXT "\t-?|-h|--help\n\t    show usage and exit\n" 2 17 2
GTXT "\t-j|--jdkhome <path>\n\t    specify the JVM directory\n" 3 17 3
GTXT "\t-J<jvm_options>\n\t    pass <jvm_options> to JVM\n" 4 17 4
GTXT "\t-v|--verbose\n\t    enable verbose output\n" 5 17 5
GTXT "\t-V|--version\n\t    show version and exit\n" 6 17 6
GTXT "\nAll other options and arguments are passed to the Analyzer.\nSee documentations for details.\n" 7 17 7
GTXT "Cannot find JVM. Please set the JAVA_PATH environment variable to point\nto your JVM installation directory, or use the -j switch.\n" 8 17 8
GTXT "Cannot find JVM at \"%s\". Please set the JAVA_PATH\nenvironment variable to point to your JVM installation directory,\nor use the -j switch.\n" 9 17 9
GTXT "\t-f|--fontsize <size>\n\t    specify the font size to be used in the gprofng GUI\n" 10 17 10
GTXT "Unrecognized option \"%s\"\n" 11 17 11
GTXT "Studio Default" 12 17 12
GTXT "WARNING: JDK_1_4_HOME is obsolete, and is ignored\n" 13 17 13
GTXT "Java at %s selected by %s\n" 14 17 14
GTXT "System Default" 15 17 15
GTXT "%s: ERROR: environment variable DISPLAY is not set\n" 16 17 16
GTXT "%s: WARNING: Current directory does not exist\n" 17 17 17
GTXT "%s: WARNING: Changed current directory to /\n" 18 17 18
GTXT "\t-c|--compare\n\t    enter compare mode\n" 19 17 19
GTXT "\t-u|--userdir <path>\n\t    use specified directory to store user settings\n" 20 17 20
GTXT "%s: ERROR: argument is missed for %s option\n" 21 17 21
GTXT "Error: java (%s) cannot be used to run gprofng GUI (command not found)\n" 22 17 22
GTXT "Error: java (%s) cannot be used to run gprofng GUI (Unsupported major.minor version)\n" 23 17 23
GTXT "    The following J2SE[tm] versions are recommended:\n" 24 17 24
GTXT "      J2SE[tm] 1.7.0_72 or later 1.7.0 updates\n" 25 17 25
GTXT "      J2SE[tm] 1.8.0_20 or later 1.8.0 updates\n" 26 17 26
GTXT "Error: java (%s) cannot be used to run gprofng GUI (unrecognized class file version)\n" 27 17 27
Message()
{
    text=I18Ntxt$1
    mset=I18Nset$1
    mid=I18Nid$1
    if [ -x /usr/dt/bin/dtdspmsg ]; then
	eval eval /usr/dt/bin/dtdspmsg -s \${${mset}} analyzer.cat \${${mid}} \\\"\\\${$text}\\\"  ${2:+"\\\"$2\\\""} ${3:+"\\\"$3\\\""} ${4:+"\\\"$4\\\""}
    else
        eval eval printf \\\"\\\${$text}\\\" ${2:+"\\\"$2\\\""} ${3:+"\\\"$3\\\""} ${4:+"\\\"$4\\\""}
    fi
}

Usage()
{
    Message 1 $0    # "Usage: %s {options} arguments\n\nOptions can be\n\n"
    Message 2       # "\t-?|-h|--help\n\t    show usage and exit\n"
    Message 3       # "\t-j|--jdkhome <path>\n\t    specify the JVM directory\n"
    Message 4       # "\t-J<jvm_options>\n\t    pass <jvm_options> to JVM\n"
    Message 10      # "\t-f|--fontsize <size>\n\t    specify the font size to be used in the Analyzer\n"
    Message 5       # "\t-v|--verbose\n\t    enable verbose output\n"
    Message 6       # "\t-V|--version\n\t    show version and exit\n"
    Message 19      # -c|--compare
    Message 20      # -u|--userdir
    Message 7       # "\nAll other options and arguments are passed to the Analyzer.\nSee documentations for details.\n"
}

###########################################################################
# main program starts here

# defaults
# jdkhome will be really set below
# the jvmflags value set here can be augmented by a -J argument
# the verbose value set here can be overridden by a -v argument

jdkhome=""
java_how="?"
jvmflags="-Xmx1024m"
verbose="false"

# end of defaults

PATH=$PATH:/bin:/usr/bin
export PATH

#
# define OS_TYPE
#

OS_TYPE=`/bin/uname`

PRG=$(readlink -f -- "$0")
progdir=$(dirname -- "$PRG")

fdhome="$progdir/.."

GPROFNG_bindir=
GPROFNG_libdir=
GPROFNG_datadir=

#
# L10N path
#
NLSPATH="${GPROFNG_libdir}/locale/%L/LC_MESSAGES/%N.cat:${GPROFNG_libdir}/locale/%L/LC_MESSAGES/%N:$NLSPATH"
export NLSPATH

#
# absolutize fdhome
#

oldpwd=`pwd` ; 
if [ $? -ne 0 ]; then
    Message 17 $0  # echo "$0: WARNING: Current directory does not exist"
    cd /
    Message 18 $0  # echo "$0: WARNING: Changed current directory to /"
    oldpwd=`pwd` ;
fi
cd "$fdhome"; fdhome=`pwd`; cd "$oldpwd"; unset oldpwd

jargs=$jvmflags

args="--bindir='${GPROFNG_bindir}' --datadir='${GPROFNG_datadir}'"

# see if invoked as "tha" or "rdt", and set environment variable accordingly
if [ "`basename $0`" = "tha" -o "`basename $0`" = "rdt" ]; then
    SP_ANALYZER_CONFIG_MODE="R"
    export SP_ANALYZER_CONFIG_MODE
fi

# Check that there is an argument for the $1 option
CheckArgsCount()
{
    if [ $2 -lt 2 ]; then
        Message 21 ${PRG} $1
        # Usage;
        exit 2
    fi
}

HOME_USER_DIR=${HOME}/.gprofng
DEFAULT_USER_DIR=${HOME_USER_DIR}/gui
USER_DIR=${DEFAULT_USER_DIR}

whoami=`basename $PRG`
# parse arguments
endargs=false
while [ $# -gt 0 ] ; do
    case "$1" in
	--whoami=*)
            whoami=`echo -- "$1" | sed -e 's/--whoami=//'`
	    ;;
	-j|--jdkhome) 
            CheckArgsCount "$1" $# ;
            shift;
            java_how="-j";
            jdkhome=$1; # jdk home
            ;;
	-J*) jopt=`expr $1 : '-J\(.*\)'`; jargs="$jargs \"$jopt\"";;
	-v|--verbose) verbose="true";;
	-V|--version) echo GNU `basename $PRG`: "REPLACE_ME_WITH_VERSION"; exit 0;;
	-\?|-h|--help) Usage; exit 0;;
	-f|--fontsize) 
            CheckArgsCount "$1" $# ;
            args="$args \"$1\"" ; # option
            shift;
            args="$args \"$1\"" ; # font size
            ;;
	-c|--compare) args="$args $1" ;;
	-u|--userdir) 
            CheckArgsCount "$1" $# ;
            args="$args \"$1\"" ; # option
            shift;
            args="$args \"$1\"" ; # user directory
            USER_DIR=$1
            ;;
	--gprofngdir=*)
	    args="$args \"$1\""
	    ;;
	-*) Message 11 $1; Usage; exit 1;;
	*)  endargs=true ;;
    esac
    [ $endargs = true ] && break
    shift
done

# Add remaining arguments and pass them down
while [ $# -gt 0 ] ; do
    args="$args \"$1\"" ;
    shift
done

# create user directory and set correct permissions
if [ ! -d ${USER_DIR} ]; then
    /bin/mkdir -p ${USER_DIR}
    if [ "${USER_DIR}" = "${DEFAULT_USER_DIR}" ]; then
        /bin/chmod 700 ${HOME_USER_DIR}
    fi
fi
/bin/chmod 700 ${USER_DIR}

# if user did not specify a jdkhome, set default
if [ "$jdkhome" = "" ]; then
    # look at environment variables, and -j to find Java

    # if JAVA_PATH is set it overrides the default in the script
    if [ ! -z "$JAVA_PATH" ] ; then
	jdkhome="$JAVA_PATH"
	java_how="JAVA_PATH"
    fi

    # if JDK_HOME is set it overrides the default and JAVA_PATH in the script
    if [ ! -z "$JDK_HOME" ] ; then
	jdkhome="$JDK_HOME"
	java_how="JDK_HOME"
    fi

    # if we still don't have a jdkhome, try the user's path
    if [ "$jdkhome" = "" ]; then
        javaloc=`LC_ALL=C type java | sed -e 's|^[^/]*||' | sed -e "s|/java'|/java|"`
        if [ -f "$javaloc" ]; then
            javaloc1=$(dirname -- "$javaloc")
            jdkhome=$(dirname -- "$javaloc1")
            java_how="PATH"
        fi
    fi

    # finally, just try /usr/java/bin/java
    if [ "$jdkhome" = "" ]; then
	jdkhome=/usr/java
	java_how=$I18Ntxt15
    fi
fi

#
# check JVM
#

if [ -z "$jdkhome" ] ; then
    Message 8
    exit 1
fi

if [ ! -x "$jdkhome/bin/java" ] ; then
    Message 9 $jdkhome
    exit 1
fi

if [ $verbose = "true" ] ; then
	echo `basename $PRG`: "REPLACE_ME_WITH_VERSION";
	Message 14 "$jdkhome/bin/java" "$java_how"
	$jdkhome/bin/java -version;
fi

# Check DISPLAY variable
if [ "no$DISPLAY" = "no" ]; then
    Message 16 $0  # "$0: ERROR: environment variable DISPLAY is not set"
    exit 2
fi

# If this is Solaris SPARC disable sun.java2d.xrender
if [ "${OS_TYPE}" = "SunOS" ]; then
    # if [ "no$VNCDESKTOP" != "no" ]; then
        HW_TYPE=`/bin/uname -p`
        if [ "${HW_TYPE}" = "sparc" ]; then
            jargs="-Dsun.java2d.xrender=false ${jargs}"
        fi
    # fi
fi

# Set special environment variable
SP_COLLECTOR_FROM_GUI=1
export SP_COLLECTOR_FROM_GUI

PID=$$
LOG="${USER_DIR}/an.${PID}.log"
/bin/rm -f -- "${LOG}"

gprofng_jar="${GPROFNG_datadir}/gprofng-gui/gprofng-analyzer.jar"
if [ $verbose = "true" ] ; then
    echo "Run java:"
    echo "'$jdkhome/bin/java' $jargs -jar ${gprofng_jar} $args > ${LOG} 2>&1"
#    eval "/usr/bin/strace -v -f -t -o ${USER_DIR}/truss.log '$jdkhome/bin/java'" $jargs -jar ${gprofng_jar} $args
#    exit
fi

eval "'$jdkhome/bin/java'" $jargs -jar ${gprofng_jar} $args > "${LOG}" 2>&1
res=$?

if [ ${res} -eq 0 ]; then
    /bin/cat -- "${LOG}"
    /bin/rm -f -- "${LOG}"
    exit ${res}
fi

# Execution failed
# /bin/echo "Command failed: $jdkhome/bin/java $jargs -jar ${gprofng_jar} $args"
err=`/bin/cat -- "${LOG}" | /bin/grep UnsupportedClassVersionError | wc -l`
if [ ${err} -eq 0 ]; then
    err=`/bin/cat -- "${LOG}" | /bin/grep 'java: command not found' | wc -l`
    if [ ${err} -eq 0 ]; then
        err=`/bin/cat -- "${LOG}" | /bin/grep ClassFormatError | wc -l`
        if [ ${err} -eq 0 ]; then
            # unknown error
            /bin/cat -- "${LOG}"
            /bin/rm -f -- "${LOG}"
            exit ${res}
        fi
        Message 27 "$jdkhome/bin/java"
        # /bin/echo "Error: java from PATH cannot be used to run Performance Analyzer GUI (unrecognized class file version)"
    else
        Message 22 "$jdkhome/bin/java"
        # /bin/echo "Error: java from PATH cannot be used to run Performance Analyzer GUI (command not found)"
    fi
else
    Message 23 "$jdkhome/bin/java"
    # /bin/echo "Error: java from PATH cannot be used to run Performance Analyzer GUI (Unsupported major.minor version)"
fi
Message 24
Message 25
Message 26
/bin/rm -f -- "${LOG}"
exit ${res}

