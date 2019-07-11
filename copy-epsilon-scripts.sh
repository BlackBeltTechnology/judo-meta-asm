#!/bin/bash

basedir=`dirname "${0}"`

cp -rv "${basedir}/src/main/epsilon/operations/asm" "${basedir}/../judo-tatami/judo-tatami-psm2asm/src/main/epsilon/operations"
cp -rv "${basedir}/src/main/epsilon/operations/asm" "${basedir}/../judo-tatami/judo-tatami-asm2jaxrsapi/src/main/epsilon/operations"
cp -rv "${basedir}/src/main/epsilon/operations/asm" "${basedir}/../judo-tatami/judo-tatami-asm2openapi/src/main/epsilon/operations"
cp -rv "${basedir}/src/main/epsilon/operations/asm" "${basedir}/../judo-tatami/judo-tatami-asm2rdbms/src/main/epsilon/operations"
cp -rv "${basedir}/src/main/epsilon/operations/asm" "${basedir}/../judo-tatami/judo-tatami-asm2sdk/src/main/epsilon/operations"

