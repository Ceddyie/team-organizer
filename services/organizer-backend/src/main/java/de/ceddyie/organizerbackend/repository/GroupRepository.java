package de.ceddyie.organizerbackend.repository;

import de.ceddyie.organizerbackend.model.Group;
import de.ceddyie.organizerbackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupRepository extends JpaRepository<Group, Long> {
    boolean existsByInviteCode(String code);

    Optional<Group> findByInviteCode(String inviteCode);

    List<Group> findAllByMembersUser(User user);
}
