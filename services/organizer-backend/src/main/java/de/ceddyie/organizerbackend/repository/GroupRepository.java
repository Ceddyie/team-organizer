package de.ceddyie.organizerbackend.repository;

import de.ceddyie.organizerbackend.model.Group;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    boolean existsByInviteCode(String code);

    Optional<Group> findByInviteCode(String inviteCode);
}
