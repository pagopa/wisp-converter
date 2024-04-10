const {makeNumericalString} = require("util")

let nodoInviaRPT = `
<soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/" xmlns:ppt="http://ws.pagamenti.telematici.gov/ppthead" xmlns:ws="http://ws.pagamenti.telematici.gov/">
    <soapenv:Header>
        <ppt:intestazionePPT>
            <identificativoIntermediarioPA>${creditorInstitutionBroker}</identificativoIntermediarioPA>
            <identificativoStazioneIntermediarioPA>${station}</identificativoStazioneIntermediarioPA>
            <identificativoDominio>${creditorInstitution}</identificativoDominio>
            <identificativoUnivocoVersamento>${makeNumericalString}</identificativoUnivocoVersamento>
            <codiceContestoPagamento>CCD01</codiceContestoPagamento>
        </ppt:intestazionePPT>
    </soapenv:Header>
    <soapenv:Body>
        <ws:nodoInviaRPT>
            <password>pwdpwdpwd</password>
            <identificativoPSP>${psp}</identificativoPSP>
            <identificativoIntermediarioPSP>${pspBroker}</identificativoIntermediarioPSP>
            <identificativoCanale>${channel}</identificativoCanale>
            <tipoFirma></tipoFirma>
            <rpt>${atob(rpt)}</rpt>
        </ws:nodoInviaRPT>
    </soapenv:Body>
</soapenv:Envelope>
`;

let rpt = `
<pay_i:RPT xmlns:pay_i="http://www.digitpa.gov.it/schemas/2011/Pagamenti/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://www.digitpa.gov.it/schemas/2011/Pagamenti/ PagInf_RPT_RT_6_2_0.xsd ">
    <pay_i:versioneOggetto>6.0</pay_i:versioneOggetto>
    <pay_i:dominio>
        <pay_i:identificativoDominio>66666666666</pay_i:identificativoDominio>
        <pay_i:identificativoStazioneRichiedente>66666666666_05</pay_i:identificativoStazioneRichiedente>
    </pay_i:dominio>
    <pay_i:identificativoMessaggioRichiesta>idMsgRichiesta</pay_i:identificativoMessaggioRichiesta>
    <pay_i:dataOraMessaggioRichiesta>2016-09-16T11:24:10</pay_i:dataOraMessaggioRichiesta>
    <pay_i:autenticazioneSoggetto>CNS</pay_i:autenticazioneSoggetto>
    <pay_i:soggettoVersante>
        <pay_i:identificativoUnivocoVersante>
            <pay_i:tipoIdentificativoUnivoco>F</pay_i:tipoIdentificativoUnivoco>
            <pay_i:codiceIdentificativoUnivoco>RCCGLD09P09H501E</pay_i:codiceIdentificativoUnivoco>
        </pay_i:identificativoUnivocoVersante>
        <pay_i:anagraficaVersante>Gesualdo;Riccitelli</pay_i:anagraficaVersante>
        <pay_i:indirizzoVersante>via del gesu</pay_i:indirizzoVersante>
        <pay_i:civicoVersante>11</pay_i:civicoVersante>
        <pay_i:capVersante>00186</pay_i:capVersante>
        <pay_i:localitaVersante>Roma</pay_i:localitaVersante>
        <pay_i:provinciaVersante>RM</pay_i:provinciaVersante>
        <pay_i:nazioneVersante>IT</pay_i:nazioneVersante>
    </pay_i:soggettoVersante>
    <pay_i:soggettoPagatore>
        <pay_i:identificativoUnivocoPagatore>
            <pay_i:tipoIdentificativoUnivoco>F</pay_i:tipoIdentificativoUnivoco>
            <pay_i:codiceIdentificativoUnivoco>RCCGLD09P09H501E</pay_i:codiceIdentificativoUnivoco>
        </pay_i:identificativoUnivocoPagatore>
        <pay_i:anagraficaPagatore>Gesualdo;Riccitelli</pay_i:anagraficaPagatore>
        <pay_i:indirizzoPagatore>via del gesu</pay_i:indirizzoPagatore>
        <pay_i:civicoPagatore>11</pay_i:civicoPagatore>
        <pay_i:capPagatore>00186</pay_i:capPagatore>
        <pay_i:localitaPagatore>Roma</pay_i:localitaPagatore>
        <pay_i:provinciaPagatore>RM</pay_i:provinciaPagatore>
        <pay_i:nazionePagatore>IT</pay_i:nazionePagatore>
    </pay_i:soggettoPagatore>
    <pay_i:enteBeneficiario>
        <pay_i:identificativoUnivocoBeneficiario>
            <pay_i:tipoIdentificativoUnivoco>G</pay_i:tipoIdentificativoUnivoco>
            <pay_i:codiceIdentificativoUnivoco>11111111117</pay_i:codiceIdentificativoUnivoco>
        </pay_i:identificativoUnivocoBeneficiario>
        <pay_i:denominazioneBeneficiario>AZIENDA XXX</pay_i:denominazioneBeneficiario>
        <pay_i:codiceUnitOperBeneficiario>123</pay_i:codiceUnitOperBeneficiario>
        <pay_i:denomUnitOperBeneficiario>XXX</pay_i:denomUnitOperBeneficiario>
        <pay_i:indirizzoBeneficiario>IndirizzoBeneficiario</pay_i:indirizzoBeneficiario>
        <pay_i:civicoBeneficiario>123</pay_i:civicoBeneficiario>
        <pay_i:capBeneficiario>00123</pay_i:capBeneficiario>
        <pay_i:localitaBeneficiario>Roma</pay_i:localitaBeneficiario>
        <pay_i:provinciaBeneficiario>RM</pay_i:provinciaBeneficiario>
        <pay_i:nazioneBeneficiario>IT</pay_i:nazioneBeneficiario>
    </pay_i:enteBeneficiario>
    <pay_i:datiVersamento>
        <pay_i:dataEsecuzionePagamento>2016-09-16</pay_i:dataEsecuzionePagamento>
        <pay_i:importoTotaleDaVersare>10.00</pay_i:importoTotaleDaVersare>
        <pay_i:tipoVersamento>PO</pay_i:tipoVersamento>
        <pay_i:identificativoUnivocoVersamento>141577487701211</pay_i:identificativoUnivocoVersamento>
        <pay_i:codiceContestoPagamento>CCD01</pay_i:codiceContestoPagamento>
        <pay_i:ibanAddebito>IT45R0760103200000000001016</pay_i:ibanAddebito>
        <pay_i:bicAddebito>ARTIITM1045</pay_i:bicAddebito>
        <pay_i:firmaRicevuta>0</pay_i:firmaRicevuta>
        <pay_i:datiSingoloVersamento>
            <pay_i:importoSingoloVersamento>10.00</pay_i:importoSingoloVersamento>
            <pay_i:commissioneCaricoPA>1.00</pay_i:commissioneCaricoPA>
            <pay_i:bicAccredito>ARTIITM1050</pay_i:bicAccredito>
            <pay_i:ibanAppoggio>IT45R0760103200000000001016</pay_i:ibanAppoggio>
            <pay_i:bicAppoggio>ARTIITM1050</pay_i:bicAppoggio>
            <pay_i:credenzialiPagatore>CP1.1</pay_i:credenzialiPagatore>
            <pay_i:causaleVersamento>pagamento fotocopie pratica RPT</pay_i:causaleVersamento>
            <pay_i:datiSpecificiRiscossione>1/abc</pay_i:datiSpecificiRiscossione>
            <pay_i:datiMarcaBolloDigitale>
                <pay_i:tipoBollo>01</pay_i:tipoBollo>
                <pay_i:hashDocumento>wHpFSLCGZjIvNSXxqtGbxg7275t446DRTk5ZrsdUQ6E=</pay_i:hashDocumento>
                <pay_i:provinciaResidenza>MI</pay_i:provinciaResidenza>
            </pay_i:datiMarcaBolloDigitale>
        </pay_i:datiSingoloVersamento>
    </pay_i:datiVersamento>
</pay_i:RPT>
`

module.exports = {nodoInviaRPT, rpt}