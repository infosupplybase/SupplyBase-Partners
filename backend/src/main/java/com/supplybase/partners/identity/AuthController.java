package com.supplybase.partners.identity;

import com.supplybase.partners.identity.dto.OtpRequestRequest;
import com.supplybase.partners.identity.dto.OtpVerifyRequest;
import com.supplybase.partners.identity.dto.SessionResponse;
import com.supplybase.partners.identity.dto.StaffLoginRequest;
import com.supplybase.partners.partner.PartnerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final OtpService otpService;
    private final AppUserRepository appUserRepository;
    private final PartnerService partnerService;
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;

    public AuthController(OtpService otpService, AppUserRepository appUserRepository, PartnerService partnerService,
                           AuthenticationManager authenticationManager, SecurityContextRepository securityContextRepository) {
        this.otpService = otpService;
        this.appUserRepository = appUserRepository;
        this.partnerService = partnerService;
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/otp/request")
    public ResponseEntity<Map<String, Object>> requestOtp(@Valid @RequestBody OtpRequestRequest body, HttpServletRequest request) {
        OtpService.RequestResult result = otpService.requestOtp(body.phone(), request.getRemoteAddr());
        return ResponseEntity.ok(Map.of(
                "phone", result.phoneE164(),
                "expiresAt", result.expiresAt(),
                "resendAvailableAt", result.resendAvailableAt()
        ));
    }

    @PostMapping("/otp/verify")
    @Transactional
    public ResponseEntity<SessionResponse> verifyOtp(@Valid @RequestBody OtpVerifyRequest body,
                                                       HttpServletRequest request, HttpServletResponse response) {
        String phone = otpService.verifyOtp(body.phone(), body.code());
        AppUser user = appUserRepository.findByPhoneE164(phone).orElseGet(() -> {
            AppUser u = new AppUser();
            u.setPhoneE164(phone);
            u.getRoles().add(Role.PARTNER);
            return appUserRepository.save(u);
        });
        partnerService.ensurePartnerExists(user.getId());

        AppUserPrincipal principal = new AppUserPrincipal(user);
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        return ResponseEntity.ok(toSessionResponse(user));
    }

    @PostMapping("/staff/login")
    public ResponseEntity<SessionResponse> staffLogin(@Valid @RequestBody StaffLoginRequest body,
                                                        HttpServletRequest request, HttpServletResponse response) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(body.username(), body.password()));
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        AppUser user = appUserRepository.findByUsername(body.username()).orElseThrow();
        return ResponseEntity.ok(toSessionResponse(user));
    }

    @GetMapping("/session")
    public ResponseEntity<SessionResponse> session() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof AppUserPrincipal principal)) {
            return ResponseEntity.ok(SessionResponse.anonymous());
        }
        AppUser user = appUserRepository.findById(principal.getUserId()).orElseThrow();
        return ResponseEntity.ok(toSessionResponse(user));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        SecurityContextHolder.clearContext();
        var session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return ResponseEntity.noContent().build();
    }

    private SessionResponse toSessionResponse(AppUser user) {
        String displayName = user.getName() != null ? user.getName() : user.getPhoneE164();
        return new SessionResponse(true, user.getId(), displayName, user.getRoles());
    }
}
