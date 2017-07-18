package br.com.evertontrindade.nosql.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import br.com.evertontrindade.nosql.model.Role;

/**
 * Created by unik on 11/10/16.
 */
public interface RoleRepository extends JpaRepository<Role, Long> {

}