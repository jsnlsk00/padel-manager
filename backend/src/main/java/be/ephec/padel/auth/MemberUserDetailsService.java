package be.ephec.padel.auth;

import be.ephec.padel.members.MemberRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MemberUserDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    public MemberUserDetailsService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String matricule) throws UsernameNotFoundException {
        return memberRepository.findByMatriculeIgnoreCase(matricule)
                .map(PadelUserDetails::new)
                .orElseThrow(() -> new UsernameNotFoundException("Identifiants invalides"));
    }
}
