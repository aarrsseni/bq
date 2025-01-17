#!/usr/bin/env bash
curl -OL https://github.com/oracle/graal/releases/download/vm-19.1.1/graalvm-ce-linux-amd64-19.1.1.tar.gz
tar zxf graalvm-ce-linux-amd64-19.1.1.tar.gz
sudo mv graalvm-ce-19.1.1 /usr/lib/jvm/
export JAVA_HOME=/usr/lib/jvm/graalvm-ce-19.1.1
export PATH=$PATH:${JAVA_HOME}/bin

mvn clean install
${JAVA_HOME}/bin/gu install native-image
NAME=$(basename $(find . -type f -name 'bq-*.jar'))
mkdir release-deb
cd release-deb

native-image --no-server --report-unsupported-elements-at-runtime --no-fallback \
             --initialize-at-build-time=io.bootique.tools.shell.command.ShellCommand \
             --initialize-at-build-time=io.bootique.command.Command \
             -jar ../target/${NAME} -H:Name=bq

PACK_NAME=$(ls)
chmod +x ${PACK_NAME}
mkdir packageroot
mkdir packageroot/DEBIAN
touch packageroot/DEBIAN/control

VERSION=$(echo "${NAME%.*}" | cut -d'-' -f 2)

echo "Package: $PACK_NAME
Version: $VERSION
Architecture: amd64
Maintainer: John Doe <john@doe.com>
Description: test
" > packageroot/DEBIAN/control

cat packageroot/DEBIAN/control

mkdir -p packageroot/usr/bin
cp ${PACK_NAME} packageroot/usr/bin/
dpkg-deb -b packageroot ${PACK_NAME}-${VERSION}.deb
sudo dpkg -i ./${PACK_NAME}-${VERSION}.deb
sudo apt-get install -f

DEB_PACK=$(find . -type f -name 'bq-*.deb')
echo ${DEB_PACK}
cp ${DEB_PACK} ../target/

cd ../target

# convert to rpm
sudo apt-get update
sudo apt-get install rpm
sudo apt-get install ruby ruby-dev rubygems build-essential
gem install --no-ri --no-rdoc fpm

fpm -t rpm -s deb ${PACK_NAME}-${VERSION}.deb

pwd
ls



