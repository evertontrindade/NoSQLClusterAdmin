package br.com.evertontrindade.nosql.controller;

import java.util.ArrayList;
import java.util.List;

import javax.transaction.Transactional;

import br.com.evertontrindade.nosql.model.User;
import br.com.evertontrindade.nosql.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import br.com.evertontrindade.nosql.model.Profile;
import br.com.evertontrindade.nosql.model.Role;
import br.com.evertontrindade.nosql.repository.ProfileRepository;
import br.com.evertontrindade.nosql.repository.RoleRepository;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * Created by unik on 11/10/16.
 */
@Controller
public class ProfileController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProfileController.class);

    @Autowired
    private ProfileRepository profileRepository;
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRepository userRepository;

    @PreAuthorize("hasAnyAuthority('INSERT_PROFILE')")
    @RequestMapping("profile/new")
    public String newUser(Model model) {
        model.addAttribute("profile", new Profile());
        model.addAttribute("rolesAvailable", roleRepository.findAll());
        return "profileform";
    }
    
    @PreAuthorize("hasAnyAuthority('INSERT_PROFILE', 'UPDATE_PROFILE')")
    @RequestMapping(value = "profile", method = RequestMethod.POST)
    @Transactional
    public String save(Profile profile) {
    	Profile profileDatabase = null;
    	
    	if (profile.getId() != null && profile.getId() != 0) {
    		profileDatabase = profileRepository.findOne(profile.getId());
    	} else {
    		profileDatabase = new Profile();
    	}
    	
    	profileDatabase.setName(profile.getName());

    	if (profileDatabase.getRoles() == null) {
    		profileDatabase.setRoles(new ArrayList<>());
    	} else {
    		profileDatabase.getRoles().clear();
    	}

    	String[] arr = profile.getRolesAdded().split(",");
    	
    	for (String item : arr) {
			profileDatabase.getRoles().add(new Role(Long.valueOf(item)));
		}

    	LOGGER.info(profileDatabase.toString());
    	profileRepository.save(profileDatabase);
    	
        return "redirect:/profiles";

    }

    @PreAuthorize("hasAnyAuthority('UPDATE_PROFILE')")
    @RequestMapping("profile/edit/{id}")
    public String edit(@PathVariable Long id,  Model model) {
    	Profile profile = profileRepository.findOne(id);
    	List<Role> rolesAvailable = roleRepository.findAll();
    	profile.setRolesAdded("");
    	for (Role role : profile.getRoles()) {
			rolesAvailable.remove(role);
	    	profile.setRolesAdded(profile.getRolesAdded() + role.getId().toString() + ",");
		}
    	profile.setRolesAdded(profile.getRolesAdded().substring(0, profile.getRolesAdded().length() - 1 ));

    	LOGGER.info(profile.toString());
    	model.addAttribute("profile", profile);
        model.addAttribute("rolesAvailable", rolesAvailable);
        return "profileform";

    }

    @PreAuthorize("hasAnyAuthority('DELETE_PROFILE')")
    @RequestMapping("profile/delete/{id}")
    public String delete(@PathVariable Long id,  Model model) {
    	profileRepository.delete(id);
        return "redirect:/profiles";

    }

    @PreAuthorize("hasAuthority('LIST_PROFILE')")
    @RequestMapping("profiles")
    public String listAll(Model model) {
    	model.addAttribute("profiles", profileRepository.findAll());
        return "profilelist";
    }

    @RequestMapping("profile/validate-users/{id}")
    @ResponseBody
    public Boolean validateUsersByProfile(@PathVariable Long id) {
        List<User> users = userRepository.findAllByProfileId(id);
        return users != null && !users.isEmpty();
    }

}
