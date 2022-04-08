#!/usr/bin/env sh

if [ -n "${CRYPTOPRO_SERIAL}" ] && [ -n "${CRYPTOPRO_COMPANY}" ]; then
  echo "Enabling license..."
  "$JAVA_HOME/jre/bin/java" ru.CryptoPro.JCP.tools.License -serial "${CRYPTOPRO_SERIAL}" -company "${CRYPTOPRO_COMPANY}" -store
fi

# Копируем корневые серты КриптоПро в cacerts
keytool -J-Dkeytool.compat=true -J-Duse.cert.stub=true -list -provider ru.CryptoPro.JCP.JCP -storetype HDImageStore -keystore NONE -storepass 1 \
  | grep PrivateKeyEntry \
  | cut -d , -f1 \
  | while read -r alias; do
    CERT=$(keytool -J-Dkeytool.compat=true -J-Duse.cert.stub=true -list -provider ru.CryptoPro.JCP.JCP -storetype HDImageStore -keystore NONE -storepass 1 -alias "$alias" -rfc)
    ROOT_ALIAS="root-$alias"
    echo "${CERT##*:}" > "/tmp/$ROOT_ALIAS"
    keytool -J-Dkeytool.compat=true -J-Duse.cert.stub=true -keystore "$JAVA_HOME/jre/lib/security/cacerts" -storepass changeit -noprompt -trustcacerts -import -alias "root-$alias" -provider ru.CryptoPro.JCP.JCP -file "/tmp/$ROOT_ALIAS"
  done;

"$JAVA_HOME/jre/bin/java" -jar /opt/app.jar -Dru.CryptoPro.AdES.validate_tsp=false