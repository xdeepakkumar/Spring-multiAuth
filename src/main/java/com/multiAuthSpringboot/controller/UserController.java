package com.multiAuthSpringboot.controller;

import java.security.Principal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.multiAuthSpringboot.common.UserConstant;
import com.multiAuthSpringboot.entity.User;
import com.multiAuthSpringboot.repository.UserRepository;

@RestController
@RequestMapping("/user")
public class UserController {
	
	@Autowired
	private UserRepository userRepository;
	
	@Autowired
	private BCryptPasswordEncoder bCryptPasswordEncoder;
	

	@PostMapping("/join")
	public String joinGroup(@RequestBody User user) {
		user.setRoles(UserConstant.DEFAULT_ROLE);
		String encodedPassword = bCryptPasswordEncoder.encode(user.getPassword());
		user.setPassword(encodedPassword);
		userRepository.save(user);
		return "Hi "+user.getUserName() + " Welcome to the Group";
	}
	
	//assiging other roles
	
	@GetMapping("/access/{userId}/{userRole}")
	@PreAuthorize("hasAuthority('ROLE_ADMIN') or hasAuthority('ROLE_MODERATOR')")
	public String giveAccessToUser(@PathVariable int userId, @PathVariable String userRole, Principal principal) {
		User user = userRepository.findById(userId).get();
		List<String> activeRoles = getRolesByLoggedInUser(principal);
		String newRole = "";
		if(activeRoles.contains(userRole)) {
			newRole = user.getRoles() + ","+userRole;
			user.setRoles(newRole);
		}
		userRepository.save(user);
		return "Hi " + user.getUserName() + " new role assigned to you by " + principal.getName();
	}
	
	//demo
	@GetMapping
	@Secured("ROLE_ADMIN")
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	public List<User> loadUsers(){
		return userRepository.findAll();
	}
	@PreAuthorize("hasAuthority('ROLE_ADMIN')")
	@GetMapping("/test")
	public String testUserAccess() {
		return "user can only access this";
	}
	
	
	private List<String> getRolesByLoggedInUser(Principal principal){
		String roles = getLoggedInUser(principal).getRoles();
		List<String> assignedRole = Arrays.stream(roles.split(",")).collect(Collectors.toList());
		if(assignedRole.contains("ROLE_ADMIN")) {
			return Arrays.stream(UserConstant.ADMIN_ACCESS).collect(Collectors.toList());
		}
		if(assignedRole.contains("ROLE_MODERATOR")) {
			return Arrays.stream(UserConstant.MODERATOR_ACCESS).collect(Collectors.toList());
		}
		return Collections.emptyList(); 
	}
	
	//get the loggedin user
	private User getLoggedInUser(Principal principal) {
		return userRepository.findByUserName(principal.getName()).get();
	}
}
