package it.gov.pagopa.wispconverter.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller()
@RequestMapping("/static")
public class StaticPagesController {

    @RequestMapping("/error")
    public String error() {
        return "error";
    }
}
