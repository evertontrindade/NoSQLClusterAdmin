package br.com.evertontrindade.nosql.helper;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;

import br.com.evertontrindade.nosql.model.CurrentUser;
import br.com.evertontrindade.nosql.model.Profile;

@Service
public class CurrentUserService {

	public boolean canAccessUser(CurrentUser currentUser, Long userId) {
		return currentUser != null && (currentUser.getProfile().getId() == Profile.MASTER || currentUser.getId().equals(userId));
	}

	public boolean canAccessUser(CurrentUser currentUser, String... roles) {
		boolean canAccess = false;
		
		if (currentUser != null) {
			for (String role : roles) {
				for (GrantedAuthority g : currentUser.getAuthorities()) {
					if (g.getAuthority().equals(role)) {
						canAccess = true;
						break;
					}
				}
			}
		}
		
		return canAccess;
	}
}
