
package com.personalai.personal_ai;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.io.Console;

public class PasswordHashGenerator {

    public static void main(String[] args) {
        Console console = System.console();

        if (console == null) {
            System.out.println(
                "Run this class in a terminal that supports password input."
            );
            return;
        }

        char[] password = console.readPassword("Enter owner password: ");
        char[] confirm = console.readPassword("Confirm owner password: ");

        if (!java.util.Arrays.equals(password, confirm)) {
            System.out.println("Passwords do not match.");
            return;
        }

        String hash = new BCryptPasswordEncoder().encode(
            new String(password)
        );

        System.out.println("BCrypt hash:");
        System.out.println(hash);

        java.util.Arrays.fill(password, '\0');
        java.util.Arrays.fill(confirm, '\0');
    }
}
