package br.com.evertontrindade.nosql.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.evertontrindade.nosql.model.User;

/**
 * Created by unik on 11/10/16.
 */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findOneByEmail(String email);
    List<User> findAllByProfileId(Long profileId);
}