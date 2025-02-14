package com.ephr;

// import com.auth0.AuthenticationController;
// import com.auth0.IdentityVerificationException;
// import com.auth0.Tokens;
import com.auth0.jwt.JWT;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.io.IOException;

public class Auth0Helper {

    // private static final String CLIENT_ID = "bXvPocN2anepQPNrcpAmG1sGxmasqZXU"; // Replace with your Auth0 Client ID
    // private static final String CLIENT_SECRET = "Blr82K6nYaT96Jcy0P05XgIEcJ2ew6pm0jIpiUlhh5L1sO9P8p27Fn5agSHrIXBa"; // Replace with your Auth0 Client Secret
    // private static final String DOMAIN = "dev-vf4tegc6nvpgpbkj.us.auth0.com"; // Replace with your Auth0 domain

    // private static AuthenticationController authController;

    // static {
    //     authController = AuthenticationController.newBuilder(DOMAIN, CLIENT_ID, CLIENT_SECRET)
    //             .build();
    // }

    // public static String buildLoginUrl() {
    //     // Redirect the user to the Auth0-hosted login page
    //     return authController.buildAuthorizeUrl(null, "http://localhost/callback")
    //             .withScope("openid profile email") // Request user profile and email
    //             .build();
    // }

    // public static Tokens handleCallback(String callbackUrl) throws IdentityVerificationException {
    //     // Handle the callback and retrieve the tokens
    //     return authController.handle(callbackUrl);
    // }

    // public static DecodedJWT verifyToken(String idToken) {
    //     // Verify the ID token
    //     return JWT.decode(idToken);
    // }
}