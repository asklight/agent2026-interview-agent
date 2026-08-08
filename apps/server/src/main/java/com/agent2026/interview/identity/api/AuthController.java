package com.agent2026.interview.identity.api;

import com.agent2026.interview.common.Result;
import com.agent2026.interview.identity.application.AuthApplicationService;
import com.agent2026.interview.identity.application.AuthenticationResult;
import com.agent2026.interview.identity.security.CurrentUserProvider;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthApplicationService auth;
    private final RefreshCookieService cookies;
    private final CurrentUserProvider currentUser;

    public AuthController(AuthApplicationService auth, RefreshCookieService cookies,
                          CurrentUserProvider currentUser) {
        this.auth = auth;
        this.cookies = cookies;
        this.currentUser = currentUser;
    }

    @PostMapping("/register")
    public Result<AuthResponse> register(@Valid @RequestBody AuthCredentialsRequest request,
                                         HttpServletResponse response) {
        return authenticated(auth.register(request.username(), request.password()), response);
    }

    @PostMapping("/login")
    public Result<AuthResponse> login(@Valid @RequestBody AuthCredentialsRequest request,
                                      HttpServletResponse response) {
        return authenticated(auth.login(request.username(), request.password()), response);
    }

    @PostMapping("/refresh")
    public Result<AuthResponse> refresh(
            @CookieValue(value = RefreshCookieService.COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        return authenticated(auth.refresh(refreshToken), response);
    }

    @PostMapping("/logout")
    public Result<Void> logout(
            @CookieValue(value = RefreshCookieService.COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        auth.logout(refreshToken);
        response.addHeader(HttpHeaders.SET_COOKIE, cookies.clear().toString());
        return Result.success();
    }

    @GetMapping("/me")
    public Result<UserResponse> me() {
        return Result.success(UserResponse.from(auth.currentUser(currentUser.requireUserId())));
    }

    private Result<AuthResponse> authenticated(AuthenticationResult result, HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE,
                cookies.issue(result.refreshToken(), result.refreshTokenExpiresAt()).toString());
        response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
        return Result.success(AuthResponse.from(result));
    }
}
