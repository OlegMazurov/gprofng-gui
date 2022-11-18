#!/bin/sh

doit() {
    # $1 is a script with arguments
    echo "% $@"
    eval "$@"
    return $?
}

doit pwd

PROG_NAME="`which $0 2> /dev/null`"
[ ! -n "`echo ${PROG_NAME} | sed -n '/^\//p'`" ] && \
    PROG_NAME="`pwd`/`echo $0 | sed 's/^\.\///'`"
DIR="`dirname ${PROG_NAME}`"

RPM_DIR=${DIR}/../RPM_`basename ${DIR} |  sed -e 's/.git$//'`
doit "mkdir -p ${RPM_DIR}"
RPM_DIR=`readlink -f ${RPM_DIR}`



doit "rm -rf ${RPM_DIR}/{RPMS,BUILD{,ROOT},SOURCES,SPEC}"
doit "mkdir -p ${RPM_DIR}/{logs,RPMS,BUILD{,ROOT},SOURCES,SPEC}"

F_LOG="${RPM_DIR}/logs/rpm`date '+%Y_%m%d_%H%M%S'`.log"
pwd > ${F_LOG}
date >> ${F_LOG}
doit "cp ${DIR}/gprofng-gui.spec ${RPM_DIR}/SPEC"

VERSION=`grep ^Version: ${DIR}/gprofng-gui.spec | sed -e 's/^[^2]*//'`
doit "( cd ${DIR}/..; tar --transform 's/^`basename ${DIR}`/gprofng-gui-${VERSION}/' \
  -cJf ${RPM_DIR}/SOURCES/gprofng-gui-${VERSION}.tar.xz `basename ${DIR}` )"
doit "(cd ${RPM_DIR}; time rpmbuild --define='_topdir ${RPM_DIR}' \
    -vv -bb SPEC/gprofng-gui.spec >> ${F_LOG} 2>&1 )"

#    -vv  --short-circuit -bc ${DIR}/linux.binutils.spec >> ${F_LOG} 2>&1 )"

doit "tail -50 ${F_LOG}"
echo "F_LOG: ${F_LOG}"
