package org.example.services;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Autenticación en memoria (simulación sin base de datos).
 */
public class AuthService {

    private static final AuthService INSTANCE = new AuthService();

    private final Map<String, String> credentials = new HashMap<>();
    private final Map<String, String> displayNames = new HashMap<>();
    private final Map<String, LocalDateTime> registrationDates = new HashMap<>();
    private final Map<String, LocalDateTime> lastAccessDates = new HashMap<>();

    private String currentSessionEmail;

    private AuthService() {
    }

    public static AuthService getInstance() {
        return INSTANCE;
    }

    public enum LoginResult {
        SUCCESS,
        USER_NOT_FOUND,
        WRONG_PASSWORD,
        EMPTY_FIELDS
    }

    public enum RegisterResult {
        SUCCESS,
        EMAIL_EXISTS,
        PASSWORD_MISMATCH,
        EMPTY_FIELDS
    }

    public LoginResult login(String emailOrUser, String password) {
        String key = normalize(emailOrUser);

        if (key.isEmpty() || password == null || password.isBlank()) {
            return LoginResult.EMPTY_FIELDS;
        }
        if (!credentials.containsKey(key)) {
            return LoginResult.USER_NOT_FOUND;
        }
        if (!credentials.get(key).equals(password)) {
            return LoginResult.WRONG_PASSWORD;
        }

        setCurrentSession(key);
        return LoginResult.SUCCESS;
    }

    public RegisterResult register(String email, String password, String confirmPassword, String name) {
        String key = normalize(email);

        if (key.isEmpty() || password == null || password.isBlank()
                || confirmPassword == null || confirmPassword.isBlank()) {
            return RegisterResult.EMPTY_FIELDS;
        }
        if (!password.equals(confirmPassword)) {
            return RegisterResult.PASSWORD_MISMATCH;
        }
        if (credentials.containsKey(key)) {
            return RegisterResult.EMAIL_EXISTS;
        }

        credentials.put(key, password);
        displayNames.put(key, name == null || name.isBlank() ? key : name.trim());
        registrationDates.putIfAbsent(key, LocalDateTime.now());
        lastAccessDates.put(key, LocalDateTime.now());
        return RegisterResult.SUCCESS;
    }

    public boolean userExists(String email) {
        return credentials.containsKey(normalize(email));
    }

    public void setCurrentSession(String email) {
        String key = normalize(email);
        if (key.isEmpty()) {
            return;
        }
        currentSessionEmail = key;
        lastAccessDates.put(key, LocalDateTime.now());
        registrationDates.putIfAbsent(key, LocalDateTime.now());
    }

    public void logout() {
        currentSessionEmail = null;
    }

    public String getCurrentSessionEmail() {
        return currentSessionEmail;
    }

    public String getDisplayName(String email) {
        String key = normalize(email);
        return displayNames.getOrDefault(key, key);
    }

    public String getDisplayNameForCurrentSession() {
        if (currentSessionEmail == null) {
            return "Usuario";
        }
        return getDisplayName(currentSessionEmail);
    }

    public LocalDateTime getRegistrationDate(String email) {
        String key = normalize(email);
        return registrationDates.getOrDefault(key, LocalDateTime.now());
    }

    public LocalDateTime getLastAccess(String email) {
        String key = normalize(email);
        return lastAccessDates.getOrDefault(key, LocalDateTime.now());
    }

    public boolean updateDisplayName(String email, String newName) {
        String key = normalize(email);
        if (!credentials.containsKey(key) || newName == null || newName.isBlank()) {
            return false;
        }
        displayNames.put(key, newName.trim());
        return true;
    }

    public boolean updateEmail(String currentEmail, String newEmail) {
        String oldKey = normalize(currentEmail);
        String newKey = normalize(newEmail);

        if (!credentials.containsKey(oldKey) || newKey.isEmpty()) {
            return false;
        }
        if (credentials.containsKey(newKey) && !oldKey.equals(newKey)) {
            return false;
        }

        String password = credentials.remove(oldKey);
        credentials.put(newKey, password);

        displayNames.put(newKey, displayNames.remove(oldKey));
        registrationDates.put(newKey, registrationDates.remove(oldKey));
        lastAccessDates.put(newKey, lastAccessDates.remove(oldKey));

        if (oldKey.equals(currentSessionEmail)) {
            currentSessionEmail = newKey;
        }
        return true;
    }

    private String normalize(String email) {
        if (email == null) {
            return "";
        }
        return email.trim().toLowerCase();
    }
}

