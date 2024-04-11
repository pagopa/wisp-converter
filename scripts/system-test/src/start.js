const {getNodoInviaRPT: get_RPT_OK_nostamp} = require("./requests/rpt/ok_nostamp");
const {getNodoInviaRPT: get_RPT_KO_withstamp} = require("./requests/rpt/ko_withstamp");

const {call} = require("./lib/client");
const {DOMParser} = require('xmldom');
const {env} = require("dotenv");


const nodoInviaRPTPrimitive = "NodoInviaRPT";
const nodoInviaCarrelloRPTPrimitive = "NodoInviaCarrelloRPT";

const file = process.argv[2];
const subkey = process.argv[3];
main();


async function main() {

    let request = getRequest();
    let responseFromSOAPConverter = await callWispSoapConverter(request);

    const parser = new DOMParser();
    const xmlDoc = parser.parseFromString(responseFromSOAPConverter.data, "text/xml");
    const outcome = xmlDoc.getElementsByTagName("esito")[0].textContent;
    console.log('==Response:==\nStatus: [', responseFromSOAPConverter.status, ']\n=====================\n');


    let url = "";
    let isOK = false;
    if (outcome === "OK") {
        isOK = true;
        url = xmlDoc.getElementsByTagName("url")[0].textContent;
    } else {
        const faultCode = xmlDoc.getElementsByTagName("faultCode")[0].textContent;
        const description = xmlDoc.getElementsByTagName("description")[0].textContent;
        console.log('Error [', faultCode, ']: ', description, "\n=====================\n");
    }

    if (isOK) {
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
    console.log('==Request:==\nContent: ', request[1], "\n=====================\n");
    let headers = {
        "SOAPAction": request[0],
        "Ocp-Apim-Subscription-Key": subkey
    }
    return await call("POST", process.env.wisp_converter_soap_host, request[1], headers);
}