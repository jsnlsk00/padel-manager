package be.ephec.padel.auth;

import be.ephec.padel.common.exceptions.ForbiddenOperationException;
import be.ephec.padel.members.Member;
import be.ephec.padel.members.Role;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/** Acces au membre authentifie et verification de la portee des admins. */
@Service
public class CurrentMemberService {

    public Member requireMember() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null
                ? null
                : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (principal instanceof PadelUserDetails details) {
            return details.getMember();
        }
        throw new ForbiddenOperationException("Aucun membre authentifie");
    }

    public boolean isGlobalAdmin(Member member) {
        return member.getRoles().contains(Role.ROLE_ADMIN_GLOBAL);
    }

    /**
     * Un admin de site ne peut consulter que son propre site ; un admin global voit tout.
     */
    public void requireScopeOn(Member member, Long siteId) {
        if (isGlobalAdmin(member)) {
            return;
        }
        if (!member.getRoles().contains(Role.ROLE_ADMIN_SITE)) {
            throw new ForbiddenOperationException("Acces reserve aux administrateurs");
        }
        Long adminSiteId = member.getAdminSite() == null ? null : member.getAdminSite().getId();
        if (adminSiteId == null || !adminSiteId.equals(siteId)) {
            throw new ForbiddenOperationException(
                    "Un administrateur de site ne peut consulter que son propre site");
        }
    }

    /** Portee de lecture : null pour un admin global (tous les sites), sinon son site. */
    public Long readableSiteScope(Member member) {
        if (isGlobalAdmin(member)) {
            return null;
        }
        if (member.getAdminSite() == null) {
            throw new ForbiddenOperationException("Aucun site administre pour ce compte");
        }
        return member.getAdminSite().getId();
    }
}
