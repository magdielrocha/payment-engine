package paymentengine.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import paymentengine.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class AuthorizationService implements UserDetailsService {

    private final UserRepository repository;

    @Override
    public UserDetails loadUserByUsername (String username) throws UsernameNotFoundException {
        return repository.findByEmail(username)
                .map(CustomUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }

}
