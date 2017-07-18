package br.com.evertontrindade.nosql.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.evertontrindade.nosql.model.Profile;

/**
 * Created by unik on 11/10/16.
 */
public interface ProfileRepository extends JpaRepository<Profile, Long> {

}