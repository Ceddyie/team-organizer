package de.ceddyie.organizerbackend.repository;

import de.ceddyie.organizerbackend.model.Group;
import de.ceddyie.organizerbackend.model.GroupMember;
import de.ceddyie.organizerbackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
    boolean existsByGroupAndUser(Group group, User user);

    List<GroupMember> findByUserIdAndGroupIdIn(Long userId, List<Long> groupIds);

    Optional<GroupMember> findByUserId(Long userId);

    List<GroupMember> findAllByUser(User user);

    void deleteByGroupAndUser(Group group, User user);

    void deleteByGroup(Group group);

    List<GroupMember> findByGroup(Group group);

    Optional<GroupMember> findByGroupIdAndUserId(Long groupId, Long memberId);
}
