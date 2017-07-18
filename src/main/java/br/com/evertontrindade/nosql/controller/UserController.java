package br.com.evertontrindade.nosql.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import br.com.evertontrindade.nosql.model.CurrentUser;
import br.com.evertontrindade.nosql.model.User;
import br.com.evertontrindade.nosql.repository.ProfileRepository;
import br.com.evertontrindade.nosql.repository.UserRepository;

/**
 * Created by unik on 11/10/16.
 */
@Controller
public class UserController {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProfileRepository profileRepository;

    @PreAuthorize("hasAnyAuthority('INSERT_USER')")
    @RequestMapping("user/new")
    public String newUser(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("type", "NEW");
        model.addAttribute("profiles", profileRepository.findAll());
        return "userform";
    }
    
    @PreAuthorize("hasAnyAuthority('INSERT_USER')")
    @RequestMapping(value = "user", method = RequestMethod.POST)
    public String save(User user) {
    	User userDatabase = null;
    	
    	if (user.getId() != null && user.getId() != 0) {
    		userDatabase = userRepository.findOne(user.getId());
    	} else {
    		userDatabase = new User();
    	}
    	
    	userDatabase.setEmail(user.getEmail());
        if (user.getName() != null) {
            userDatabase.setName(user.getName());
        }
    	if (!StringUtils.isEmpty(user.getPasswordHash()) && !StringUtils.isEmpty(user.getPasswordRepeated()) ) {
    		userDatabase.setPasswordHash(new BCryptPasswordEncoder().encode(user.getPasswordHash()));
    	}
    	userDatabase.setEmail(user.getEmail());

        if (user.getProfile() != null) {
            userDatabase.setProfile(user.getProfile());
        }

    	userRepository.save(userDatabase);
    	
        return "redirect:/users";

    }

    @PreAuthorize("hasAnyAuthority('UPDATE_USER')")
    @RequestMapping("user/edit/{id}")
    public String edit(@PathVariable Long id,  Model model) {
        model.addAttribute("user", userRepository.findOne(id));
        model.addAttribute("type", "EDIT");
        model.addAttribute("profiles", profileRepository.findAll());
        return "userform";

    }

    @RequestMapping("user/my-profile")
    public String myProfile(Model model) {
    	Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    	CurrentUser user = (CurrentUser)auth.getPrincipal();
        model.addAttribute("user", userRepository.findOne(user.getUser().getId()));
        model.addAttribute("profiles", profileRepository.findAll());
        model.addAttribute("type", "PROFILE");
        return "userform";

    }

    
    @PreAuthorize("hasAnyAuthority('DELETE_USER')")
    @RequestMapping("user/delete/{id}")
    public String delete(@PathVariable Long id,  Model model) {
    	userRepository.delete(id);
        return "redirect:/users";

    }

    @PreAuthorize("hasAuthority('LIST_USER')")
    @RequestMapping("users")
    public String listAll(Model model) {
    	model.addAttribute("users", userRepository.findAll());

        return "userlist";
    }

    
}
