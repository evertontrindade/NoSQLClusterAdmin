package br.com.evertontrindade.nosql.model;

import java.util.ArrayList;
import java.util.List;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Created by unik on 11/10/16.
 */
public class CurrentUser extends org.springframework.security.core.userdetails.User {

	private static final long serialVersionUID = -2650625556277072907L;
	private User user;

    public CurrentUser(User user) {
        super(user.getEmail(), user.getPasswordHash(), getRoles(user));
        this.user = user;
    }
    
    private static List<GrantedAuthority> getRoles(User user) {
    	List<GrantedAuthority> list = new ArrayList<>();
    	for (Role role : user.getProfile().getRoles()) {
    		list.add(new SimpleGrantedAuthority(role.getName()));
		}
    	
    	return list;

    }

    public Long getId() {
        return user.getId();
    }

    public Profile getProfile() {
        return user.getProfile();
    }
    
    public User getUser() {
		return user;
	}
    
}
