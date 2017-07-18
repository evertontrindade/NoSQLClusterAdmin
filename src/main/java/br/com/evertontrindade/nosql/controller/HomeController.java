package br.com.evertontrindade.nosql.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Created by unik on 17/10/16.
 */
@Controller
public class HomeController {

    private static final Logger LOGGER = LoggerFactory.getLogger(HomeController.class);

    @RequestMapping("/")
    public String getHomePage() {
        LOGGER.debug("Getting home page");

        Authentication auth =  SecurityContextHolder.getContext().getAuthentication();

        if ( (auth != null) && (!"anonymousUser".equals(auth.getPrincipal())) ) {
            return "index";
        } else {
            return "redirect:/login";
        }

    }

}