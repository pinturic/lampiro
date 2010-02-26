#! /bin/sh


if [ $# -eq 0 ]
then
	ALL_LANG="en"
else
    ALL_LANG=$@
fi
for LANG_FILE in $ALL_LANG
do
	echo "$LANG_FILE"
	RELEASES="releases" 
	RESPATH="${RELEASES}_${LANG_FILE}"
	echo "Building $RESPATH"
	rm -rf $RESPATH
	mkdir $RESPATH
	mkdir $RESPATH/base
	mkdir $RESPATH/compression
	mkdir $RESPATH/TLS

	ant deployedSuite -propertyfile eclipseme-build-compression.properties -Dlang=$LANG_FILE build
	cp deployed/lampiro.ja* $RESPATH/compression/
	zip -j $RESPATH/compression/lampiro-${LANG_FILE}-10.2.1-compression.zip $RESPATH/compression/lampiro.jad $RESPATH/compression/lampiro.jar

	ant deployedSuite -Dlang=$LANG_FILE build
	cp deployed/lampiro.ja* $RESPATH/base/
	zip -j $RESPATH/base/lampiro-${LANG_FILE}-10.2.1.zip $RESPATH/base/lampiro.jad $RESPATH/base/lampiro.jar

	ant deployedSuite -propertyfile eclipseme-build-TLS.properties -Dlang=$LANG_FILE build
	cp deployed/lampiro.ja* $RESPATH/TLS/
	zip -j $RESPATH/TLS/lampiro-${LANG_FILE}-10.2.1-TLS.zip $RESPATH/TLS/lampiro.jad $RESPATH/TLS/lampiro.jar
done
