package it.gov.pagopa.wispconverter.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller()
@RequestMapping("/static")
public class StaticPagesController {

    @GetMapping(path = "/error")
    public String error() {
        return "error";
    }
}
