package org.example.services;

import org.example.models.UserProfile;
import java.time.LocalDateTime;

public class UserProfileService {
    private static final UserProfileService INSTANCE = new UserProfileService();
    private final AuthService authService = AuthService.getInstance();

    private UserProfileService() {}

    public static UserProfileService getInstance() {
        return INSTANCE;
    }

    public UserProfile getCurrentUser() {
        String email = authService.getCurrentSessionEmail();
        if (email == null) {
            return null; // Not logged in
        }

        String name = authService.getDisplayName(email);
        LocalDateTime regDate = authService.getRegistrationDate(email);
        LocalDateTime lastAccess = authService.getLastAccess(email);
        
        // Simulating ID based on hash of email
        String id = "USR-" + Math.abs(email.hashCode());

        return new UserProfile(id, name, email, regDate, lastAccess, null);
    }

    public boolean updateProfile(String newName, String newEmail) {
        String currentEmail = authService.getCurrentSessionEmail();
        if (currentEmail == null) return false;

        boolean updated = false;

        if (newName != null && !newName.isBlank() && !newName.equals(authService.getDisplayName(currentEmail))) {
            authService.updateDisplayName(currentEmail, newName);
            updated = true;
        }

        if (newEmail != null && !newEmail.isBlank() && !newEmail.equals(currentEmail)) {
            if (authService.updateEmail(currentEmail, newEmail)) {
                updated = true;
            } else {
                return false; // Email update failed (probably exists)
            }
        }

        return updated;
    }
}
