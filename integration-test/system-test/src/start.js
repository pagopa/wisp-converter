const {getNodoInviaRPT: get_RPT_OK_nostamp} = require("./requests/rpt/ok_nostamp");
const {getNodoInviaRPT: get_RPT_KO_withstamp} = require("./requests/rpt/ko_withstamp");

const {call} = require("./lib/client");
const {DOMParser} = require('xmldom');
const {env} = require("dotenv");


const nodoInviaRPTPrimitive = "nodoInviaRPT";
const nodoInviaCarrelloRPTPrimitive = "nodoInviaCarrelloRPT";

const file = process.argv[2];
const subkey = process.argv[3];

console.log("Executing test case [", file, "]");
main();


async function main() {

    let request = getRequest();
    let responseFromSOAPConverter = await callWispSoapConverter(request);

    if (responseFromSOAPConverter.status !== 200) {
        console.log('Error [', responseFromSOAPConverter.data, ']\n=====================\n');
        return;
    }

    const parser = new DOMParser();
    const xmlDoc = parser.parseFromString(responseFromSOAPConverter.data, "text/xml");
    const outcome = xmlDoc.getElementsByTagName("esito")[0].textContent;


    let url = "";
    if (outcome === "OK") {
        url = xmlDoc.getElementsByTagName("url")[0].textContent;
    } else {
        const faultCode = xmlDoc.getElementsByTagName("faultCode")[0].textContent;
        const description = xmlDoc.getElementsByTagName("description")[0].textContent;
        console.log('Error [', faultCode, ']: ', description, "\n=====================\n");
        return;
    }

    // temporary replacement
    url = url.replace(/http:\/\/adapterecommerce\.pagopa\.it\?idSession/g, process.env.wisp_converter_host);

    console.log('Calling WISP Converter at URL [', url, ']\n=====================\n');
    let responseFromConverter = await call("GET", url, {});

    console.log('==Response:==\nStatus: [', responseFromConverter.status, ']\n');
    if (responseFromConverter.status !== 200) {
        console.log(responseFromConverter.data);
    }
    console.log('\n=====================\n');
}

function getRequest() {
    let request;
    switch(file) {
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
    console.log('==Request:==\nURL: [', url, ']\nContent: ', request[1], "\nHeaders: ", headers, "\n=====================\n");
    return await call("POST", process.env.wisp_converter_soap_host, request[1], headers);
}

