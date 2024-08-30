import http from "k6/http";
import { check } from "k6";
import { parseHTML } from "k6/html";
import { SharedArray } from "k6/data";
import { makeidNumber, makeidMix, randomString } from "./modules/helpers.js";
import { getNodoInviaRPTReqBody } from "./modules/data.js";

export let options = JSON.parse(open(__ENV.TEST_TYPE));

// read configuration
const varsArray = new SharedArray("vars", function () {
  return JSON.parse(open(`./${__ENV.VARS}`)).environment;
});
const vars = varsArray[0];
const wisp_url = `${vars.wisp_url}`;

// initialize parameters taken from env
const nodoBasePath = `${vars.nodo_pa_host}`;
const creditorInstitutionCode = `${vars.id_pa}`;
const idBrokerPA = `${vars.id_broker_pa}`;
const idStation = `${vars.id_station}`;
const idBrokerPsp = `${vars.id_broker_psp}`;
const idPsp = `${vars.id_psp}`;
const idChannel = `${vars.id_channel}`;
const creditorIban = `${vars.iban_pa}`;

const nodoSubscriptionKey = `${__ENV.NODO_SUBSCRIPTION_KEY}`;
const pwdStation = `${__ENV.STATION_PWD}`;

export default function () {
  // initialize constant values for this execution
  const iuv = makeidNumber(17);
  const ccp = makeidNumber(17);
  const due_date = new Date().addDays(30);
  const retention_date = new Date().addDays(90);


  // Get details of a specific payment option.
  var tag = {
    paymentRequest: "nodoInviaRPT",
  };

  // defining URL, body and headers related to the nodoInviaRPT call
  var url = `${nodoBasePath}`;
  var soapParams = {
    responseType: "text",
    headers: {
      "Content-Type": "text/xml",
      SOAPAction: "nodoInviaRPT",
      "Ocp-Apim-Subscription-Key": nodoSubscriptionKey
    },
  };

  var payload = getNodoInviaRPTReqBody(idPsp, idBrokerPsp, idChannel, creditorInstitutionCode, idBrokerPA, idStation, iuv, ccp, pwdStation, creditorIban)

      // execute the call and check the response
      var response = http.post(url, payload, soapParams);

      console.log(
        "Send nodoInviaRPT req - creditor_institution_code = " +
          creditorInstitutionCode +
          ", iuv = " +
          iuv +
          ", Status = " +
          response.status +
          " \n\t[URL: " +
          url +
          "]"
      );
      if (response.status != 200 && response.status != 504) {
        console.error(
          "-> NodoInviaRPT req - creditor_institution_code = " +
            creditorInstitutionCode +
            ", iuv = " +
            iuv +
            ", Status = " +
            response.status +
            ", Body=" +
            response.body
        );
      }

      check(
        response,
        {
          "NodoInviaRPT status is 200, outcome is OK and url is wisp dismantling": (response) =>
            response.status === 200 &&
            parseHTML(response.body)
              .find("esito")
              .text() === "OK" &&
            parseHTML(response.body)
              .find("url")
              .text().startsWith(wisp_url),
        },
        tag
      );
}
