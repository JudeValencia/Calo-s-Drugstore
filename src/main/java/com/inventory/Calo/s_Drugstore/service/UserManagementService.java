package com.inventory.Calo.s_Drugstore.service;

import com.inventory.Calo.s_Drugstore.entity.User;
import com.inventory.Calo.s_Drugstore.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * UserManagementService - Handles admin operations for managing staff accounts
 *
 * WHAT THIS FILE DOES:
 * - Create new staff accounts (from Settings menu)
 * - Reset passwords for existing staff
 * - Manage user accounts (activate/deactivate)
 * - Get list of all users
 *
 * WHO CAN USE THIS:
 * - Only ADMIN users should access these functions
 * - Regular staff cannot create/modify other accounts
 */

@Service  // Spring Boot will manage this service
public class UserManagementService {

    // DEPENDENCIES (things this service needs)

    @Autowired
    private UserRepository userRepository;  // To access the database

    // Password encoder to encrypt passwords (same one used in login)
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();


    /**
     * CREATE NEW STAFF ACCOUNT
     *
     * Called when admin clicks "Create Staff Account" in settings
     *
     * HOW IT WORKS:
     * 1. Check if username already exists
     * 2. Check if email already exists
     * 3. Encrypt the password
     * 4. Save the new user to database
     *
     * @param username - The login username
     * @param email - Staff email address
     * @param password - Plain text password (will be encrypted)
     * @param fullName - Staff member's full name
     * @param role - ADMIN, MANAGER, or STAFF
     * @return The created User object
     * @throws RuntimeException if username/email already exists
     */
    public User createStaffAccount(String username, String email, String password,
                                   String fullName, String role) {

        // STEP 1: Check if username already exists
        if (userRepository.existsByUsername(username)) {
            throw new RuntimeException("Username already exists");
        }

        // STEP 2: Check if email already exists
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("Email already exists");
        }

        // STEP 3: Create new User object
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);

        // IMPORTANT: Encrypt password before saving!
        user.setPassword(passwordEncoder.encode(password));

        user.setFullName(fullName);
        user.setRole(role);
        user.setActive(true);  // Account is active by default

        // STEP 4: Save to database and return
        return userRepository.save(user);
    }


    /**
     * RESET USER PASSWORD
     *
     * Called when admin clicks "Reset Password" in settings
     *
     * HOW IT WORKS:
     * 1. Find user by username
     * 2. If found, encrypt new password
     * 3. Update user in database
     *
     * @param username - The username to reset password for
     * @param newPassword - The new password (plain text)
     * @return true if successful, false if user not found
     */
    public boolean resetPassword(String username, String newPassword) {

        // STEP 1: Try to find the user
        Optional<User> userOpt = userRepository.findByUsername(username);

        // STEP 2: If user exists, update password
        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Encrypt the new password
            user.setPassword(passwordEncoder.encode(newPassword));

            // Save to database
            userRepository.save(user);

            return true;  // Success!
        }

        return false;  // User not found
    }


    /**
     * GET ALL USERS
     *
     * Returns a list of all staff members in the system
     * Useful for displaying a user management table (future feature)
     *
     * @return List of all User objects
     */
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }


    /**
     * GET USER BY ID
     *
     * Find a specific user by their ID
     *
     * @param userId - The user's database ID
     * @return Optional containing User if found, empty if not
     */
    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }


    /**
     * ACTIVATE/DEACTIVATE USER ACCOUNT
     *
     * Turn a user account on or off without deleting it
     * Inactive users cannot login
     *
     * @param userId - The user's database ID
     * @param active - true to activate, false to deactivate
     * @return true if successful, false if user not found
     */
    public boolean setUserActiveStatus(Long userId, boolean active) {
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            user.setActive(active);
            userRepository.save(user);
            return true;
        }

        return false;
    }


    /**
     * DELETE USER ACCOUNT
     *
     * Permanently remove a user from the system
     * USE WITH CAUTION - this cannot be undone!
     *
     * @param userId - The user's database ID
     * @return true if deleted, false if user not found
     */
    public boolean deleteUser(Long userId) {
        if (userRepository.existsById(userId)) {
            userRepository.deleteById(userId);
            return true;
        }
        return false;
    }


    /**
     * UPDATE USER INFORMATION
     *
     * Update a user's profile information (not password)
     *
     * @param userId - The user's database ID
     * @param fullName - New full name
     * @param email - New email
     * @param role - New role
     * @return Updated User object, or null if not found
     */
    public User updateUserInfo(Long userId, String fullName, String email, String role) {
        Optional<User> userOpt = userRepository.findById(userId);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // Update fields
            user.setFullName(fullName);
            user.setEmail(email);
            user.setRole(role);

            // Save and return
            return userRepository.save(user);
        }

        return null;
    }


    /**
     * COUNT TOTAL STAFF MEMBERS
     *
     * Returns how many users are in the system
     * Useful for dashboard statistics
     *
     * @return Total number of users
     */
    public long getTotalStaffCount() {
        return userRepository.count();
    }


    /**
     * COUNT ACTIVE STAFF MEMBERS
     *
     * Returns how many users are currently active
     *
     * @return Number of active users
     */
    public long getActiveStaffCount() {
        return userRepository.findAll()
                .stream()
                .filter(User::isActive)
                .count();
    }
}
