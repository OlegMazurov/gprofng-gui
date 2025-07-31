dnl Copyright (C) 2022-2025 Free Software Foundation
dnl
dnl This program is free software: you can redistribute it and/or modify
dnl it under the terms of the GNU General Public License as published by
dnl the Free Software Foundation, either version 3 of the License, or
dnl (at your option) any later version.
dnl
dnl This program is distributed in the hope that it will be useful,
dnl but WITHOUT ANY WARRANTY; without even the implied warranty of
dnl MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
dnl GNU General Public License for more details.
dnl
dnl You should have received a copy of the GNU General Public License
dnl along with this program. If not, see <http://www.gnu.org/licenses/>.

AC_INIT([gprofng-gui], 2.0)
AM_INIT_AUTOMAKE
AC_PROG_CC
AC_CHECK_PROGS([JAVA],[java],[no])
AC_CHECK_PROGS([JAVAC],[javac],[no])
AC_CHECK_PROGS([JAR],[jar],[no])
AC_CONFIG_FILES(Makefile)
AM_MAINTAINER_MODE

if test "x$JAVA" = "xno"; then
   AC_ERROR([java is needed to build gprofng-gui.
Please install the JRE.])
fi

if test "x$JAVAC" = "xno"; then
   AC_ERROR([A java compiler is needed to build gprofng-gui.
Please install the JDK.])
fi

if test "x$JAR" = "xno"; then
   AC_ERROR([JAR is needed to buidl gprofng-guil.
Please install the JDK.])
fi

AC_CANONICAL_HOST
build_linux=no
build_windows=no
build_mac=no
echo "host_os=${host_os}" 
case "${host_os}" in
  linux*)
    build_linux=yes
    ;;
  cygwin*|mingw*)
    build_windows=yes
    ;;
  darwin*)
    build_mac=yes
    ;;
  *)
    AC_MSG_ERROR(["OS $host_os is not supported"])
    ;;
esac
AM_CONDITIONAL([BUILD_LINUX], [test "$build_linux" = "yes"])
AM_CONDITIONAL([BUILD_WINDOWS], [test "$build_windows" = "yes"])
AM_CONDITIONAL([BUILD_OSX], [test "$build_mac" = "yes"])

AC_OUTPUT
