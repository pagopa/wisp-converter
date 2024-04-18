const {getNodoInviaRPT: get_RPT_OK_nostamp} = require("./requests/rpt/ok_nostamp");
const {getNodoInviaRPT: get_RPT_KO_withstamp} = require("./requests/rpt/ko_withstamp");

const {call} = require("./lib/client");
const {DOMParser} = require('xmldom');
const {env} = require("dotenv");


const nodoInviaRPTPrimitive = "nodoInviaRPT";
const nodoInviaCarrelloRPTPrimitive = "nodoInviaCarrelloRPT";

const testCase = process.argv[2];
const subkey = process.argv[3];


printSeparator();
console.log("Executing test case [", testCase, "]");
printSeparator();

main();


async function main() {

    // retrieve the request based on test case
    let request = getRequest(testCase);

    // executing call to WISP SOAP converter
    let responseFromSOAPConverter = await callWispSoapConverter(request);
    if (responseFromSOAPConverter.status !== 200) {
        console.log('Error [', responseFromSOAPConverter.data, ']');
        printSeparator();
        return;
    }

    // extract outcome from WISP SOAP converter's response
    const xmlDoc = new DOMParser().parseFromString(responseFromSOAPConverter.data, "text/xml");
    const outcome = xmlDoc.getElementsByTagName("esito")[0].textContent;

    //
    let url = "";
    if (outcome === "OK") {
        url = xmlDoc.getElementsByTagName("url")[0].textContent;
    }

    else {
        const faultCode = xmlDoc.getElementsByTagName("faultCode")[0].textContent;
        const description = xmlDoc.getElementsByTagName("description")[0].textContent;
        console.log('Error [', faultCode, ']: ', description);
        printSeparator();
        return;
    }

    // temporary replacement
    url = url.replace(/http:\/\/adapterecommerce\.pagopa\.it\?idSession/g, process.env.wisp_converter_host);

    console.log('Calling WISP Converter at URL [', url, ']');
    printSeparator();
    let responseFromConverter = await call("GET", url, {});

    console.log('==Response:==\nStatus: [', responseFromConverter.status, ']\n');
    if (responseFromConverter.status !== 200) {
        console.log(responseFromConverter.data);
    }
    printSeparator();
}

function getRequest(testCase) {
    let request;
    switch(testCase) {
        case "rpt_ok_nostamp":
            request = [nodoInviaRPTPrimitive, get_RPT_OK_nostamp()];
            break;
        case "rpt_ko_withstamp":
            request = [nodoInviaRPTPrimitive, get_RPT_KO_withstamp()];
            break;
        default:
            throw "\nTEST CASE NOT FOUND!\n"
            break;
    }
    return request;
}

async function callWispSoapConverter(request) {
    let url = process.env.wisp_converter_soap_host;
    let headers = {
        "SOAPAction": request[0],
        "Ocp-Apim-Subscription-Key": `${subkey};product=nodo-auth-wisp`
    }

    printSeparator();
    console.log('==Request==\nURL: [', url, ']\nContent: ', request[1]);
    printSeparator();
    return await call("POST", process.env.wisp_converter_soap_host, request[1], headers);
}

function printSeparator() {
    console.log('\n=========================================\n');
}

