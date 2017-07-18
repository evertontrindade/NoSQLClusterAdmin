package br.com.evertontrindade.nosql.helper;

import br.com.evertontrindade.nosql.model.CurrentUser;
import br.com.evertontrindade.nosql.model.User;
import br.com.evertontrindade.nosql.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

/**
 * Created by unik on 14/10/16.
 */
@Service
public class CurrentUserDetailService implements UserDetailsService {

    private static final Logger LOGGER = LoggerFactory.getLogger(CurrentUserDetailService.class);

    @Autowired
    private UserRepository userRepository;

    @Override
    public CurrentUser loadUserByUsername(String email) throws UsernameNotFoundException {

        LOGGER.debug("Authenticating user with email={}", email.replaceFirst("@.*", "@***"));

        User user = userRepository.findOneByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException(String.format("User with email=%s was not found", email)));

        return new CurrentUser(user);
    }


}
