package br.com.evertontrindade.nosql.model;

import java.util.List;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.Transient;

import lombok.Data;

@Entity
@Data
public class Profile {

	public static final int MASTER = 1;
	
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name="profile_role", joinColumns={@JoinColumn(name="profile_id")}, inverseJoinColumns={@JoinColumn(name="role_id")})
    List<Role> roles;
    
    @Transient
    private String rolesAdded;
    
}
