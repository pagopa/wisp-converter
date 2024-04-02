#!/bin/bash

cd ../

echo "Downloading last stable version for WSDL and XSD files from repository [pagopa-api]."
echo "\n------------------------------------------------------------------------"
echo "Downloading [NodoPerPa.wsdl] file from remote repository..."
curl -L https://raw.githubusercontent.com/pagopa/pagopa-api/master/gad/wsdl/NodoPerPa.wsdl > src/main/resources/wsdl/NodoPerPa.wsdl
echo "Downloaded [NodoPerPa.wsdl] file!"
echo "\n------------------------------------------------------------------------"
echo "Downloading [PagInf_RPT_RT_6_2_0.xsd] file from remote repository..."
curl -L https://raw.githubusercontent.com/pagopa/pagopa-api/master/gad/xsd/PagInf_RPT_RT_6_2_0.xsd > src/main/resources/xsd/PagInf_RPT_RT_6_2_0.xsd
echo "Downloaded [PagInf_RPT_RT_6_2_0.xsd] file!"
echo "\n------------------------------------------------------------------------"
echo "Downloading [envelope.xsd] file from remote repository..."
curl -L https://raw.githubusercontent.com/pagopa/pagopa-api/master/gad/xsd/envelope.xsd > src/main/resources/xsd/envelope.xsd
echo "Downloaded [envelope.xsd] file!"
echo "\n------------------------------------------------------------------------"
echo "Downloading [sac-common-types-1.0.xsd] file from remote repository..."
curl -L https://raw.githubusercontent.com/pagopa/pagopa-api/master/gad/xsd/sac-common-types-1.0.xsd > src/main/resources/xsd/sac-common-types-1.0.xsd
echo "Downloaded [sac-common-types-1.0.xsd] file!"
echo "\n------------------------------------------------------------------------"
echo "Downloaded all files!"
echo "\n------------------------------------------------------------------------"
echo "Compiling class from new WSDL and XSD files..."
mvn clean compile
echo "\n\nEnded compilation!"
echo "\n------------------------------------------------------------------------"