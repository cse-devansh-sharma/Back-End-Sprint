package com.cap.identity.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.cap.identity.entity.User;
import com.cap.identity.enums.Status;

public interface UserRepository extends JpaRepository<User,Long>{
	
	public Optional<User> findByEmail(String email);
	
	public Optional<User> findByEmployeeCode(String employeeCode);
	
	public List<User> findByEmailAndStatus(String email,Status status);

}
