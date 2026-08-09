package be.ephec.padel.members;

import be.ephec.padel.common.exceptions.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MemberService {

    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Transactional(readOnly = true)
    public Member findById(Long id) {
        return memberRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Membre", id));
    }

    @Transactional(readOnly = true)
    public Member findByMatricule(String matricule) {
        return memberRepository.findByMatriculeIgnoreCase(matricule)
                .orElseThrow(() -> new ResourceNotFoundException("Membre", matricule));
    }

    /**
     * Un membre de site n'est reservable que sur son site mais reste visible partout :
     * la liste d'un site contient donc ses membres, plus tous les globaux et libres.
     */
    @Transactional(readOnly = true)
    public List<Member> findVisibleFrom(Long siteId) {
        return siteId == null ? memberRepository.findAll() : memberRepository.findVisibleFromSite(siteId);
    }

    @Transactional(readOnly = true)
    public long countByType(MemberType type) {
        return memberRepository.countByMemberType(type);
    }
}
