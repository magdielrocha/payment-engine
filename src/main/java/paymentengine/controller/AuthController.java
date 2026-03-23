package paymentengine.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import paymentengine.domain.User;
import paymentengine.dto.LoginRequestDTO;
import paymentengine.dto.RegisterRequestDTO;
import paymentengine.dto.TokenResponseDTO;
import paymentengine.repository.UserRepository;
import paymentengine.security.CustomUserDetails;
import paymentengine.security.TokenService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;


    @PostMapping("/login")
    public ResponseEntity<TokenResponseDTO> login(@RequestBody @Valid LoginRequestDTO data) {
        var userNamePassword = new UsernamePasswordAuthenticationToken(data.email(), data.password());
        var auth =  authenticationManager.authenticate(userNamePassword);

        if (auth.getPrincipal() instanceof CustomUserDetails customUserDetails) {
            var user = customUserDetails.getUser();
            var token = tokenService.generateToken(user);
            return ResponseEntity.ok(new TokenResponseDTO(token));
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
    }


    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterRequestDTO data) {

        if (userRepository.findByEmail(data.email()).isPresent()) {
            return ResponseEntity.badRequest().build();
        }

        String encryptedPassword = passwordEncoder.encode(data.password());

        User newUser = new User(null, data.email(), encryptedPassword);
        userRepository.save(newUser);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

}
