package de.ceddyie.organizerbackend.repository;

import de.ceddyie.organizerbackend.model.GroupMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupMemberRepository extends JpaRepository<GroupMember, Long> {
}
