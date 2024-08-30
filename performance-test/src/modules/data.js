import encoding from 'k6/encoding';

export function getNodoInviaRPTReqBody(idPsp, idBrokerPsp, idChannel, creditorInstitutionCode, idBrokerPA, idStation, iuv, ccp, password, creditorIban) {
  let payload = `<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ppt="http://ws.pagamenti.telematici.gov/ppthead" xmlns:ws="http://ws.pagamenti.telematici.gov/">
                     <soapenv:Header>
                         <ppt:intestazionePPT>
                             <identificativoIntermediarioPA>${idBrokerPA}</identificativoIntermediarioPA>
                             <identificativoStazioneIntermediarioPA>${idStation}</identificativoStazioneIntermediarioPA>
                             <identificativoDominio>${creditorInstitutionCode}</identificativoDominio>
                             <identificativoUnivocoVersamento>${iuv}</identificativoUnivocoVersamento>
                             <codiceContestoPagamento>${ccp}</codiceContestoPagamento>
                         </ppt:intestazionePPT>
                     </soapenv:Header>
                     <soapenv:Body>
                         <ws:nodoInviaRPT>
                             <password>${password}</password>
                             <identificativoPSP>${idPsp}</identificativoPSP>
                             <identificativoIntermediarioPSP>${idBrokerPsp}</identificativoIntermediarioPSP>
                             <identificativoCanale>${idChannel}</identificativoCanale>
                             <tipoFirma></tipoFirma>
                             <rpt>${getRPT(creditorInstitutionCode, idStation, iuv, ccp, creditorIban)}</rpt>
                         </ws:nodoInviaRPT>
                     </soapenv:Body>
                 </soapenv:Envelope>`;
  return payload;
}

function getRPT(creditorInstitutionCode, idStation, iuv, ccp, creditorIban) {
  let time = new Date().toJSON();
  let dateString = time.substring(0, 10);
  let timeString = time.substring(0, 19);
  let amount = getRandomMonetaryAmount(10.0, 599.99);
  let fee = getRandomMonetaryAmount(0, 1);
  let rpt = `<pay_i:RPT xmlns:pay_i="http://www.digitpa.gov.it/schemas/2011/Pagamenti/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.digitpa.gov.it/schemas/2011/Pagamenti/ PagInf_RPT_RT_6_2_0.xsd ">
                     <pay_i:versioneOggetto>6.0</pay_i:versioneOggetto>
                     <pay_i:dominio>
                         <pay_i:identificativoDominio>${creditorInstitutionCode}</pay_i:identificativoDominio>
                         <pay_i:identificativoStazioneRichiedente>${idStation}</pay_i:identificativoStazioneRichiedente>
                     </pay_i:dominio>
                     <pay_i:identificativoMessaggioRichiesta>systemtest</pay_i:identificativoMessaggioRichiesta>
                     <pay_i:dataOraMessaggioRichiesta>${timeString}</pay_i:dataOraMessaggioRichiesta>
                     <pay_i:autenticazioneSoggetto>CNS</pay_i:autenticazioneSoggetto>
                     <pay_i:soggettoVersante>
                         <pay_i:identificativoUnivocoVersante>
                             <pay_i:tipoIdentificativoUnivoco>F</pay_i:tipoIdentificativoUnivoco>
                             <pay_i:codiceIdentificativoUnivoco>RSSMRA70A01H501Z</pay_i:codiceIdentificativoUnivoco>
                         </pay_i:identificativoUnivocoVersante>
                         <pay_i:anagraficaVersante>Mario Rossi</pay_i:anagraficaVersante>
                         <pay_i:indirizzoVersante>Via della Conciliazione</pay_i:indirizzoVersante>
                         <pay_i:civicoVersante>1</pay_i:civicoVersante>
                         <pay_i:capVersante>00100</pay_i:capVersante>
                         <pay_i:localitaVersante>Roma</pay_i:localitaVersante>
                         <pay_i:provinciaVersante>RM</pay_i:provinciaVersante>
                         <pay_i:nazioneVersante>IT</pay_i:nazioneVersante>
                         <pay_i:e-mailVersante>mario.rossi@mail.com</pay_i:e-mailVersante>
                     </pay_i:soggettoVersante>
                     <pay_i:soggettoPagatore>
                 		<pay_i:identificativoUnivocoPagatore>
                 			<pay_i:tipoIdentificativoUnivoco>F</pay_i:tipoIdentificativoUnivoco>
                 			<pay_i:codiceIdentificativoUnivoco>VRDMRN72A12H501Z</pay_i:codiceIdentificativoUnivoco>
                 		</pay_i:identificativoUnivocoPagatore>
                         <pay_i:anagraficaPagatore>Marina Verdi</pay_i:anagraficaPagatore>
                         <pay_i:indirizzoPagatore>Via della Conciliazione</pay_i:indirizzoPagatore>
                         <pay_i:civicoPagatore>1</pay_i:civicoPagatore>
                         <pay_i:capPagatore>00100</pay_i:capPagatore>
                         <pay_i:localitaPagatore>Roma</pay_i:localitaPagatore>
                         <pay_i:provinciaPagatore>RM</pay_i:provinciaPagatore>
                         <pay_i:nazionePagatore>IT</pay_i:nazionePagatore>
                         <pay_i:e-mailPagatore>marina.verdi@mail.com</pay_i:e-mailPagatore>
                 	</pay_i:soggettoPagatore>
                     <pay_i:enteBeneficiario>
                         <pay_i:identificativoUnivocoBeneficiario>
                             <pay_i:tipoIdentificativoUnivoco>G</pay_i:tipoIdentificativoUnivoco>
                             <pay_i:codiceIdentificativoUnivoco>${creditorInstitutionCode}</pay_i:codiceIdentificativoUnivoco>
                         </pay_i:identificativoUnivocoBeneficiario>
                         <pay_i:denominazioneBeneficiario>PagoPA S.p.A</pay_i:denominazioneBeneficiario>
                         <pay_i:codiceUnitOperBeneficiario>123</pay_i:codiceUnitOperBeneficiario>
                         <pay_i:denomUnitOperBeneficiario>XXX</pay_i:denomUnitOperBeneficiario>
                         <pay_i:indirizzoBeneficiario>Piazza Colonna</pay_i:indirizzoBeneficiario>
                         <pay_i:civicoBeneficiario>370</pay_i:civicoBeneficiario>
                         <pay_i:capBeneficiario>00187</pay_i:capBeneficiario>
                         <pay_i:localitaBeneficiario>Roma</pay_i:localitaBeneficiario>
                         <pay_i:provinciaBeneficiario>RM</pay_i:provinciaBeneficiario>
                         <pay_i:nazioneBeneficiario>IT</pay_i:nazioneBeneficiario>
                     </pay_i:enteBeneficiario>
                     <pay_i:datiVersamento>
                         <pay_i:dataEsecuzionePagamento>${dateString}</pay_i:dataEsecuzionePagamento>
                         <pay_i:importoTotaleDaVersare>${amount}</pay_i:importoTotaleDaVersare>
                         <pay_i:tipoVersamento>BBT</pay_i:tipoVersamento>
                         <pay_i:identificativoUnivocoVersamento>${iuv}</pay_i:identificativoUnivocoVersamento>
                         <pay_i:codiceContestoPagamento>${ccp}</pay_i:codiceContestoPagamento>
                         <pay_i:ibanAddebito>IT12A1234512345123456789012</pay_i:ibanAddebito>
                         <pay_i:bicAddebito>ARTIITM1045</pay_i:bicAddebito>
                         <pay_i:firmaRicevuta>0</pay_i:firmaRicevuta>
                         <pay_i:datiSingoloVersamento>
                                 <pay_i:importoSingoloVersamento>${amount}</pay_i:importoSingoloVersamento>
                                 <pay_i:commissioneCaricoPA>${fee}</pay_i:commissioneCaricoPA>
                                 <pay_i:ibanAccredito>${creditorIban}</pay_i:ibanAccredito>
                                 <pay_i:bicAccredito>ARTIITM1050</pay_i:bicAccredito>
                                 <pay_i:ibanAppoggio>${creditorIban}</pay_i:ibanAppoggio>
                                 <pay_i:bicAppoggio>ARTIITM1050</pay_i:bicAppoggio>
                                 <pay_i:credenzialiPagatore>CP1.1</pay_i:credenzialiPagatore>
                                 <pay_i:causaleVersamento>/RFB/${iuv}/${amount}/TXT/DEBITORE/VRDMRN72A12H501Z</pay_i:causaleVersamento>
                                 <pay_i:datiSpecificiRiscossione>9/0301109AP</pay_i:datiSpecificiRiscossione>
                             </pay_i:datiSingoloVersamento>
                     </pay_i:datiVersamento>
                 </pay_i:RPT>`;

  return encoding.b64encode(rpt);
}

function getRandomMonetaryAmount(min, max) {
    let value = Math.random() * (max - min) + min;
    return value.toFixed(2);
}
